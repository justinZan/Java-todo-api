# 第十一周学习说明：软删除、恢复接口和 Todo 生命周期

第十一周的目标是让 Todo 从“普通 CRUD 数据”升级成“有生命周期的数据”。

前面项目已经做到：

```text
注册 / 登录
JWT 鉴权
用户数据隔离
分页 / 排序 / 筛选
OpenAPI 文档
测试体系拆分
CI 配置
```

这一周回到业务本身，重点学习：

```text
软删除
恢复接口
completedAt / deletedAt 审计字段
状态一致性
默认查询规则
```

最核心的一句话：

```text
删除不一定意味着数据消失；在业务系统里，删除经常意味着状态变化。
```

## 本周最终改动总览

| 改动 | 文件 | 目的 |
|---|---|---|
| 新增 Flyway V4 | `V4__add_todo_lifecycle_fields.sql` | 给 todos 表增加生命周期字段 |
| Todo 增加字段 | `Todo.java` | 保存 deleted / completedAt / deletedAt |
| TodoResponse 增加字段 | `TodoResponse.java` | API 返回生命周期信息 |
| Mapper 同步字段 | `TodoMapper.java` | Entity 转 DTO 时包含新字段 |
| Repository 查询排除 deleted | `TodoRepository.java` | 默认只查未删除 Todo |
| 删除改成软删除 | `TodoService.java` | DELETE 不物理删除，而是标记 deleted |
| 新增恢复方法 | `TodoService.java` | 支持把已删除 Todo 恢复 |
| 新增恢复接口 | `TodoController.java` | `PATCH /api/todos/{id}/restore` |
| 完成状态写入完成时间 | `TodoService.java` | completed 和 completedAt 保持一致 |
| 扩展测试 | `TodoApiTests.java` | 覆盖软删除、恢复、完成时间和用户隔离 |
| 更新 README | `README.md` | 补充软删除和恢复接口说明 |

## 第一步：为什么要做软删除？

之前删除 Todo 的逻辑是物理删除。

也就是：

```text
DELETE /api/todos/{id}
  -> 数据库记录真的删除
```

这在学习 CRUD 时没问题，但真实业务里经常不够用。

真实系统经常需要回答：

```text
用户误删了怎么办？
这个任务是什么时候删的？
删除前内容是什么？
统计数据要不要保留？
线上排查时能不能看到历史记录？
```

如果物理删除，数据库里记录没了，这些问题就很难回答。

所以本周改成软删除：

```text
DELETE /api/todos/{id}
  -> 数据库记录保留
  -> deleted = true
  -> deletedAt = 当前时间
```

用户看起来是删除了。

系统内部仍然保留记录。

## 第二步：新增 V4 migration

文件：

```text
src/main/resources/db/migration/V4__add_todo_lifecycle_fields.sql
```

内容：

```sql
ALTER TABLE todos
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE todos
    ADD COLUMN completed_at TIMESTAMP;

ALTER TABLE todos
    ADD COLUMN deleted_at TIMESTAMP;
```

为什么是 V4？

因为之前已经有：

```text
V1 创建 todos 表
V2 增加 priority / dueDate
V3 增加 users / user_id
```

新的结构变化必须继续新增：

```text
V4
```

不要回头修改 V1、V2、V3。

这就是 Flyway 的版本化迁移思维。

## 第三步：三个新字段分别表示什么？

本周新增：

| 字段 | 类型 | 含义 |
|---|---|---|
| `deleted` | boolean | 是否已删除 |
| `completedAt` | LocalDateTime | 完成时间 |
| `deletedAt` | LocalDateTime | 删除时间 |

为什么不只用 boolean？

因为：

```text
completed 只能说明是否完成
deleted 只能说明是否删除
```

但它们不能说明：

```text
什么时候完成？
什么时候删除？
```

`completedAt` 和 `deletedAt` 就是审计字段。

审计字段的价值是：

```text
记录关键业务事件发生的时间
```

## 第四步：Todo Entity 怎么变？

文件：

```text
src/main/java/com/zading/todoapi/model/Todo.java
```

新增字段：

```java
@Column(nullable = false)
private boolean deleted;

private LocalDateTime completedAt;

private LocalDateTime deletedAt;
```

解释：

```java
@Column(nullable = false)
private boolean deleted;
```

表示 `deleted` 在数据库里不能为空。

因为 migration 给了默认值：

```sql
deleted BOOLEAN NOT NULL DEFAULT FALSE
```

所以旧数据也会自动拥有：

```text
deleted = false
```

`completedAt` 和 `deletedAt` 没有设置 `nullable = false`。

因为它们本来就可以为空：

```text
未完成 Todo：completedAt = null
未删除 Todo：deletedAt = null
```

## 第五步：TodoResponse 为什么也要变？

文件：

