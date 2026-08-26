# 数据库设计说明

本文说明 `java-todo-api` 的表结构、约束、索引和事务设计。数据库结构由 Flyway 管理，JPA Entity 只负责映射和校验，不负责自动改表。

## 1. 实体关系

```text
users
  1 ─────── N todos
              ├── 1 ─────── N todo_attachments
              └── 1 ─────── N todo_action_logs
```

| 表 | 职责 |
| --- | --- |
| `users` | 保存登录用户、密码摘要和角色 |
| `todos` | 保存 Todo 主数据、完成状态和软删除状态 |
| `todo_attachments` | 保存附件元数据，真实文件存储在文件系统或 volume |
| `todo_action_logs` | 保存 Todo 的创建、修改、完成、删除等操作记录 |

## 2. 为什么拆成多张表

一个用户可以拥有多个 Todo，所以 `todos.user_id` 是外键。

一个 Todo 可以有多个附件和操作日志。如果把附件字段或日志字段直接放到 `todos` 中，会导致：

- 一个 Todo 只能保存固定数量的附件；
- 重复保存 Todo 基本信息；
- 日志数量增加后表结构难以维护。

因此，附件和日志分别使用独立表，通过 `todo_id` 关联 Todo。

## 3. Flyway 迁移顺序

| 版本 | 变化 |
| --- | --- |
| V1 | 创建 `todos` 基础表 |
| V2 | 增加优先级和截止日期 |
| V3 | 创建用户表，并为 Todo 建立用户关联 |
| V4 | 增加完成时间、删除时间和软删除字段 |
| V5 | 创建 Todo 操作日志表 |
| V6 | 创建 Todo 附件表 |
| V7 | 增加用户角色 |
| V8 | 增加 Todo 列表、过期扫描和管理员查询索引 |
| V9 | 增加数据检查约束和 Todo 乐观锁版本号 |

新增数据库结构时，不应该修改已经执行过的旧脚本，而应该新增一个版本，例如：

```text
V10__add_xxx.sql
```

这样不同环境可以按照同样的顺序升级数据库。

## 4. 数据库约束

V9 增加了以下约束：

| 约束 | 作用 |
| --- | --- |
| `ck_todos_title_not_blank` | 防止标题只有空格 |
| `ck_todos_priority` | 只允许 `LOW`、`MEDIUM`、`HIGH` |
| `ck_users_role` | 只允许 `USER`、`ADMIN` |
| `ck_todo_action_logs_action` | 只允许项目定义的操作类型 |
| `ck_todo_attachments_file_size` | 防止附件大小出现负数 |

Java Service 层仍然需要校验，因为 Service 可以返回更友好的错误信息；数据库约束是最后一道防线，可以防止脚本、后台任务或其他程序绕过 API 写入非法数据。

## 5. JPA Entity 与数据库字段

`Todo` 的关键映射如下：

```java
@Entity
@Table(name = "todos")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Version
    @Column(nullable = false)
    private Long version;
}
```

说明：

- `@Table(name = "todos")` 把 Java 类映射到 `todos` 表；
- `@Column(nullable = false, length = 255)` 与数据库的 `VARCHAR(255) NOT NULL` 对齐；
- `@Version` 告诉 Hibernate 使用 `version` 字段进行乐观锁控制；
- `ddl-auto=validate` 会在启动时检查 Entity 和数据库结构是否匹配。

## 6. 乐观锁

两个请求同时读取版本为 `0` 的 Todo：

```text
请求 A 读取 version=0
请求 B 读取 version=0
请求 A 更新成功，version 变成 1
请求 B 更新时发现数据库 version 已经不是 0，更新失败
```

Hibernate 生成的更新逻辑可以理解为：

```sql
UPDATE todos
SET title = ?, version = 1
WHERE id = ? AND version = 0;
```

如果 `WHERE` 没有匹配到记录，说明其他请求已经更新过这条数据。这样可以避免后写入的请求无提示地覆盖先写入的数据。

## 7. 索引和查询的对应关系

当前 Todo 索引位于 `V8__add_todo_query_indexes.sql`：

| 索引 | 对应场景 |
| --- | --- |
| `idx_todos_user_deleted_id` | 用户 Todo 默认列表和稳定分页 |
| `idx_todos_user_deleted_completed_id` | 用户按完成状态筛选 |
| `idx_todos_deleted_completed_due_id` | 定时扫描未完成且已过期 Todo |
| `idx_todos_deleted_id` | 管理员按删除状态查询 |

索引不是越多越好。每增加一个索引，数据库写入时都需要额外维护它，所以应该从真实查询条件、排序和数据量出发设计。

## 8. H2 与 PostgreSQL

当前默认配置使用 H2，原因是没有安装数据库服务时也能运行和测试：

```properties
spring.datasource.url=jdbc:h2:file:./data/todo-db-v2;MODE=PostgreSQL
```

测试使用 H2 内存数据库，并执行完整 Flyway 迁移。`MODE=PostgreSQL` 可以帮助发现一部分兼容性问题，但不能完全替代 PostgreSQL。

以后有 PostgreSQL 环境时，可以使用：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

如果使用 Docker Compose，应用容器通过 `postgres:5432` 访问数据库；宿主机直接运行应用时才使用 `localhost:5432`。

## 9. 数据库设计检查清单

- 每张表是否有明确职责？
- 主键和外键是否完整？
- 必填字段是否有 `NOT NULL`？
- 枚举值是否需要数据库约束？
- 查询条件和排序是否有合适索引？
- 迁移脚本是否可重复地在新环境执行？
- 是否需要事务保证多表操作的一致性？
- 是否需要乐观锁处理并发更新？
