# Java Todo API

这是 Java 工程化学习项目：用 Spring Boot 把 Todo 从控制台程序升级成 REST API，并逐步接入数据库持久化。

你现在的学习重点不是“数据库”，而是先理解：

```text
HTTP 请求 -> Controller -> Service -> Repository -> 数据库 -> 返回 JSON
```

## 技术栈

- Java 21
- Maven
- Spring Boot
- Spring Web
- Spring Validation
- Spring Data JPA
- Flyway
- H2 Database，默认开发环境使用
- PostgreSQL Driver，预留 Postgres 环境
- JUnit / MockMvc

## 项目结构

```text
java-todo-api/
├── pom.xml
├── README.md
├── docs/
│   ├── week-04-learning.md
│   ├── week-05-learning.md
│   └── week-06-learning.md
└── src/
    ├── main/
    │   ├── java/com/zading/todoapi/
    │   │   ├── TodoApiApplication.java
    │   │   ├── controller/
    │   │   │   ├── HelloController.java
    │   │   │   └── TodoController.java
    │   │   ├── dto/
    │   │   │   ├── ApiError.java
    │   │   │   ├── CreateTodoRequest.java
    │   │   │   ├── TodoResponse.java
    │   │   │   └── UpdateTodoRequest.java
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   └── TodoNotFoundException.java
    │   │   ├── mapper/
    │   │   │   └── TodoMapper.java
    │   │   ├── model/
    │   │   │   └── Todo.java
    │   │   ├── repository/
    │   │   │   └── TodoRepository.java
    │   │   └── service/
    │   │       └── TodoService.java
    │   └── resources/
    │       ├── application.properties
    │       ├── application-postgres.properties
    │       └── db/migration/
    │           └── V1__create_todos_table.sql
    └── test/
        ├── java/com/zading/todoapi/
        │   └── TodoApiApplicationTests.java
        └── resources/
            └── application-test.properties
```

## 启动项目

默认启动会使用 H2 文件数据库，所以你的电脑暂时没装 PostgreSQL 也可以运行。

```bash
cd /Users/zading/Documents/Java/java-todo-api
mvn spring-boot:run
```

启动成功后访问：

```text
http://localhost:8080/hello
```

返回：

```text
Hello Spring Boot
```

默认数据库文件会生成在：

```text
/Users/zading/Documents/Java/java-todo-api/data/todo-db-v2
```

这个目录已经加入 `.gitignore`，不会提交到代码仓库。

项目启动时，Flyway 会读取：

```text
src/main/resources/db/migration/V1__create_todos_table.sql
```

并自动创建 `todos` 表。JPA 现在只负责校验 Entity 和数据库表是否匹配。

## 切换到 PostgreSQL

等你本地安装 PostgreSQL 后，先创建数据库：

```sql
CREATE DATABASE java_todo_api;
```

然后用 `postgres` profile 启动：

