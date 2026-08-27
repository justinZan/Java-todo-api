# 第 23 周：查询优化、索引和慢 SQL 思维

本周目标不是“看到 SQL 就加索引”，而是建立一套可重复的排查流程：先找到慢在哪里，再看执行计划，最后根据真实查询条件设计索引，并用测试验证改动没有破坏功能。

本周已经在 `java-todo-api` 中落地：

- 为 Todo 列表、完成状态筛选、过期扫描和管理员列表增加组合索引；
- 将管理员 Todo 统计从多次独立 `COUNT` 查询合并为一次聚合查询；
- 为非唯一字段排序补充 `id` 作为第二排序键，保证分页顺序稳定；
- 增加索引、`EXPLAIN` 和聚合查询测试；
- 保留默认 H2 配置，不需要安装 PostgreSQL 或 Docker。

## 一、本周完成后要掌握什么

你应该能够回答这些问题：

1. 一个接口变慢时，如何判断是 Java、数据库还是网络耗时？
2. 什么是全表扫描？如何从 `EXPLAIN` 中看出来？
3. 为什么组合索引的字段顺序很重要？
4. `WHERE user_id = ? AND deleted = false ORDER BY id` 应该如何设计索引？
5. 为什么 `LIKE '%Java%'` 通常不能直接依赖普通 B+Tree 索引？
6. 为什么分页排序最好增加唯一字段作为第二排序条件？
7. 为什么一次聚合查询可能比多次 `COUNT` 查询更合适？
8. 索引为什么不能无限添加？

## 二、先看原来的查询场景

Todo 接口和后台任务主要有这些查询：

```java
Page<Todo> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

Page<Todo> findByUserIdAndCompletedAndDeletedFalse(
        Long userId,
        boolean completed,
        Pageable pageable
);

Page<Todo> findByCompletedFalseAndDeletedFalseAndDueDateBefore(
        LocalDate dueDate,
        Pageable pageable
);
```

它们大致会生成以下 SQL 条件：

```sql
-- 普通用户查看自己的未删除 Todo
WHERE user_id = ?
  AND deleted = FALSE
ORDER BY id

-- 用户按完成状态筛选
WHERE user_id = ?
  AND completed = ?
  AND deleted = FALSE
ORDER BY id

-- 定时任务扫描过期 Todo
WHERE completed = FALSE
  AND deleted = FALSE
  AND due_date < ?
ORDER BY id
```

索引应该围绕这些真实查询设计，而不是围绕“看起来重要的字段”设计。

## 三、第一处改变：新增 V8 索引迁移

文件：`src/main/resources/db/migration/V8__add_todo_query_indexes.sql`

### 1. 用户 Todo 列表索引

```sql
CREATE INDEX idx_todos_user_deleted_id
    ON todos (user_id, deleted, id);
```

这个索引对应：

```sql
WHERE user_id = ?
  AND deleted = FALSE
ORDER BY id
```

字段顺序的含义：

- 先按 `user_id` 缩小到当前用户；
- 再按 `deleted` 排除软删除数据；
- 最后按 `id` 保持分页和排序顺序。

### 2. 完成状态筛选索引

```sql
CREATE INDEX idx_todos_user_deleted_completed_id
    ON todos (user_id, deleted, completed, id);
```

这个索引对应用户的完成状态筛选。`user_id` 和 `deleted` 仍然是前置过滤条件，`completed` 继续缩小结果范围，`id` 用来辅助排序。

### 3. 过期扫描索引

```sql
CREATE INDEX idx_todos_deleted_completed_due_id
    ON todos (deleted, completed, due_date, id);
```

定时任务只处理未完成、未删除且已到期的 Todo，所以索引包含这几个条件。`due_date` 是范围查询字段，放在等值过滤字段之后更容易发挥作用。

### 4. 管理员列表索引

```sql
CREATE INDEX idx_todos_deleted_id
    ON todos (deleted, id);
```

管理员可以查看全部 Todo，也可以默认排除软删除数据。这个索引支持：

```sql
WHERE deleted = FALSE
ORDER BY id
```

### 5. 为什么没有给 title 直接加普通索引

