# Java Todo API

基于 Spring Boot 的 Todo REST API，提供 Todo 的创建、查询、修改、切换完成状态、删除、筛选等能力。

## 功能特性

- RESTful Todo API
- 请求 / 响应 DTO 分层
- 全局异常处理
- Bean Validation 参数校验
- Spring Data JPA 数据持久化
- Flyway 数据库迁移
- 默认使用 H2 本地数据库
- 支持 PostgreSQL profile
- MockMvc 接口测试

## 技术栈

- Java 21
- Maven
- Spring Boot 3.3.2
- Spring Web
- Spring Validation
- Spring Data JPA
- Flyway
- H2 Database
- PostgreSQL Driver
- JUnit 5 / MockMvc

## 架构说明

```text
HTTP 请求
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
├── controller   HTTP 接口入口
├── dto          请求 / 响应对象
├── exception    自定义异常和全局异常处理
├── mapper       Entity 到 DTO 的转换
├── model        JPA Entity
├── repository   Spring Data JPA Repository
└── service      业务逻辑
```

## 项目结构

```text
java-todo-api/
├── pom.xml
├── README.md
├── docs/
│   ├── week-01-learning.md
│   ├── week-02-learning.md
│   ├── week-03-learning.md
│   ├── week-04-learning.md
│   ├── week-05-learning.md
│   └── week-06-learning.md
└── src/
    ├── main/
    │   ├── java/com/zading/todoapi/
    │   │   ├── TodoApiApplication.java
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── exception/
    │   │   ├── mapper/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   └── resources/
    │       ├── application.properties
    │       ├── application-postgres.properties
    │       └── db/migration/
    │           └── V1__create_todos_table.sql
    └── test/
        ├── java/com/zading/todoapi/
        └── resources/
            └── application-test.properties
```

## 环境要求

- JDK 21
- Maven 3.9+

PostgreSQL 是可选依赖。默认 profile 使用 H2，因此本地没有安装 PostgreSQL 也可以直接运行。

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

### 测试配置

测试配置文件：

```text
src/test/resources/application-test.properties
```

测试环境使用 H2 内存数据库。

## 数据库迁移

Flyway 迁移脚本目录：

```text
src/main/resources/db/migration/
```

当前迁移脚本：

```text
V1__create_todos_table.sql
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

## 运行测试

```bash
mvn test
```

测试覆盖：

- 健康检查接口
- 创建 Todo
- 查询 Todo 列表
- 修改 Todo
- 切换完成状态
- 删除 Todo
- 按完成状态筛选
- 按标题关键词搜索
- 参数校验错误响应
- 资源不存在错误响应

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

### 查询 Todo 列表

```http
GET /api/todos
```

Query 参数：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `completed` | boolean | 否 | 按完成状态筛选 |
| `keyword` | string | 否 | 按标题关键词搜索 |

示例：

```http
GET /api/todos
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
    "title": "实现 Todo API",
    "completed": false,
    "createdAt": "2026-07-08T10:00:00.123456",
    "updatedAt": "2026-07-08T10:00:00.123456"
  }
]
```

### 查询单个 Todo

```http
GET /api/todos/{id}
```

示例：

```bash
curl http://localhost:8080/api/todos/1
```

### 创建 Todo

```http
POST /api/todos
Content-Type: application/json
```

请求体：

```json
{
  "title": "实现 Todo API"
}
```

示例：

```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"实现 Todo API"}'
```

成功状态码：

```text
201 Created
```

### 修改 Todo

```http
PATCH /api/todos/{id}
Content-Type: application/json
```

请求体：

```json
{
  "title": "更新 API 文档",
  "completed": true
}
```

两个字段都可以单独传。

示例：

```bash
curl -X PATCH http://localhost:8080/api/todos/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"更新 API 文档","completed":true}'
```

### 切换完成状态

```http
PATCH /api/todos/{id}/toggle
```

示例：

```bash
curl -X PATCH http://localhost:8080/api/todos/1/toggle
```

### 删除 Todo

```http
DELETE /api/todos/{id}
```

示例：

```bash
curl -X DELETE http://localhost:8080/api/todos/1
```

成功状态码：

```text
204 No Content
```

## 错误响应

参数校验错误示例：

```json
{
  "timestamp": "2026-07-08T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "title: 任务标题不能为空",
  "path": "/api/todos"
}
```

资源不存在示例：

```json
{
  "timestamp": "2026-07-08T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Todo 不存在，id = 999",
  "path": "/api/todos/999"
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