```text
src/main/java/com/zading/todoapi/dto/TodoResponse.java
```

新增：

```java
private boolean deleted;
private LocalDateTime completedAt;
private LocalDateTime deletedAt;
```

为什么响应 DTO 也返回这些字段？

因为前端可能需要展示：

```text
这个任务是否已删除
什么时候完成
什么时候删除
```

比如以后做“回收站”页面，就需要：

```text
任务标题
删除时间
恢复按钮
```

如果响应里没有 `deletedAt`，前端就没法展示删除时间。

## 第六步：Mapper 为什么必须同步？

文件：

```text
src/main/java/com/zading/todoapi/mapper/TodoMapper.java
```

现在转换逻辑包含：

```java
return new TodoResponse(
        todo.getId(),
        todo.getTitle(),
        todo.isCompleted(),
        todo.isDeleted(),
        todo.getPriority(),
        todo.getDueDate(),
        todo.getCompletedAt(),
        todo.getDeletedAt(),
        todo.getCreatedAt(),
        todo.getUpdatedAt()
);
```

这延续第六周的 DTO 分层思想：

```text
Todo Entity 是数据库模型
TodoResponse 是接口返回模型
TodoMapper 负责转换
```

Entity 加字段，不代表接口一定要返回。

但这次业务上希望前端知道生命周期信息，所以 DTO 和 Mapper 都同步新增。

## 第七步：Repository 为什么要加 `DeletedFalse`？

文件：

```text
src/main/java/com/zading/todoapi/repository/TodoRepository.java
```

之前：

```java
Page<Todo> findByUserId(Long userId, Pageable pageable);
```

现在：

```java
Page<Todo> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);
```

关键是：

```text
DeletedFalse
```

Spring Data JPA 会根据方法名生成：

```sql
WHERE user_id = ?
AND deleted = false
```

为什么必须加？

因为软删除后，删除的数据还在数据库里。

如果查询不排除：

```text
deleted = true
```

用户删除后列表里仍然会看到它。

所以普通业务查询默认只看：

```text
deleted = false
```

这就是软删除最重要的查询规则。

## 第八步：本周 Repository 方法变化

现在 Repository 有：

```java
Page<Todo> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

Page<Todo> findByUserIdAndCompletedAndDeletedFalse(Long userId, boolean completed, Pageable pageable);

Page<Todo> findByUserIdAndTitleContainingIgnoreCaseAndDeletedFalse(Long userId, String keyword, Pageable pageable);

Page<Todo> findByUserIdAndCompletedAndTitleContainingIgnoreCaseAndDeletedFalse(
        Long userId,
        boolean completed,
        String keyword,
        Pageable pageable
);

Optional<Todo> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

Optional<Todo> findByIdAndUserId(Long id, Long userId);
```

注意最后两个的区别。

普通详情查询用：

```java
findByIdAndUserIdAndDeletedFalse
```

因为普通接口不应该看到已删除 Todo。

恢复接口用：

```java
findByIdAndUserId
```

因为恢复接口需要找到“已经被删除”的 Todo。

这个区别非常关键。

## 第九步：删除逻辑如何从物理删除变成软删除？

文件：

```text
src/main/java/com/zading/todoapi/service/TodoService.java
```

之前：

```java
public void deleteTodo(Long userId, Long id) {
    if (!todoRepository.existsByIdAndUserId(id, userId)) {
        throw new TodoNotFoundException(id);
    }

    todoRepository.deleteById(id);
}
```

现在：

```java
public void deleteTodo(Long userId, Long id) {
    Todo todo = getTodo(userId, id);
    todo.setDeleted(true);
    todo.setDeletedAt(LocalDateTime.now());

    todoRepository.save(todo);
}
```

变化点：

```text
不再 deleteById
改为 setDeleted(true)
写入 deletedAt
save 更新数据库
```

所以现在 DELETE 接口背后的 SQL 更接近：

```sql
UPDATE todos
SET deleted = true,
    deleted_at = ?
WHERE id = ?
```

不是：

```sql
DELETE FROM todos
WHERE id = ?
```

## 第十步：为什么 deleteTodo 里使用 getTodo？

`getTodo(userId, id)` 现在只查未删除 Todo：

```java
return todoRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
        .orElseThrow(() -> new TodoNotFoundException(id));
```

这意味着：

```text
已经删除的 Todo 再次删除，会返回 404
用户 B 删除用户 A 的 Todo，也会返回 404
```

这让普通业务接口保持一个清晰规则：

```text
只能操作当前用户未删除的数据
```

## 第十一步：恢复接口如何实现？

Controller 新增：

```java
@PatchMapping("/{id}/restore")
public TodoResponse restoreTodo(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
    return todoMapper.toResponse(todoService.restoreTodo(currentUser.getId(), id));
}
```

接口：

```http
PATCH /api/todos/{id}/restore
```