当前关键字查询是“包含搜索”，概念上接近：

```sql
WHERE LOWER(title) LIKE LOWER('%Java%')
```

普通 B+Tree 索引通常更适合：

```sql
WHERE title LIKE 'Java%'
```

因为 `%Java%` 的开头没有确定前缀，数据库很难直接从索引树定位起点。真实项目如果需要高质量全文搜索，可以考虑：

- PostgreSQL `pg_trgm`；
- PostgreSQL 全文检索；
- Elasticsearch；
- 对搜索词和业务场景进行重新设计。

现在项目使用 H2 作为默认数据库，因此本周不引入 PostgreSQL 专属索引。

## 四、组合索引的左前缀原则

索引：

```text
(user_id, deleted, completed, id)
```

可以比较好地支持从左侧开始连续使用的条件，例如：

```sql
WHERE user_id = ?

WHERE user_id = ? AND deleted = FALSE

WHERE user_id = ? AND deleted = FALSE AND completed = FALSE
```

但如果只查询：

```sql
WHERE completed = FALSE
```

由于跳过了左侧的 `user_id` 和 `deleted`，这个组合索引通常不能像单独的 `completed` 索引一样高效。

所以设计索引时要先看真实的 `WHERE` 条件顺序和字段选择性，不能只看实体字段定义顺序。

## 五、第二处改变：统计从多次查询合并为一次聚合查询

原来的管理员统计需要分别查询：

```java
todoRepository.count();
todoRepository.countByDeletedFalse();
todoRepository.countByDeletedFalseAndCompletedTrue();
todoRepository.countByDeletedFalseAndCompletedFalse();
todoRepository.countByDeletedTrue();
```

一个请求就可能产生多次数据库往返。现在 Repository 使用一次 JPQL 聚合查询：

```java
@Query("""
        select new com.zading.todoapi.dto.TodoStatistics(
            count(t.id),
            sum(case when t.deleted = false then 1L else 0L end),
            sum(case when t.deleted = false and t.completed = true then 1L else 0L end),
            sum(case when t.deleted = false and t.completed = false then 1L else 0L end),
            sum(case when t.deleted = true then 1L else 0L end)
        )
        from Todo t
        """)
TodoStatistics getStatistics();
```

返回值是 `TodoStatistics` record：

```java
public record TodoStatistics(
        Long totalCount,
        Long activeCount,
        Long completedCount,
        Long incompleteCount,
        Long deletedCount
) {
}
```

### 为什么使用 `Long` 而不是 `long`

`COUNT` 一定会返回数字，但 `SUM` 在没有匹配数据时可能返回 `NULL`。所以 DTO 使用包装类型 `Long`，然后通过：

```java
public long activeCountOrZero() {
    return activeCount == null ? 0L : activeCount;
}
```

将空结果转换为业务层需要的 `0`。

### 这是不是永远更快

不是。优化结论必须基于实际数据量和执行计划。合并查询的主要收益是减少数据库往返和重复扫描，但当统计逻辑很复杂时，也需要重新查看执行计划和数据库负载。

## 六、第三处改变：稳定分页排序

原来如果请求：

```text
GET /api/todos?sort=title,asc&page=0&size=10
```

数据库只按 `title` 排序。当多条 Todo 的标题相同时，不同页之间的顺序可能不稳定，数据插入或执行计划变化后，某一条记录可能在两页之间移动。

现在 `TodoController` 对非 `id` 排序自动补充：

```java
requestedSort = requestedSort.and(
        Sort.by(Sort.Direction.ASC, "id")
);
```

最终效果类似：

```sql
ORDER BY title ASC, id ASC
```

`id` 是唯一字段，因此相同标题的记录也能获得确定顺序。这不是单纯的“加速”，而是让分页结果更加可靠。

## 七、第四处改变：增加可执行的性能相关测试

文件：`src/test/java/com/zading/todoapi/TodoQueryOptimizationTests.java`

### 1. 验证聚合统计

测试创建已完成、未完成和已删除 Todo，然后调用 `getStatistics()`，确认五个统计数字正确。

这个测试保证查询优化没有改变业务结果。

### 2. 验证 V8 索引已经执行

