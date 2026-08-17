# Java Todo API

基于 Spring Boot 的 Todo REST API，提供用户注册、登录认证、JWT 鉴权，以及 Todo 的创建、查询、修改、切换完成状态、删除、筛选等能力。

## 功能特性

- RESTful Todo API
- 用户注册和登录
- JWT Token 鉴权
- Todo 数据按用户隔离
- Todo 软删除和恢复
- completedAt / deletedAt 生命周期字段
- 统一 API 响应结构
- 业务错误码
- 请求 / 响应 DTO 分层
- 分页、排序、筛选列表查询
- 全局异常处理
- Bean Validation 参数校验
- Spring Data JPA 数据持久化
- Flyway 数据库迁移
- 默认使用 H2 本地数据库
- 支持 PostgreSQL profile
- 提供 Docker Compose PostgreSQL 配置
- Swagger / OpenAPI 接口文档
- 请求日志记录
- 按功能拆分的 MockMvc 接口测试
- GitHub Actions CI 配置

## 技术栈

- Java 21
- Maven
- Spring Boot 3.3.2
- Spring Web
- Spring Validation
- Spring Security
- Spring Data JPA
- Springdoc OpenAPI
- Flyway
- H2 Database
- PostgreSQL Driver
- Docker Compose（可选）
- GitHub Actions（可选）
- JUnit 5 / MockMvc

## 架构说明

```text
HTTP 请求
  -> Security Filter
  -> Controller
  -> Service
  -> Repository
  -> Database

Entity
  -> Mapper
  -> Response DTO
```

主要包结构：

```text
com.zading.todoapi
├── config       工程配置，例如 OpenAPI 配置
├── controller   HTTP 接口入口
├── dto          请求 / 响应对象
├── exception    自定义异常和全局异常处理
├── logging      请求日志过滤器
├── mapper       Entity 到 DTO 的转换
├── model        JPA Entity
├── repository   Spring Data JPA Repository
├── security     JWT 和 Spring Security 配置
└── service      业务逻辑
```

## 项目结构

```text
java-todo-api/
├── .env.example
├── .github/
│   └── workflows/
│       └── ci.yml
├── docker-compose.yml
├── pom.xml
├── README.md
├── docs/
│   ├── week-01-learning.md
│   ├── week-02-learning.md
│   ├── week-03-learning.md
│   ├── week-04-learning.md
│   ├── week-05-learning.md
│   ├── week-06-learning.md
│   ├── week-07-learning.md
│   ├── week-08-learning.md
│   ├── week-09-learning.md
│   ├── week-10-learning.md
│   ├── week-11-learning.md
│   └── week-12-learning.md
└── src/
    ├── main/
    │   ├── java/com/zading/todoapi/
    │   │   ├── TodoApiApplication.java
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── exception/
    │   │   ├── logging/
    │   │   ├── mapper/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   ├── security/
    │   │   └── service/
    │   └── resources/
    │       ├── application.properties
    │       ├── application-postgres.properties
    │       └── db/migration/
    │           ├── V1__create_todos_table.sql
    │           ├── V2__add_priority_and_due_date_to_todos.sql
    │           ├── V3__create_users_and_link_todos.sql
    │           └── V4__add_todo_lifecycle_fields.sql
    └── test/
        ├── java/com/zading/todoapi/
        │   ├── ApplicationSmokeTests.java
        │   ├── AuthApiTests.java
        │   ├── OpenApiTests.java
        │   ├── TodoApiTests.java
        │   └── support/
        └── resources/
            └── application-test.properties
```

## 环境要求

- JDK 21
- Maven 3.9+

PostgreSQL 是可选依赖。默认 profile 使用 H2，因此本地没有安装 PostgreSQL 也可以直接运行。

Docker 也是可选工具。当前仓库提供了 `docker-compose.yml`，但不要求本机已经安装 Docker。

## 配置说明

### 默认配置

默认配置文件：

```text
src/main/resources/application.properties
```

默认数据源：