```bash
cd /Users/zading/Documents/Java/java-todo-api
DB_USERNAME=postgres DB_PASSWORD=你的密码 mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

默认 PostgreSQL 配置在：

```text
src/main/resources/application-postgres.properties
```

默认连接信息：

```text
host: localhost
port: 5432
database: java_todo_api
username: postgres
password: 从 DB_PASSWORD 环境变量读取
```

## 在 VS Code 里正确运行

请用 VS Code 打开整个工程目录：

```text
/Users/zading/Documents/Java/java-todo-api
```

不要只打开单个 `.java` 文件，也不要直接点击普通 Java 的 `Run` 按钮来运行 `TodoApiApplication`。如果看到类似下面的命令，说明 VS Code 正在把它当成普通 Java 类运行：

```text
java -cp .../redhat.java/jdt_ws/... com.zading.todoapi.TodoApiApplication
```

这种运行方式不会自动带上 Maven 里的 Spring Boot 依赖，所以会出现：

```text
SpringApplication cannot be resolved
```

推荐两种方式：

1. 在 VS Code 终端运行：

   ```bash
   mvn spring-boot:run
   ```

2. 使用 VS Code 任务：

   ```text
   Terminal -> Run Task -> Run Spring Boot
   ```

如果 VS Code 仍然识别不到 Maven 依赖，可以执行：

```text
Command Palette -> Java: Clean Java Language Server Workspace
```

然后重新打开 `/Users/zading/Documents/Java/java-todo-api` 工程目录。

## 运行测试

```bash
mvn test
```

测试会验证：

- `/hello`
- 新增 Todo
- 查询 Todo
- 修改 Todo
- 切换完成状态
- 删除 Todo
- 按完成状态筛选
- 按标题关键词搜索
- 响应里包含 `createdAt` / `updatedAt`
- 参数错误返回 400
- 不存在资源返回 404

## API 文档

### 1. 健康/入门接口

```http
GET /hello
```

响应：

```text
Hello Spring Boot
```

### 2. 查询 Todo 列表

```http
GET /api/todos
```

支持筛选：

```http
GET /api/todos?completed=true
GET /api/todos?completed=false
GET /api/todos?keyword=java
GET /api/todos?completed=false&keyword=java
```

响应：

```json
[
  {
    "id": 1,
    "title": "学习 Spring Boot API",
    "completed": false,
    "createdAt": "2026-07-06T17:00:00.123456",
    "updatedAt": "2026-07-06T17:00:00.123456"
  }
]
```

curl：

```bash
curl http://localhost:8080/api/todos
```

### 3. 查询单个 Todo

```http
GET /api/todos/{id}
```

示例：

```bash
curl http://localhost:8080/api/todos/1
```

### 4. 新增 Todo

```http
POST /api/todos
Content-Type: application/json
```

请求体：

```json
{
  "title": "学习 POST 接口"
}
```

curl：

```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"学习 POST 接口"}'
```

成功响应状态码：

```text
201 Created
```

### 5. 修改 Todo

```http
PATCH /api/todos/{id}
Content-Type: application/json
```

请求体可以只传一个字段：

```json
{
  "title": "新的标题"
}
```

也可以同时传：

```json
{
  "title": "新的标题",
  "completed": true
}
```

curl：

```bash
curl -X PATCH http://localhost:8080/api/todos/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"新的标题","completed":true}'
```

### 6. 切换完成状态

```http
PATCH /api/todos/{id}/toggle
```

curl：

```bash
curl -X PATCH http://localhost:8080/api/todos/1/toggle
```

### 7. 删除 Todo

```http
DELETE /api/todos/{id}
```

curl：

```bash
curl -X DELETE http://localhost:8080/api/todos/1
```

成功响应状态码：

```text
204 No Content
```

## 错误响应示例

### 标题为空

```http
POST /api/todos
```

请求体：

```json
{
  "title": ""
}
```

响应：

```json
{
  "timestamp": "2026-07-06T...",
  "status": 400,
  "error": "Bad Request",
  "message": "title: 任务标题不能为空",
  "path": "/api/todos"
}
```

### Todo 不存在

```http
GET /api/todos/999
```

响应：

```json
{
  "timestamp": "2026-07-06T...",
  "status": 404,
  "error": "Not Found",
  "message": "Todo 不存在，id = 999",
  "path": "/api/todos/999"
}
```

## 前端调用示例

```js
const res = await fetch('http://localhost:8080/api/todos')
const todos = await res.json()
console.log(todos)
```

新增 Todo：

```js
await fetch('http://localhost:8080/api/todos', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    title: '从前端创建 Todo'
  })
})
```

## 这一周你要重点看懂

- `@SpringBootApplication`
- `@RestController`
- `@RequestMapping`
- `@GetMapping`
- `@PostMapping`
- `@PatchMapping`
- `@DeleteMapping`
- `@RequestBody`
- `@PathVariable`
- `@Service`
- `@Repository`
- `@RestControllerAdvice`
- `@Entity`
- `@Table`
- `@Id`
- `@GeneratedValue`
- `JpaRepository`
- `@PrePersist`
- `@PreUpdate`
- `@RequestParam`
- `Flyway`
- `DTO`
- `Mapper`

先不要急着背，先把请求跑通。Spring Boot 一开始像一台自动咖啡机：按钮很多，但你先学会点“美式”，后面再拆机器。