测试查询 H2 的元数据表：

```sql
SELECT INDEX_NAME
FROM INFORMATION_SCHEMA.INDEXES
WHERE TABLE_NAME = 'TODOS'
```

然后检查四个索引都存在。这样 Flyway 文件即使被误删或命名错误，也会在测试阶段暴露出来。

### 3. 验证可以生成执行计划

```sql
EXPLAIN
SELECT *
FROM todos
WHERE user_id = 1
  AND deleted = FALSE
ORDER BY id
```

当前测试只验证 H2 能生成执行计划。实际是否使用了某个索引，还要结合数据量、数据库版本和成本估算判断，不能仅凭“索引存在”得出结论。

## 八、如何学习和运行本周代码

### 第 1 天：理解慢在哪里

先区分三类时间：

```text
请求总耗时 = Java 处理时间 + 数据库耗时 + 网络传输时间
```

执行：

```bash
mvn -q -Dtest=TodoApiTests test
```

观察 Hibernate 输出的 SQL，注意查询条件、排序字段和分页参数。

### 第 2 天：阅读 V8 索引

打开：

```text
src/main/resources/db/migration/V8__add_todo_query_indexes.sql
```

对照 TodoRepository 中的方法，逐个回答：

- 这个索引服务哪个查询？
- 第一个字段为什么放在最前面？
- 是否包含排序字段？
- 是否可能和现有索引重复？

### 第 3 天：使用 H2 查看执行计划

执行：

```bash
mvn -q -Dtest=TodoQueryOptimizationTests test
```

也可以打开 H2 Console，执行：

```sql
EXPLAIN SELECT *
FROM todos
WHERE user_id = 1
  AND deleted = FALSE
ORDER BY id;
```

### 第 4 天：学习聚合查询

重点阅读：

- `TodoRepository.getStatistics()`；
- `TodoStatistics` record；
- `AdminService.getStatistics()`。

思考：如果将来新增“高优先级 Todo 数量”，应该在一次聚合查询中增加一列，还是再发起一次查询？

### 第 5 天：学习分页稳定性

创建多条同名 Todo，然后分别请求：

```bash
GET /api/todos?sort=title,asc&page=0&size=2
GET /api/todos?sort=title,asc&page=1&size=2
```

理解 `title ASC, id ASC` 如何避免相同标题记录的顺序不确定。

### 第 6 天：模拟慢查询分析

按照下面模板记录一个查询：

```text
接口：GET /api/todos
查询条件：user_id + deleted + completed
排序：id ASC
原始问题：数据量增大后扫描行数增加
执行计划：是否使用索引、是否排序、估算行数
优化方案：增加组合索引
验证方式：EXPLAIN + 接口测试
副作用：写入时维护索引需要额外成本
```

### 第 7 天：复盘

运行全部测试：

```bash
mvn -q test
```

确认索引迁移、Repository、Controller 和测试全部通过后，再总结本周的查询优化记录。

## 九、H2 和 PostgreSQL 的差异

本地默认使用 H2，因此本周可以完成：

- Flyway 索引迁移；
- Repository 聚合查询；
- `EXPLAIN` 基础练习；
- 单元测试和 JPA 集成测试。

以后使用 PostgreSQL 时，常用命令是：

```sql
EXPLAIN
SELECT *
FROM todos
WHERE user_id = 1
  AND deleted = FALSE
ORDER BY id;

EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM todos
WHERE user_id = 1
  AND deleted = FALSE
ORDER BY id;
```

`EXPLAIN` 只生成估算计划；`EXPLAIN ANALYZE` 会真正执行 SQL 并记录实际耗时和实际行数。生产环境执行 `EXPLAIN ANALYZE` 前要确认查询没有副作用，写操作更要谨慎。

## 十、常见问题

### 1. 索引越多越好吗？

不是。索引会带来：

- 额外磁盘空间；
- INSERT、UPDATE、DELETE 时的维护成本；
- 优化器选择路径变复杂；
- 过多重复索引难以维护。

索引应该由真实查询驱动，并通过执行计划和性能数据验证。

### 2. 为什么没有给每个字段分别建索引？

