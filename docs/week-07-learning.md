# 第七周学习说明：分页、排序、参数校验和字段扩展

第七周的目标是把 Todo 列表接口升级成更真实的后端列表查询接口。

第六周结束时，列表接口返回的是：

```java
List<TodoResponse>
```

也就是一次性返回所有 Todo。

第七周升级后，列表接口返回：

```java
PageResponse<TodoResponse>
```

包含当前页数据、页码、每页数量、总数量、总页数等分页信息。

## 本周最终改动总览

| 改动 | 文件 | 目的 |
|---|---|---|
| 新增 `TodoPriority` | `model/TodoPriority.java` | 表达 Todo 优先级 |
| Todo 增加 `priority` / `dueDate` | `model/Todo.java` | 增加业务字段 |
| 新增 Flyway V2 | `V2__add_priority_and_due_date_to_todos.sql` | 用迁移脚本修改表结构 |
| 列表查询改为分页 | `TodoRepository.java` / `TodoService.java` | 避免一次性返回所有数据 |
| 新增 `PageResponse<T>` | `dto/PageResponse.java` | 对外返回稳定的分页响应结构 |
| Controller 增加分页参数 | `TodoController.java` | 支持 `page` / `size` / `sort` |
| 增加 query 参数校验 | `TodoController.java` / `GlobalExceptionHandler.java` | 限制非法分页参数 |
| 测试扩展 | `TodoApiApplicationTests.java` | 覆盖分页、排序、字段扩展和参数错误 |

## 第一步：为什么列表接口需要分页？

如果接口一直这样设计：

```http
GET /api/todos
```

并且一次性返回所有数据，当数据量变大时会出现问题：

- 数据库查询变慢
- 后端内存压力变大
- 网络传输变大
- 前端渲染变慢
- 用户通常只需要看一部分数据

所以真实项目里的列表接口通常会分页：

```http
GET /api/todos?page=0&size=10
```

注意：Spring Data JPA 默认页码从 `0` 开始。

## 第二步：从 List 改成 Page

第五周的 Repository 方法返回：

```java
List<Todo> findByCompleted(boolean completed, Sort sort);
```

第七周改成：

```java
Page<Todo> findByCompleted(boolean completed, Pageable pageable);
```

为什么？

`List<Todo>` 只有数据本身。

`Page<Todo>` 除了当前页数据，还包含：

- 当前页码
- 每页数量
- 总数据量
- 总页数
- 是否第一页
- 是否最后一页

这就是分页接口需要的信息。

## 第三步：为什么不直接返回 `Page<Todo>`？

`Page<Todo>` 是 Spring Data 的内部分页模型。

如果直接返回给前端，会把很多框架字段暴露出去，而且响应结构不够可控。

所以新增：

```java
public class PageResponse<T> {
    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
```

对前端来说，分页响应更稳定：

```json
{
  "items": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

这延续了第六周的思想：

```text
内部模型不要直接暴露给前端。
```

## 第四步：Controller 增加分页参数

现在列表接口支持：

```http
GET /api/todos?page=0&size=10&sort=createdAt,desc
```

Controller 参数：

```java
@RequestParam(defaultValue = "0") int page
@RequestParam(defaultValue = "10") int size
@RequestParam(defaultValue = "id,asc") String sort
```

含义：

- `page`：第几页，从 0 开始
- `size`：每页多少条
- `sort`：排序字段和方向

例如：

```http
sort=createdAt,desc
```

表示按创建时间倒序。

## 第五步：为什么要限制 size？

如果不限制，客户端可以传：

```http
GET /api/todos?size=100000
```

这会让数据库和后端一次处理大量数据。

所以第七周加入校验：

```java
@Min(value = 1, message = "size 不能小于 1")
@Max(value = 100, message = "size 不能大于 100")
```

这样接口有了保护边界。

## 第六步：为什么要限制排序字段？

如果客户端传入任意排序字段：

```http
GET /api/todos?sort=unknown,desc
```

JPA 可能在运行时报错。

所以 Controller 中增加白名单：

```java
private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id",
        "title",
        "completed",
        "priority",
        "dueDate",
        "createdAt",
        "updatedAt"
);
```

只有白名单里的字段允许排序。

这是 API 参数防御的一种方式。

## 第七步：用 Flyway V2 增加字段

本周新增两个字段：

```text
priority  优先级
dueDate   截止日期
```

没有修改 V1，而是新增：

```text
V2__add_priority_and_due_date_to_todos.sql
```

内容：

```sql
ALTER TABLE todos
    ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';

ALTER TABLE todos
    ADD COLUMN due_date DATE;
