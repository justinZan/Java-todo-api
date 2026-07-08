# 第五周学习说明：Spring Boot + JPA + 数据库持久化

这一周的目标是把 Todo API 从“内存存储”升级为“数据库存储”。

当前工程采用一个对学习很友好的配置：

- 默认运行：H2 文件数据库，不需要安装 PostgreSQL。
- PostgreSQL 运行：启用 `postgres` profile 后连接本地 PostgreSQL。
- 测试运行：H2 内存数据库，避免测试污染你的本地开发数据。

## 为什么不直接默认连接 PostgreSQL？

因为你的电脑现在还没有安装 PostgreSQL。

如果默认配置直接写成：

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/java_todo_api
```

Spring Boot 启动时会立刻尝试连接 PostgreSQL。数据库不存在或服务没启动时，项目就会启动失败。

所以我们现在这样设计：

```text
默认 profile
  -> H2
  -> 方便你马上运行和学习

postgres profile
  -> PostgreSQL
  -> 等你安装 PostgreSQL 后再启用
```

## 当前调用链

```text
HTTP 请求
  -> TodoController
  -> TodoService
  -> TodoRepository
  -> 数据库
```

这条链路就是后端 CRUD 项目的基本骨架。

## 第五周每一步改了什么，为什么这样改

第五周的核心变化是：底层数据来源从“内存 Map”变成“数据库”。接口路径尽量保持不变，这样前端调用方式不需要跟着大改。

### 1. 添加 JPA、H2、PostgreSQL 依赖

做了什么：

- 在 `pom.xml` 添加 `spring-boot-starter-data-jpa`
- 添加 `h2`
- 添加 `postgresql`

为什么这么做：

- `spring-boot-starter-data-jpa` 提供 ORM 和 Repository 能力。
- H2 让你没安装 PostgreSQL 时也能运行项目。
- PostgreSQL Driver 预留真实数据库连接能力。

这一步对应前端理解：

```text
安装 ORM 和数据库驱动，就像前端安装 Prisma / Sequelize / 数据库 client。
```

### 2. 把 Todo 改成 Entity

做了什么：

- 给 `Todo` 添加 `@Entity`
- 添加 `@Table(name = "todos")`
- 给 `id` 添加 `@Id`
- 给 `id` 添加 `@GeneratedValue`

为什么这么做：

- 普通 Java 类只存在于内存里。
- Entity 代表这个类要映射到数据库表。
- `@Id` 告诉 JPA 哪个字段是主键。
- `@GeneratedValue` 告诉 JPA 主键由数据库生成。

核心理解：

```text
Todo 对象
  <-> todos 数据表的一行记录
```

### 3. 删除内存 Repository 实现

做了什么：

- 删除 `InMemoryTodoRepository`
- 不再自己维护 `Map<Long, Todo>`

为什么这么做：

- 数据不能只活在内存里。
- 内存存储在服务重启后会丢失。
- 数据库才是后端项目的持久化来源。

这一变化的意义是：

```text
项目从 demo 存储，进入真实数据存储。
```

### 4. 用 JpaRepository 替代手写 Repository

做了什么：

```java
public interface TodoRepository extends JpaRepository<Todo, Long> {
}
```

为什么这么做：

- `JpaRepository` 已经内置常用 CRUD。
- 不需要自己写 `findById`、`save`、`deleteById`。
- Spring Data JPA 会在运行时帮你生成实现类。

你之前问过的两个点都在这里：

```java
existsById(id)
findByCompleted(completed, sort)
findByTitleContainingIgnoreCase(keyword, sort)
```

这些方法不是你自己实现的，是 Spring Data JPA 根据接口和方法名生成的。

### 5. Service 保持业务入口不变

做了什么：

- `TodoService` 继续负责新增、修改、删除、切换完成状态
- 底层调用从内存实现变成 JPA Repository

为什么这么做：

- Controller 不关心数据来自内存还是数据库。
- Service 不关心 SQL 怎么写。
- Repository 负责数据访问细节。

这就是分层的价值：

```text
替换底层实现时，上层 API 尽量不动。
```

### 6. 增加 query 参数筛选

做了什么：

- 支持 `GET /api/todos?completed=true`
- 支持 `GET /api/todos?keyword=java`
- 支持 `GET /api/todos?completed=false&keyword=java`

为什么这么做：

- 真实列表接口通常不只是返回全部。
- 前端经常需要筛选、搜索、排序。
- 这是你从 CRUD 走向列表查询设计的第一步。

对应代码：

```java
@RequestParam(required = false) Boolean completed
@RequestParam(required = false) String keyword
```

`required = false` 表示这个 query 参数可以不传。

### 7. 添加默认 H2 配置和 PostgreSQL profile

做了什么：

- 默认配置使用 H2
- `application-postgres.properties` 预留 PostgreSQL

为什么这么做：

- 你的电脑还没安装 PostgreSQL，默认连接 Postgres 会导致项目启动失败。
- H2 保证学习时“先跑起来”。
- Profile 让同一套代码可以用不同数据库运行。

这一点很像前端里的环境配置：

```text
.env.local
.env.test
.env.production
```

Spring Boot 里对应：

```text
application.properties
application-postgres.properties
```

### 8. 更新测试，让数据库版 API 仍然通过

做了什么：

- 测试使用 H2 内存数据库
- 测试仍然验证原来的 API 行为

为什么这么做：

- 测试不应该依赖你本地是否安装 PostgreSQL。
- 测试数据应该隔离，不能污染开发数据库。
- 从内存实现切到数据库实现后，测试能证明 API 行为没有坏。

这一步的工程意义很大：

```text
重构底层实现，测试保护外部行为。
```

## 1. Entity：Java 对象如何映射数据库表

文件：

```text
src/main/java/com/zading/todoapi/model/Todo.java
```

核心代码：

```java
@Entity
@Table(name = "todos")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private boolean completed;
}
```

你可以这样理解：

| Java | 数据库 |
|---|---|
| `Todo` 类 | `todos` 表 |
| `id` 字段 | `id` 列 |
| `title` 字段 | `title` 列 |
| `completed` 字段 | `completed` 列 |

## 2. Repository：不用自己手写 CRUD

文件：

```text
src/main/java/com/zading/todoapi/repository/TodoRepository.java
```

核心代码：

```java
public interface TodoRepository extends JpaRepository<Todo, Long> {
}
```

`JpaRepository<Todo, Long>` 的意思是：

- 这个 Repository 操作 `Todo` 表。
- `Todo` 的主键类型是 `Long`。

它会自动提供：

- `findAll()`
- `findById(id)`
- `save(todo)`
- `deleteById(id)`
- `existsById(id)`

这就是 Spring Data JPA 的强大之处：你写接口，Spring 帮你生成实现。

## 3. 方法名查询

本周新增了筛选能力：

```java
List<Todo> findByCompleted(boolean completed, Sort sort);