为什么用 PATCH？

因为恢复不是创建资源，也不是替换整个资源。

它只是修改部分状态：

```text
deleted: true -> false
deletedAt: 某个时间 -> null
```

所以 PATCH 合适。

## 第十二步：restoreTodo 为什么不能用 getTodo？

Service：

```java
public Todo restoreTodo(Long userId, Long id) {
    Todo todo = todoRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new TodoNotFoundException(id));

    todo.setDeleted(false);
    todo.setDeletedAt(null);

    return todoRepository.save(todo);
}
```

这里故意不用：

```java
getTodo(userId, id)
```

原因是：`getTodo` 只查未删除 Todo。

但恢复接口要查的是可能已经删除的数据。

所以 restore 必须使用：

```java
findByIdAndUserId
```

也就是：

```text
允许找到 deleted = true 的记录
但仍然必须限制 userId
```

不能忘记 userId。

否则用户 B 可能恢复用户 A 的 Todo。

## 第十三步：completedAt 如何保持一致？

之前 toggle 只是：

```java
todo.setCompleted(!todo.isCompleted());
```

现在改成：

```java
applyCompleted(todo, !todo.isCompleted());
```

辅助方法：

```java
private void applyCompleted(Todo todo, boolean completed) {
    todo.setCompleted(completed);

    if (completed) {
        todo.setCompletedAt(LocalDateTime.now());
    } else {
        todo.setCompletedAt(null);
    }
}
```

规则：

```text
completed = true   -> completedAt 有值
completed = false  -> completedAt = null
```

这样不会出现矛盾数据：

```json
{
  "completed": false,
  "completedAt": "2026-08-04T10:00:00"
}
```

状态字段和时间字段必须保持一致。

## 第十四步：为什么 updateTodo 也要用 applyCompleted？

Todo 有两个地方会改变完成状态：

```text
PATCH /api/todos/{id}
PATCH /api/todos/{id}/toggle
```

如果 toggle 维护了 `completedAt`，但 update 不维护，就会出现不一致。

所以 update 中：

```java
if (completed != null) {
    applyCompleted(todo, completed);
}
```

这体现一个小但重要的设计：

```text
同一个业务规则只写一份。
```

## 第十五步：测试新增了什么？

本周测试仍然在：

```text
src/test/java/com/zading/todoapi/TodoApiTests.java
```

新增或增强场景：

```text
创建 Todo 时返回 deleted=false、completedAt=null、deletedAt=null
完成 Todo 时 completedAt 有值
取消完成时 completedAt 清空
删除 Todo 后列表查不到
删除 Todo 后 GET /api/todos/{id} 返回 404
restore 后 Todo 可重新查询
用户 B 不能 restore 用户 A 的 Todo
PATCH completed=false 时 completedAt 清空
```

测试数量从 14 个增加到 17 个。

## 第十六步：为什么删除后 GET 返回 404？

删除后数据库记录还在。

但普通查询使用：

```java
findByIdAndUserIdAndDeletedFalse
```

所以 deleted=true 的记录查不到。

对普通 API 来说，它就是：

```text
不存在
```

这也是很多系统的设计：

```text
软删除数据默认对普通用户不可见
```

## 当前 API 行为

删除 Todo：

```http
DELETE /api/todos/{id}
Authorization: Bearer <token>
```

效果：

```text
deleted = true
deletedAt = 当前时间
```

恢复 Todo：

```http
PATCH /api/todos/{id}/restore
Authorization: Bearer <token>
```

效果：

```text
deleted = false
deletedAt = null
```

完成 Todo：

```http
PATCH /api/todos/{id}/toggle
Authorization: Bearer <token>
```

效果：

```text
completed = true
completedAt = 当前时间
```

取消完成：

```http
PATCH /api/todos/{id}/toggle
Authorization: Bearer <token>
```

效果：

```text
completed = false
completedAt = null
```

响应新增字段：

```json
{
  "deleted": false,
  "completedAt": null,
  "deletedAt": null
}
```

## 本周复盘问题

1. 什么是软删除？
2. 为什么真实业务里经常不用物理删除？
3. `deleted` 和 `deletedAt` 分别解决什么问题？
4. 为什么新增字段要写 V4，而不是修改 V1？
5. 为什么普通查询都要加 `DeletedFalse`？
6. 为什么 restore 不能使用普通的 `getTodo`？
7. 为什么 completed=false 时要清空 completedAt？
8. 为什么 DELETE 接口仍然可以返回 204？
9. 用户 B 为什么不能恢复用户 A 的 Todo？
10. 软删除会带来什么额外复杂度？

## 参考答案

### 1. 什么是软删除？

软删除是指不真正删除数据库记录，而是用字段标记它已删除。

当前项目中：

```text
deleted = true
deletedAt = 删除时间
```

### 2. 为什么真实业务里经常不用物理删除？