```

为什么不改 V1？

因为 V1 是已经执行过的历史迁移。数据库结构继续演进时，应该新增 V2、V3，而不是修改历史。

## 第八步：新增 TodoPriority

新增枚举：

```java
public enum TodoPriority {
    LOW,
    MEDIUM,
    HIGH
}
```

为什么用 enum？

因为优先级不是任意字符串，它只有固定取值。

用 enum 可以让非法值更早暴露。

Entity 中使用：

```java
@Enumerated(EnumType.STRING)
private TodoPriority priority = TodoPriority.MEDIUM;
```

`EnumType.STRING` 表示数据库里保存 `LOW`、`MEDIUM`、`HIGH` 这些字符串。

## 第九步：请求和响应 DTO 同步新增字段

`CreateTodoRequest` 增加：

```java
private TodoPriority priority;
private LocalDate dueDate;
```

`UpdateTodoRequest` 增加：

```java
private TodoPriority priority;
private LocalDate dueDate;
```

`TodoResponse` 增加：

```java
private TodoPriority priority;
private LocalDate dueDate;
```

为什么三处都要改？

- 创建请求：允许创建时传优先级和截止日期。
- 修改请求：允许后续调整优先级和截止日期。
- 响应对象：把字段返回给客户端。

## 第十步：测试为什么要改？

因为列表响应结构变了。

以前是数组：

```json
[
  {
    "id": 1
  }
]
```

现在是分页对象：

```json
{
  "items": [
    {
      "id": 1
    }
  ],
  "page": 0,
  "size": 10
}
```

所以测试断言也要从：

```java
jsonPath("$", hasSize(1))
```

改成：

```java
jsonPath("$.items", hasSize(1))
```

新增测试覆盖：

- 分页
- 排序
- page 参数非法
- size 参数非法
- sort 字段非法
- 新增字段 `priority` / `dueDate`
- 默认优先级 `MEDIUM`

## 当前 API 示例

分页查询：

```http
GET /api/todos?page=0&size=10
```

筛选 + 分页：

```http
GET /api/todos?page=0&size=10&completed=false
```

搜索 + 排序：

```http
GET /api/todos?page=0&size=10&keyword=java&sort=createdAt,desc
```

创建 Todo：

```json
{
  "title": "实现分页接口",
  "priority": "HIGH",
  "dueDate": "2026-07-20"
}
```

## 本周复盘问题

1. 为什么列表接口需要分页？
2. `Page<Todo>` 和 `List<Todo>` 有什么区别？
3. 为什么不直接把 `Page<Todo>` 返回给前端？
4. 为什么要限制 `size` 最大值？
5. 为什么要限制排序字段白名单？
6. 为什么新增字段要写 V2，而不是修改 V1？
7. `TodoPriority` 为什么适合用 enum？

## 本周复盘问题参考答案

### 1. 为什么列表接口需要分页？

因为真实数据量可能很大，一次性返回所有数据会影响数据库、后端、网络和前端渲染性能。

分页可以让客户端一次只请求一小部分数据。

### 2. `Page<Todo>` 和 `List<Todo>` 有什么区别？

`List<Todo>` 只有当前数据集合。

`Page<Todo>` 除了数据，还包含分页元信息：

- 当前页
- 每页数量
- 总数量
- 总页数
- 是否第一页
- 是否最后一页

分页接口需要这些信息。

### 3. 为什么不直接把 `Page<Todo>` 返回给前端？

因为 `Page<Todo>` 是 Spring Data 的内部模型，字段多且不一定稳定。

自定义 `PageResponse<T>` 可以让 API 响应更清晰、更稳定。

### 4. 为什么要限制 `size` 最大值？

因为客户端可能传很大的 size，导致数据库和后端一次处理过多数据。

限制 `size <= 100` 是为了保护接口性能和系统稳定性。

### 5. 为什么要限制排序字段白名单？

因为排序字段来自客户端输入，不能完全信任。

白名单可以避免非法字段导致运行时错误，也让 API 支持的排序能力更明确。

### 6. 为什么新增字段要写 V2，而不是修改 V1？

因为 V1 是已经执行过的数据库迁移历史。

修改 V1 会破坏 Flyway 校验。

新增 V2 可以让数据库结构变化可追踪、可回放。

### 7. `TodoPriority` 为什么适合用 enum？

因为优先级只有固定取值：

```text
LOW
MEDIUM
HIGH
```

用 enum 比字符串更安全，可以避免拼写错误和非法值。

## 更多复盘问题

1. `Pageable` 的作用是什么？
2. `PageRequest.of(page, size, sort)` 做了什么？
3. `sort=createdAt,desc` 是如何被解析的？
4. 为什么 `dueDate` 使用 `LocalDate`，而不是 `LocalDateTime`？
5. 为什么默认 priority 是 `MEDIUM`？
6. query 参数校验失败为什么返回 400？
7. 分页接口的测试为什么要验证 `totalElements` 和 `totalPages`？

## 更多复盘问题参考答案

### 1. `Pageable` 的作用是什么？

`Pageable` 封装分页和排序请求。

它包含：

- page
- size
- sort

Repository 接收到 `Pageable` 后，Spring Data JPA 会生成带分页和排序的 SQL。

### 2. `PageRequest.of(page, size, sort)` 做了什么？

它创建一个 `Pageable` 对象。

例如：

```java
PageRequest.of(0, 10, Sort.by("createdAt").descending())
```

表示查询第 0 页，每页 10 条，按 `createdAt` 倒序。

### 3. `sort=createdAt,desc` 是如何被解析的？

Controller 会把字符串按逗号拆开：

```text
createdAt,desc
```

得到：

```text
字段：createdAt
方向：desc
```

然后转换成：

```java
Sort.by(Sort.Direction.DESC, "createdAt")
```

### 4. 为什么 `dueDate` 使用 `LocalDate`，而不是 `LocalDateTime`？

截止日期通常只关心日期，不关心具体时分秒。

例如：

```text
2026-07-20
```

所以 `LocalDate` 更合适。

而 `createdAt` / `updatedAt` 需要记录具体时间点，所以使用 `LocalDateTime`。

### 5. 为什么默认 priority 是 `MEDIUM`？

因为创建 Todo 时，客户端可以不传 priority。

如果不设置默认值，数据库的非空字段就可能没有值。

`MEDIUM` 是一个合理的中间默认优先级。

### 6. query 参数校验失败为什么返回 400？

因为这是客户端传参错误。

例如：

```http
GET /api/todos?size=101
```

不符合接口约束，所以返回：

```text
400 Bad Request
```

### 7. 分页接口的测试为什么要验证 `totalElements` 和 `totalPages`？

因为分页接口不只返回数据，还返回分页元信息。

如果 `items` 正确，但 `totalElements` 或 `totalPages` 错了，前端分页器仍然会显示错误。

所以分页测试要同时验证数据和分页信息。
