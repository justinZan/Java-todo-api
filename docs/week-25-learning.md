# 第 25 周：PostgreSQL 深入和数据库设计

本周把“数据库能保存数据”进一步提升为“数据库能够保护数据，并支持可维护的查询和并发更新”。本周没有安装 PostgreSQL 或 Docker，所有自动化验证继续使用 H2 和 Flyway 完成。

## 一、本周完成了什么

| 文件 | 改变 | 目的 |
| --- | --- | --- |
| `V9__add_data_constraints_and_todo_version.sql` | 增加检查约束和 `todos.version` | 让数据库保护业务数据，并支持乐观锁 |
| `Todo.java` | 增加 `@Version`，明确标题长度 | 让 Hibernate 与数据库版本号协作 |
| `TodoActionLog.java` | 明确操作类型和描述长度 | 让 Entity 映射更接近表结构 |
| `TodoAttachment.java` | 明确文件名和内容类型长度 | 避免 Java 默认长度与数据库设计不一致 |
| `TodoDatabaseDesignTests.java` | 测试迁移、约束和版本号 | 不依赖 PostgreSQL 也能验证数据库设计 |
| `database-design.md` | 记录表关系、索引、约束和事务 | 形成可维护的数据库设计文档 |

## 二、第一处改变：增加数据库检查约束

文件：`src/main/resources/db/migration/V9__add_data_constraints_and_todo_version.sql`

例如标题约束：

```sql
ALTER TABLE todos
    ADD CONSTRAINT ck_todos_title_not_blank
    CHECK (TRIM(title) <> '');
```

逐句理解：

1. `ALTER TABLE todos` 表示修改已经存在的 `todos` 表；
2. `ADD CONSTRAINT` 表示新增一个有名字的约束；
3. `TRIM(title)` 去掉标题前后的空格；
4. `<> ''` 表示处理空格后不能是空字符串。

Java 层已经会对标题做校验，但数据库约束仍然有必要。因为未来可能有数据修复脚本、后台定时任务、管理员工具或其他服务直接连接数据库，这些写入方式可能不会经过当前 Controller 和 Service。

## 三、第二处改变：约束枚举值

```sql
ALTER TABLE todos
    ADD CONSTRAINT ck_todos_priority
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));
```

项目中的 `TodoPriority` 使用字符串保存：

```java
@Enumerated(EnumType.STRING)
private TodoPriority priority;
```

使用字符串比使用数字更容易阅读，也不容易因为枚举顺序变化而产生错误。但是字符串也可能被写入拼写错误的值，因此数据库用 `CHECK` 限制合法集合。

同样的思路也应用到了用户角色、Todo 操作类型和附件大小。

## 四、第三处改变：Todo 乐观锁

`Todo.java` 新增：

```java
@Version
@Column(nullable = false)
private Long version;
```

`@Version` 是 JPA 的乐观锁标记。它不是让数据库锁住整张表，而是给每一行增加一个版本号。

新增 Todo 时版本号为 `0`，每次更新成功后加 `1`。Hibernate 更新时会把旧版本放在 `WHERE` 条件中：

```sql
UPDATE todos
SET title = ?, version = 1
WHERE id = ? AND version = 0;
```

如果另一个请求已经把版本改成 `1`，这个更新就匹配不到数据，Hibernate 会抛出乐观锁异常。这样可以发现并发冲突，而不是让后一个请求悄悄覆盖前一个请求。

Todo 更新通常很短，大部分请求也不会同时修改同一个 Todo，因此乐观锁适合当前场景。发生冲突时，后续可以让前端重新加载最新数据。

项目的全局异常处理会把这个冲突转换为 HTTP 409：

```json
{
  "success": false,
  "code": "CONCURRENT_UPDATE_CONFLICT",
  "message": "数据已被其他请求修改，请刷新后重试"
}
```

409 表示请求本身格式正确，但当前资源状态已经发生冲突。前端可以在收到 409 后重新请求 Todo 详情，再决定是否让用户重新提交修改。

## 五、第四处改变：让 Entity 和表结构对齐

之前部分字段依赖 JPA 默认长度，例如：

```java
@Column(nullable = false)
private String title;
```

本周改为：

```java
@Column(nullable = false, length = 255)
private String title;
```

操作日志和附件元数据也补充了对应长度：

- 操作类型：50；
- 描述：255；
- 原始文件名：255；
- 保存文件名：255；
- 内容类型：100。

这些长度和 Flyway 中的 `VARCHAR` 定义保持一致。