因为 Todo 查询通常同时使用多个条件。一个匹配查询模式的组合索引，通常比多个互相独立的单列索引更有针对性。但最终效果要以具体数据库和数据分布为准。

### 3. Boolean 字段适合做索引吗？

单独给低基数字段建立索引，收益可能有限。例如 `completed` 只有 true 和 false 两种值。把它放进包含 `user_id`、`deleted`、`due_date` 的组合索引中，是因为它属于真实查询的一部分，而不是因为 Boolean 字段本身很适合索引。

### 4. 为什么还保留 Spring Data 的方法名查询？

简单查询使用方法名查询可读性好、维护成本低。本周没有为了“手写 SQL”而手写 SQL，只在统计查询需要一次聚合时使用 `@Query`。复杂查询以后可以使用显式 JPQL、原生 SQL 或专门的查询对象。

### 5. `Page<T>` 为什么可能比较慢？

分页查询通常包含两部分：

1. 查询当前页数据；
2. 执行 `COUNT` 查询计算总记录数。

当数据量很大、页码很深时，`OFFSET` 分页也可能变慢。真实生产系统可能需要游标分页或基于 `id` 的 seek pagination。本项目当前保留 `Page<T>`，因为它更适合学习总页数和前后页信息。

## 十一、本周复盘题与参考答案

### 问题 1：遇到慢接口，第一步应该做什么？

先测量和定位，不要直接加索引。需要确认是 Java 业务逻辑慢、数据库 SQL 慢、连接池等待，还是网络和序列化慢。

### 问题 2：组合索引 `(user_id, deleted, completed, id)` 为什么不是随便排序？

数据库通常从组合索引的左侧开始匹配。查询最稳定、最常用的前置条件应该放在左侧，范围条件和排序字段的位置也需要结合执行计划判断。

### 问题 3：为什么 `LIKE '%Java%'` 不能简单通过 `title` 索引解决？

因为前置 `%` 使数据库无法根据固定前缀快速定位索引范围。要支持高质量包含搜索，需要全文索引、trigram 或搜索引擎等方案。

### 问题 4：为什么统计查询从五次改为一次？

一次聚合查询可以减少数据库往返，并且让数据库在一次扫描中计算多个统计值。但复杂度增加后仍然要查看执行计划，不能把“少写几行 Java”当成性能证明。

### 问题 5：索引能解决所有慢 SQL 吗？

不能。慢 SQL 还可能来自返回数据过多、N+1 查询、错误的分页方式、锁等待、连接池不足、排序和分组成本高等问题。

### 问题 6：为什么分页排序要补充 `id`？

因为只按非唯一字段排序时，相同值之间没有确定顺序。补充唯一的 `id` 可以让每一页的边界更稳定，减少翻页时重复或漏数据的概率。

## 十二、下一周

按照既定路线，下一周是第 24 周：Docker 基础和项目容器化。届时会学习如何编写 Dockerfile、构建 Java 镜像、配置容器环境变量，并让应用在容器中运行。

## 代码精读补充

### 1. Repository 方法名和 SQL 条件的对应关系

```java
findByUserIdAndDeletedFalseOrderByIdAsc(Long userId, Pageable pageable)
```

可以拆成：

```text
findBy                  查询
UserId                  user_id = ?
AndDeletedFalse         deleted = false
OrderByIdAsc            ORDER BY id ASC
```

方法名查询适合规则清晰的简单查询；当统计、分组或数据库特性变复杂时，再使用 `@Query` 或原生 SQL。

### 2. `EXPLAIN` 应该看什么

```sql
EXPLAIN SELECT *
FROM todos
WHERE user_id = 1 AND deleted = false
ORDER BY id;
```

重点看访问路径、是否使用索引、预计扫描行数和排序成本。索引存在不等于一定会使用，优化需要结合数据量、选择性和执行计划。

### 3. 为什么索引字段顺序与代码有关

如果代码总是先按 `user_id`、`deleted` 过滤，再按 `id` 排序，那么索引 `(user_id, deleted, id)` 才能同时服务主要过滤和排序。索引设计必须从真实查询出发，而不是只看单个字段名称。