```properties
spring.datasource.url=jdbc:h2:file:./data/todo-db-v2;MODE=PostgreSQL
spring.datasource.username=sa
spring.datasource.password=
```

数据库文件会生成在：

```text
data/
```

`data/` 目录已加入 Git 忽略规则。

### PostgreSQL 配置

PostgreSQL 配置文件：

```text
src/main/resources/application-postgres.properties
```

默认连接信息：

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/java_todo_api}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:}
```

创建数据库：

```sql
CREATE DATABASE java_todo_api;
```

使用 PostgreSQL profile 启动：

```bash
DB_USERNAME=postgres DB_PASSWORD=your_password mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

如果本机以后安装了 Docker，可以使用仓库内置的 Compose 配置启动 PostgreSQL：

```bash
cp .env.example .env
docker compose up -d postgres
```

然后启动 PostgreSQL profile：

```bash
DB_USERNAME=postgres DB_PASSWORD=postgres mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

### 测试配置

测试配置文件：

```text
src/test/resources/application-test.properties
```

测试环境使用 H2 内存数据库。

### JWT 配置

默认配置文件提供了学习环境可用的 JWT 配置：

```properties
app.jwt.secret=${JWT_SECRET:java-todo-api-learning-secret-change-me-at-least-32-chars}
app.jwt.expiration-minutes=${JWT_EXPIRATION_MINUTES:120}
```

生产环境不要使用默认密钥，应该通过环境变量设置：

```bash
JWT_SECRET=your-strong-secret
JWT_EXPIRATION_MINUTES=120
```

### OpenAPI 配置

接口文档地址：

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON 地址：

```text
http://localhost:8080/v3/api-docs
```

### 请求日志配置

默认开启请求日志：

```properties
app.request-logging.enabled=true
```

日志会记录：

```text
HTTP GET /api/todos -> 200 (12 ms)
```

## 数据库迁移

Flyway 迁移脚本目录：

```text
src/main/resources/db/migration/
```

当前迁移脚本：

```text
V1__create_todos_table.sql
V2__add_priority_and_due_date_to_todos.sql
V3__create_users_and_link_todos.sql
V4__add_todo_lifecycle_fields.sql
```

JPA 不负责自动修改表结构：

```properties
spring.jpa.hibernate.ddl-auto=validate
```

数据库表结构由 Flyway 管理，JPA 只校验 Entity 模型和数据库表结构是否匹配。

## 启动项目

```bash
cd /Users/zading/Documents/Java/java-todo-api
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/hello
```

预期响应：

```text
Hello Spring Boot
```

接口文档：

```text
http://localhost:8080/swagger-ui.html
```

## 运行测试

```bash
mvn test
```

测试结构：

```text
src/test/java/com/zading/todoapi/
├── ApplicationSmokeTests.java   应用冒烟测试
├── AuthApiTests.java            注册 / 登录接口测试
├── OpenApiTests.java            OpenAPI 文档测试
├── TodoApiTests.java            Todo 业务接口测试
└── support/
    ├── AbstractApiTest.java     测试公共配置和数据清理
    ├── AuthTestClient.java      认证接口测试辅助类
    └── TodoTestClient.java      Todo 接口测试辅助类