List<Todo> findByTitleContainingIgnoreCase(String keyword, Sort sort);

List<Todo> findByCompletedAndTitleContainingIgnoreCase(boolean completed, String keyword, Sort sort);
```

这些方法没有实现类，但可以直接用。

Spring Data JPA 会根据方法名推导查询语句：

```java
findByCompleted(true)
```

大概等价于：

```sql
select * from todos where completed = true;
```

```java
findByTitleContainingIgnoreCase("java")
```

大概等价于：

```sql
select * from todos where lower(title) like lower('%java%');
```

## 4. Controller：新增 query 参数

现在查询列表支持：

```http
GET /api/todos
GET /api/todos?completed=true
GET /api/todos?completed=false
GET /api/todos?keyword=java
GET /api/todos?completed=false&keyword=java
```

对应 Controller：

```java
@GetMapping
public List<Todo> getTodos(
        @RequestParam(required = false) Boolean completed,
        @RequestParam(required = false) String keyword
) {
    return todoService.getTodos(completed, keyword);
}
```

重点看 `@RequestParam`。

它负责读取 URL 上的 query 参数。

## 5. Service：业务逻辑仍然放在 Service

虽然 Repository 已经能操作数据库，但 Controller 不应该直接调用 Repository。

我们仍然保留：

```text
Controller -> Service -> Repository
```

原因是：

- Controller 只负责 HTTP。
- Service 负责业务判断。
- Repository 负责数据库。

这就是分层。

## 6. 如何运行

默认运行：

```bash
mvn spring-boot:run
```

使用 PostgreSQL 运行：

```bash
DB_USERNAME=postgres DB_PASSWORD=你的密码 mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

打包：

```bash
mvn package
```

运行测试：

```bash
mvn test
```

## 7. 本周你要重点掌握

- `@Entity`
- `@Table`
- `@Id`
- `@GeneratedValue`
- `JpaRepository`
- `@RequestParam`
- `application.properties`
- Spring profile
- H2 和 PostgreSQL 的区别

一句话总结：

> 这一周你不是在学“怎么连数据库”这么简单，而是在学后端项目最核心的数据层设计方式。

## 本周复盘问题

1. 为什么要把内存存储换成数据库？
2. Entity 是什么？
3. 为什么 `JpaRepository` 不用自己实现？
4. `findByCompleted`、`findByTitleContainingIgnoreCase` 为什么不用实现？
5. 为什么默认使用 H2，而不是直接使用 PostgreSQL？
6. 为什么 Controller 不应该直接调用 Repository？

## 本周复盘问题参考答案

### 1. 为什么要把内存存储换成数据库？

内存存储只在程序运行期间存在，服务一重启数据就会丢失。

数据库可以持久化数据：

```text
程序停止后，数据仍然保留
```

所以第五周的核心升级是：

```text
内存 Map -> 数据库表
```