因为很多数据需要保留历史。

例如：

```text
误删恢复
审计追踪
统计分析
问题排查
```

物理删除后，这些信息就丢失了。

### 3. `deleted` 和 `deletedAt` 分别解决什么问题？

`deleted` 表示是否删除。

`deletedAt` 表示什么时候删除。

一个回答状态，一个回答时间。

### 4. 为什么新增字段要写 V4，而不是修改 V1？

因为 V1、V2、V3 可能已经在数据库执行过。

修改已执行 migration 会破坏 Flyway 的版本历史。

新的结构变化应该新增 V4。

### 5. 为什么普通查询都要加 `DeletedFalse`？

因为软删除数据还在数据库里。

如果不加 `deleted=false` 条件，删除后的 Todo 仍然会出现在列表和详情里。

### 6. 为什么 restore 不能使用普通的 `getTodo`？

因为普通 `getTodo` 只查询：

```text
deleted = false
```

但 restore 要找到已经删除的 Todo。

所以 restore 使用：

```java
findByIdAndUserId
```

它不限制 deleted，但仍然限制 userId。

### 7. 为什么 completed=false 时要清空 completedAt？

因为未完成的 Todo 不应该有完成时间。

否则会出现字段矛盾：

```text
completed = false
completedAt 有值
```

### 8. 为什么 DELETE 接口仍然可以返回 204？

因为 204 表示请求成功且没有响应体。

客户端不关心后端是物理删除还是软删除，只关心删除操作成功。

### 9. 用户 B 为什么不能恢复用户 A 的 Todo？

因为 restore 查询使用：

```java
findByIdAndUserId(id, currentUserId)
```

即使 Todo 存在，只要不属于当前用户，也查不到。

### 10. 软删除会带来什么额外复杂度？

所有普通查询都要记得排除 deleted 数据。

同时还要考虑：

```text
恢复接口
删除后的更新规则
删除后的统计规则
唯一索引是否受 deleted 影响
```

软删除很有价值，但确实会让查询规则更复杂。

## 更多复盘问题

1. 软删除和回收站是什么关系？
2. 为什么本周没有直接引入 TodoStatus enum？
3. `completedAt` 算不算审计字段？
4. 软删除后，数据库记录是否还会占空间？
5. 如果以后要永久删除，应该新增什么接口？
6. 为什么 deleteTodo 使用 `save`，而不是 `deleteById`？
7. 为什么 `deleted` 字段要有默认值 false？
8. 如果用户恢复一个未删除 Todo，当前代码会怎样？
9. 为什么测试要验证删除后列表查不到？
10. 如果以后做“回收站列表”，Repository 需要什么查询？

## 更多复盘问题参考答案

### 1. 软删除和回收站是什么关系？

软删除是实现回收站的基础。

被软删除的数据仍然在数据库里，所以可以查询出 deleted=true 的数据做回收站列表。

### 2. 为什么本周没有直接引入 TodoStatus enum？

因为项目当前已经大量使用 `completed`。

直接改成 `status` 会影响接口、查询、测试和文档。

本周采用温和演进：

```text
保留 completed
新增 deleted / completedAt / deletedAt
```

### 3. `completedAt` 算不算审计字段？

算。

它记录“完成”这个业务事件发生的时间。

### 4. 软删除后，数据库记录是否还会占空间？

会。

软删除保留记录，所以会占用数据库空间。

这是用空间换历史和可恢复能力。

### 5. 如果以后要永久删除，应该新增什么接口？

可以新增管理员或回收站里的永久删除接口，例如：

```http
DELETE /api/todos/{id}/purge
```

但这类接口风险更高，需要更严格权限控制。

### 6. 为什么 deleteTodo 使用 `save`，而不是 `deleteById`？

因为软删除本质是更新状态。

`save` 会把：

```text
deleted
deletedAt
```

更新到数据库。

### 7. 为什么 `deleted` 字段要有默认值 false？

因为旧 Todo 在新增字段前并没有 deleted 值。

默认 false 表示旧数据都不是删除状态。

这样迁移可以兼容已有数据。

### 8. 如果用户恢复一个未删除 Todo，当前代码会怎样？

当前代码会把：

```text
deleted = false
deletedAt = null
```

重新设置一遍，然后保存。

这不会破坏数据。

以后如果希望更严格，可以要求只有 deleted=true 的 Todo 才能 restore。

### 9. 为什么测试要验证删除后列表查不到？

因为软删除最容易漏的是查询条件。

如果删除后列表还能查到，说明 `DeletedFalse` 条件没有加对。

### 10. 如果以后做“回收站列表”，Repository 需要什么查询？

可以新增：

```java
Page<Todo> findByUserIdAndDeletedTrue(Long userId, Pageable pageable);
```

它专门查询当前用户已删除的 Todo。