```

测试覆盖：

- 健康检查接口
- 用户注册
- 用户登录
- 重复用户名注册失败
- 错误密码登录失败
- 未登录访问 Todo 返回 401
- 创建 Todo
- 查询 Todo 列表
- 修改 Todo
- 切换完成状态
- 删除 Todo
- 恢复软删除 Todo
- 按完成状态筛选
- 按标题关键词搜索
- 分页和排序
- priority / dueDate 字段
- completedAt / deletedAt 生命周期字段
- 软删除后默认查询不可见
- Todo 数据按用户隔离
- OpenAPI 文档可访问
- 参数校验错误响应
- 统一成功 / 错误响应结构
- 业务错误码
- 资源不存在错误响应

## CI

仓库提供 GitHub Actions 配置：

```text
.github/workflows/ci.yml
```

当前 CI 会在 `main` 分支的 push 和 pull request 上执行：

```text
mvn test
mvn package -DskipTests
```

CI 配置不会在本地自动执行；只有代码推送到 GitHub 并启用 Actions 后才会运行。

## 构建

```bash
mvn package
```

运行打包后的应用：

```bash
java -jar target/java-todo-api-1.0.0.jar
```

使用 PostgreSQL profile 运行打包后的应用：

```bash
DB_USERNAME=postgres DB_PASSWORD=your_password \
java -jar target/java-todo-api-1.0.0.jar --spring.profiles.active=postgres
```

## API 文档

### 健康检查

```http
GET /hello
```

响应：

```text
Hello Spring Boot
```

### 用户注册

```http
POST /api/auth/register
Content-Type: application/json
```

请求体：

```json
{
  "username": "zading",
  "password": "123456"
}
```

响应：

```json
{
  "success": true,
  "code": "CREATED",
  "message": "创建成功",
  "data": {
    "id": 1,
    "username": "zading",
    "createdAt": "2026-08-12T10:00:00.123456"
  },
  "path": null
}
```

### 用户登录

```http
POST /api/auth/login
Content-Type: application/json
```

请求体：

```json
{
  "username": "zading",
  "password": "123456"
}
```

响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "token": "xxxxx.yyyyy.zzzzz",
    "tokenType": "Bearer"
  },
  "path": null
}
```

### 统一响应结构