## 六、第五处改变：增加数据库设计测试

测试文件：`src/test/java/com/zading/todoapi/TodoDatabaseDesignTests.java`

测试包含四个重点：

1. 新建 Todo 的版本是 `0`，更新一次后变成 `1`；
2. 从 H2 的 `information_schema.table_constraints` 查询 V9 约束名称；
3. 直接使用 JDBC 插入三个空格的标题，验证数据库拒绝非法数据；
4. 查询 `todos.version` 列，验证迁移和 Entity 不是只在代码层存在。

## 七、每天学习安排

### 第一天：PostgreSQL 与 H2

- 了解数据库、Schema、Table、Column；
- 对比 H2 和 PostgreSQL 的数据类型；
- 阅读 `V1` 到 `V9`；
- 说明为什么默认配置仍然使用 H2。

### 第二天：表设计和约束

- 分析主键、外键、唯一约束和非空约束；
- 阅读 V9 的 `CHECK` 约束；
- 思考为什么 Service 校验和数据库校验要同时存在。

### 第三天：实体关系

- 画出 User、Todo、Attachment、ActionLog 的关系；
- 理解一对多关系中的外键；
- 说明附件和日志为什么不直接放进 Todo 表。

### 第四天：事务和并发

- 复习事务的 ACID；
- 阅读 Todo Service 上的 `@Transactional`；
- 理解 `@Version` 解决的并发覆盖问题。

### 第五天：索引设计

- 阅读 V8 的四个 Todo 索引；
- 将索引和 `TodoRepository` 的查询方法对应起来；
- 思考索引为什么会增加写入成本。

### 第六天：SQL 分析和分页

- 复习 `EXPLAIN`；
- 分析用户 Todo 列表、完成状态筛选、过期扫描；
- 理解为什么分页排序要增加 `id` 作为稳定排序字段。

### 第七天：设计复盘

- 独立画出数据库 ER 图；
- 不看文档说明 V1 到 V9 的变化；
- 写出一份新的表设计方案；
- 运行全部测试并记录结果。

## 八、运行和验证

在项目根目录执行：

```bash
mvn test
```

只运行本周数据库设计测试：

```bash
mvn -Dtest=TodoDatabaseDesignTests test
```

默认测试使用 `src/test/resources/application-test.properties`，因此不需要 PostgreSQL、Redis 或 Docker。

## 九、本周复盘问题与答案

### 1. 为什么不能只依赖 Java 校验？

因为数据库可能被脚本、后台任务或其他服务直接访问。数据库约束可以作为最后一道数据质量防线。

### 2. `@Version` 是悲观锁还是乐观锁？

是乐观锁。它不提前锁住数据，而是在更新时检查版本是否仍然是读取时的版本。

### 3. 为什么数据库迁移不能随便修改旧文件？

旧迁移可能已经在其他环境执行。修改旧文件会导致不同环境的迁移历史不一致，应该新增 V10、V11 等版本。

### 4. 为什么索引不能无限添加？

索引会占用空间，并且插入、更新、删除数据时也需要维护索引。无效索引会增加成本，却不能提高查询速度。

### 5. H2 的 PostgreSQL 模式能完全替代 PostgreSQL 吗？

不能。它只能提高部分 SQL 兼容性，真实 PostgreSQL 的执行计划、类型、锁和扩展能力仍需要 PostgreSQL 环境验证。

## 代码精读补充

### 1. 数据库约束和 Java 校验是两道防线

```java
@NotBlank
@Size(max = 200)
private String title;
```

```sql
CHECK (TRIM(title) <> '')
```

DTO 校验让接口尽早返回清晰错误，数据库约束防止脚本、后台任务或其他服务写入非法数据。两者不是重复劳动，而是分别保护入口和最终数据。

### 2. `@Version` 如何进入 UPDATE 条件

```java
@Version
private Long version;
```

Hibernate 更新时会生成类似：

```sql
UPDATE todos
SET title = ?, version = 2
WHERE id = ? AND version = 1;
```

如果受影响行数为 0，说明另一个请求已经修改过数据，Hibernate 抛出乐观锁异常，统一异常处理器再把它转换成 HTTP 409。

### 3. 为什么 Entity 字段长度要和数据库对应

```java
@Column(nullable = false, length = 200)
private String title;
```

Java 的 `String` 本身没有长度限制，数据库列却有。显式声明 `length` 可以让 Entity 意图和迁移脚本保持一致，减少 `validate` 或生产写入时才暴露问题。