### 2. Entity 是什么？

Entity 是 Java 对象和数据库表之间的映射。

例如：

```java
@Entity
@Table(name = "todos")
public class Todo {
    private Long id;
    private String title;
    private boolean completed;
}
```

对应数据库中的 `todos` 表。

一句话：

```text
Entity 是数据库表在 Java 代码中的表达。
```

### 3. 为什么 `JpaRepository` 不用自己实现？

因为 Spring Data JPA 会在运行时根据接口创建代理实现。

当你写：

```java
public interface TodoRepository extends JpaRepository<Todo, Long> {
}
```

它就自动拥有：

```java
findAll()
findById()
save()
deleteById()
existsById()
```

这些方法来自 `JpaRepository`，不需要自己写实现类。

### 4. `findByCompleted`、`findByTitleContainingIgnoreCase` 为什么不用实现？

这是 Spring Data JPA 的方法名查询。

Spring 会根据方法名解析查询条件。

例如：

```java
findByCompleted(true)
```

大致对应：

```sql
select * from todos where completed = true;
```

```java
findByTitleContainingIgnoreCase("java")
```

大致对应：

```sql
select * from todos
where lower(title) like lower('%java%');
```

前提是方法名里的字段要和 Entity 属性对上。

### 5. 为什么默认使用 H2，而不是直接使用 PostgreSQL？

因为本地不一定已经安装 PostgreSQL。

如果默认连接 PostgreSQL，数据库不存在或服务没启动时，项目会启动失败。

所以当前设计是：

```text
默认 profile -> H2 文件数据库
postgres profile -> PostgreSQL
test profile -> H2 内存数据库
```

这样既能马上运行，也能为后续 PostgreSQL 做准备。

### 6. 为什么 Controller 不应该直接调用 Repository？

Controller 的职责是处理 HTTP，Repository 的职责是操作数据，中间应该由 Service 承接业务逻辑。

如果 Controller 直接调用 Repository，业务规则容易散落在接口层里。

更清晰的结构是：

```text
Controller -> Service -> Repository
```

这样 Controller 更薄，Service 更容易测试和复用。

## 更多复盘问题

1. `@Entity` 和 `@Table` 分别有什么作用？
2. `@Id` 和 `@GeneratedValue` 为什么经常一起出现？
3. `JpaRepository<Todo, Long>` 里的两个泛型分别是什么意思？
4. `Sort` 的作用是什么？
5. `@RequestParam(required = false)` 表示什么？
6. H2 文件数据库和 H2 内存数据库有什么区别？
7. 为什么测试环境不直接使用本地开发数据库？

## 更多复盘问题参考答案

### 1. `@Entity` 和 `@Table` 分别有什么作用？

`@Entity` 告诉 JPA：这个类是数据库实体，需要映射到数据库表。

`@Table` 用来指定表名。

例如：

```java
@Entity
@Table(name = "todos")
public class Todo {
}
```

含义是：

```text
Todo 类 <-> todos 表
```

### 2. `@Id` 和 `@GeneratedValue` 为什么经常一起出现？

`@Id` 表示这个字段是主键。

`@GeneratedValue` 表示主键值由数据库或 JPA 自动生成。

例如：

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

这样新增 Todo 时不需要手动指定 id。

### 3. `JpaRepository<Todo, Long>` 里的两个泛型分别是什么意思？

第一个泛型 `Todo` 表示这个 Repository 操作的 Entity 类型。

第二个泛型 `Long` 表示主键类型。

```java
JpaRepository<Todo, Long>
```

含义是：

```text
操作 Todo 表
Todo 的 id 类型是 Long
```

### 4. `Sort` 的作用是什么？

`Sort` 用来指定查询结果排序方式。

例如：

```java
Sort.by(Sort.Direction.ASC, "id")
```

表示按 `id` 升序排序。

如果不指定排序，数据库返回顺序不一定稳定。

### 5. `@RequestParam(required = false)` 表示什么？

它表示这个 query 参数可以不传。

例如：

```java
@RequestParam(required = false) Boolean completed
```

可以处理：

```http
GET /api/todos
GET /api/todos?completed=true
```

如果不传，`completed` 就是 `null`。

Service 可以根据是否为 `null` 决定是否筛选。

### 6. H2 文件数据库和 H2 内存数据库有什么区别？

H2 文件数据库会把数据保存到磁盘文件。

```text
服务重启后数据还在
```

H2 内存数据库只存在于进程内存中。

```text
测试结束后数据消失
```

所以开发默认用文件数据库，测试用内存数据库。

### 7. 为什么测试环境不直接使用本地开发数据库？

因为测试会频繁创建、修改、删除数据。

如果直接使用开发数据库，可能污染本地数据。

测试数据库应该：

- 独立
- 可重复
- 易清理
- 不依赖本地历史数据

所以测试环境使用 H2 内存数据库更合适。