除 `/hello` 健康检查接口外，业务 API 统一使用下面的响应结构：

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {},
  "path": null
}
```

字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| `success` | boolean | 请求是否成功 |
| `code` | string | 业务状态码，例如 `OK`、`TODO_NOT_FOUND` |
| `message` | string | 给前端展示或调试使用的消息 |
| `data` | object/null | 真正的业务数据 |
| `path` | string/null | 出错时的请求路径 |
 
前端接入时，业务数据统一从 `data` 字段中读取。

### 认证说明

除 `/hello`、`/api/auth/register`、`/api/auth/login`、`/h2-console/**`、`/v3/api-docs/**` 和 `/swagger-ui/**` 外，其他接口都需要登录。

访问 Todo API 时需要携带：

```http
Authorization: Bearer <token>
```

### 查询 Todo 列表

```http
GET /api/todos
Authorization: Bearer <token>
```

Query 参数：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `completed` | boolean | 否 | 按完成状态筛选 |
| `keyword` | string | 否 | 按标题关键词搜索 |
| `page` | number | 否 | 页码，从 0 开始，默认 0 |
| `size` | number | 否 | 每页数量，默认 10，最大 100 |
| `sort` | string | 否 | 排序规则，格式为 `字段,方向`，默认 `id,asc` |

示例：

```http
GET /api/todos
GET /api/todos?page=0&size=10
GET /api/todos?page=0&size=10&completed=true
GET /api/todos?page=0&size=10&keyword=java
GET /api/todos?page=0&size=10&completed=false&keyword=java&sort=createdAt,desc
```

响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "items": [
      {
        "id": 1,
        "title": "实现 Todo API",
        "completed": false,
        "deleted": false,
        "priority": "HIGH",
        "dueDate": "2026-09-20",
        "completedAt": null,
        "deletedAt": null,
        "createdAt": "2026-08-12T10:00:00.123456",
        "updatedAt": "2026-08-12T10:00:00.123456"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "path": null
}
```

### 查询单个 Todo

```http
GET /api/todos/{id}
Authorization: Bearer <token>
```

示例：

```bash
curl http://localhost:8080/api/todos/1 \
  -H "Authorization: Bearer <token>"
```

### 创建 Todo

```http
POST /api/todos
Content-Type: application/json
Authorization: Bearer <token>
```

请求体：

```json
{
  "title": "实现 Todo API",
  "priority": "HIGH",
  "dueDate": "2026-09-20"
}
```

示例：

```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"title":"实现 Todo API","priority":"HIGH","dueDate":"2026-09-20"}'
```

成功状态码：

```text
201 Created
```

### 修改 Todo

```http
PATCH /api/todos/{id}
Content-Type: application/json
Authorization: Bearer <token>
```

请求体：

```json
{
  "title": "更新 API 文档",
  "completed": true,
  "priority": "LOW",
  "dueDate": "2026-09-25"
}
```

两个字段都可以单独传。

示例：

```bash
curl -X PATCH http://localhost:8080/api/todos/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"title":"更新 API 文档","completed":true,"priority":"LOW","dueDate":"2026-09-25"}'
```

### 切换完成状态

```http
PATCH /api/todos/{id}/toggle
Authorization: Bearer <token>
```

示例：

```bash
curl -X PATCH http://localhost:8080/api/todos/1/toggle \
  -H "Authorization: Bearer <token>"
```

完成状态规则：

```text
未完成 -> 完成：completed = true，completedAt 写入当前时间
完成 -> 未完成：completed = false，completedAt 清空为 null
```

### 删除 Todo

```http
DELETE /api/todos/{id}
Authorization: Bearer <token>
```

示例：

```bash
curl -X DELETE http://localhost:8080/api/todos/1 \
  -H "Authorization: Bearer <token>"
```

成功状态码：

```text
200 OK
```

响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "删除成功",
  "data": null,
  "path": null
}
```

删除行为说明：

```text
当前删除是软删除：数据库记录不会物理删除，而是设置 deleted = true，并写入 deletedAt。
普通查询默认只返回 deleted = false 的 Todo。
```

### 恢复 Todo

```http
PATCH /api/todos/{id}/restore
Authorization: Bearer <token>
```

示例：

```bash
curl -X PATCH http://localhost:8080/api/todos/1/restore \
  -H "Authorization: Bearer <token>"
```

响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "恢复成功",
  "data": {
    "id": 1,
    "title": "实现 Todo API",
    "completed": false,
    "deleted": false,
    "priority": "HIGH",
    "dueDate": "2026-09-20",
    "completedAt": null,
    "deletedAt": null,
    "createdAt": "2026-08-12T10:00:00.123456",
    "updatedAt": "2026-08-12T10:05:00.123456"
  },
  "path": null
}
```

## 错误响应

参数校验错误示例：

```json
{
  "success": false,
  "code": "VALIDATION_FAILED",
  "message": "title: 任务标题不能为空",
  "data": null,
  "path": "/api/todos"
}
```

资源不存在示例：

```json
{
  "success": false,
  "code": "TODO_NOT_FOUND",
  "message": "Todo 不存在，id = 999",
  "data": null,
  "path": "/api/todos/999"
}
```

未登录示例：

```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "请先登录",
  "data": null,
  "path": "/api/todos"
}
```

## 文档

学习文档位于 `docs/`：

- [第 1 周：Java 基础和开发环境](docs/week-01-learning.md)
- [第 2 周：面向对象、异常、集合和 Maven](docs/week-02-learning.md)
- [第 3 周：控制台 Todo 项目和分层重构](docs/week-03-learning.md)
- [第 4 周：Spring Boot Todo API](docs/week-04-learning.md)
- [第 5 周：JPA 和数据库持久化](docs/week-05-learning.md)
- [第 6 周：Profile、Flyway 和 DTO 分层](docs/week-06-learning.md)
- [第 7 周：分页、排序、参数校验和字段扩展](docs/week-07-learning.md)
- [第 8 周：用户注册、登录认证、JWT 和 Todo 数据隔离](docs/week-08-learning.md)
- [第 9 周：Docker Compose、PostgreSQL、OpenAPI 和请求日志](docs/week-09-learning.md)
- [第 10 周：测试体系重构、集成测试思维和 CI 准备](docs/week-10-learning.md)
- [第 11 周：软删除、恢复接口和 Todo 生命周期](docs/week-11-learning.md)
- [第 12 周：统一响应结构、错误码和参数校验](docs/week-12-learning.md)
