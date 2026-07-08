# 第四周学习内容：Spring Boot Todo API

这一周的目标是把 Todo 项目从控制台程序升级成后端 API。

你是前端开发，所以这周会进入一个很熟悉的世界：HTTP、JSON、接口、状态码、请求体、响应体。

## 本周核心主线

```text
前端/客户端发请求
-> Controller 接收请求
-> Service 执行业务逻辑
-> Repository 保存或读取数据
-> Controller 返回 JSON
```

当前项目先用内存存储，不接数据库。

原因是：第四周的重点是 Spring Boot 和 REST API，不要同时被数据库、ORM、SQL 分散注意力。

## 第四周每一步改了什么，为什么这样改

这一周是从“Java 控制台程序”走向“后端 API 项目”的第一步。你可以把它理解为：以前程序只和终端交互，现在程序开始通过 HTTP 和前端、浏览器、curl 交互。

### 1. 新建 Maven + Spring Boot 工程

做了什么：

- 新建 `pom.xml`
- 引入 `spring-boot-starter-web`
- 新建 `TodoApiApplication.java`
- 新建 `application.properties`

为什么这么做：

- Maven 类似前端里的 `package.json`，负责依赖和构建。
- `spring-boot-starter-web` 帮我们一次性引入 Web API 需要的能力。
- `TodoApiApplication` 是程序入口，相当于整个后端服务的启动开关。
- `application.properties` 用来放端口、数据库、日志等配置。

这一阶段你要建立的感觉是：

```text
Java 文件不是散着跑的，而是被 Maven 管理，被 Spring Boot 启动。
```

### 2. 添加 HelloController

做了什么：

- 新增 `/hello` 接口
- 返回简单字符串 `Hello Spring Boot`

为什么这么做：

- 这是最小可运行 API。
- 它帮你确认：Spring Boot 能启动，端口能监听，浏览器/curl 能访问。
- 学后端时，先跑通最小链路，比一开始写完整业务更重要。

你可以把 `/hello` 理解为后端项目里的“点火测试”。

### 3. 添加 TodoController

做了什么：

- 新增 `/api/todos` 相关接口
- 使用 `@RestController`
- 使用 `@RequestMapping`
- 使用 `@GetMapping`、`@PostMapping`、`@PatchMapping`、`@DeleteMapping`

为什么这么做：

- Controller 是 HTTP 入口。
- 它负责把 URL、请求方法、请求体这些 HTTP 概念接进 Java 世界。
- 前端请求不会直接进 Service，也不会直接进 Repository；它先到 Controller。

核心理解：

```text
Controller 负责“接待请求”，不是负责“干所有活”。
```

### 4. 添加 DTO：CreateTodoRequest / UpdateTodoRequest

做了什么：

- 新增 `CreateTodoRequest`
- 新增 `UpdateTodoRequest`
- 在 Controller 中用 `@RequestBody` 接收 JSON

为什么这么做：

- 前端传来的 JSON 需要变成 Java 对象。
- DTO 专门表达“接口输入”，不要直接拿数据库对象接收请求。
- 创建和更新的字段规则不一样，所以拆成两个请求对象更清晰。

前端类比：

```ts
type CreateTodoRequest = {
  title: string
}

type UpdateTodoRequest = {
  title?: string
  completed?: boolean
}
```

### 5. 添加 TodoService

做了什么：

- 新增 `TodoService`
- 把新增、修改、切换完成状态、删除等逻辑放进去

为什么这么做：

- Controller 不应该堆满业务逻辑。
- Service 是业务层，负责判断“这件事该怎么做”。
- 以后即使接口入口变了，比如从 HTTP 变成消息队列，业务逻辑仍然可以复用。

你可以这样记：

```text
Controller 管 HTTP
Service 管业务
Repository 管数据
```

### 6. 添加 Repository 接口和内存实现

做了什么：

- 新增 `TodoRepository` 接口
- 新增 `InMemoryTodoRepository`
- 用 `Map<Long, Todo>` 临时保存数据

为什么这么做：

- 第四周重点是 API，不是数据库。
- 用内存 Repository 可以快速跑通 CRUD。
- 先定义接口，是为了以后替换成 JPA Repository 时，Service 层不用大改。

这一步非常工程化：

```text
先依赖抽象，再替换实现。
```

### 7. 添加统一异常处理

做了什么：

- 新增 `TodoNotFoundException`
- 新增 `GlobalExceptionHandler`
- 新增 `ApiError`

为什么这么做：

- 后端不能把 Java 异常堆栈直接丢给前端。
- 业务错误应该转换成明确的 HTTP 状态码。
- 比如 Todo 不存在应该返回 `404`，请求参数错误应该返回 `400`。

这一步的核心是：

```text
异常是 Java 内部问题；HTTP 响应是给客户端看的协议。
```

### 8. 添加 MockMvc 测试

做了什么：

- 添加接口测试
- 验证创建、查询、修改、删除、错误响应

为什么这么做：

- 后端不是“启动能跑”就算完成。
- API 的状态码、JSON 字段、错误格式都应该可验证。
- 测试可以保护你后面重构，比如第五周换成数据库时，接口行为仍然不变。

这就是工程化学习的关键：

```text
功能会变，测试保护行为不变。
```

## Day 1：理解 Spring Boot 项目结构

重点文件：

- `pom.xml`
- `TodoApiApplication.java`
- `application.properties`

你要理解：

```text
pom.xml              项目依赖配置，类似 package.json
TodoApiApplication   程序入口
application.properties 项目配置
```

核心注解：

```java
@SpringBootApplication
```

它表示：

```text
这是一个 Spring Boot 应用，从这里启动。
```

今日任务：

```bash
mvn spring-boot:run
```

然后访问：

```text
http://localhost:8080/hello
```

## Day 2：理解 Controller

重点文件：

- `HelloController.java`
- `TodoController.java`

Controller 的职责：

```text
接收 HTTP 请求
调用 Service
返回响应
```

重点注解：

```java
@RestController
@RequestMapping
@GetMapping
```

前端类比：

```js
app.get('/api/todos', handler)
```

在 Spring Boot 里写成：

```java
@GetMapping("/api/todos")
```

今日任务：

用 curl 调：

```bash
curl http://localhost:8080/api/todos
```

## Day 3：理解 POST 和 @RequestBody

重点文件：

- `CreateTodoRequest.java`
- `TodoController.java`

你要理解：

```java
@RequestBody
```

它的作用是：

```text
把请求 JSON 转成 Java 对象。
```

请求：

```json
{
  "title": "学习 Spring Boot"
}
```

会变成：

```java
CreateTodoRequest
```

今日任务：

```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"学习 Spring Boot"}'
```

观察返回的：

```text
201 Created
```

## Day 4：理解 Service 层

重点文件：

- `TodoService.java`

Service 的职责：

```text
处理业务逻辑，不处理 HTTP 细节。
```

比如：

- 标题不能为空
- 新增 Todo 默认未完成
- Todo 不存在时抛异常
- 切换完成状态

Controller 不应该写太多业务逻辑。

一个健康的后端结构通常是：

```text
Controller 薄一点
Service 厚一点
Repository 专注数据
```

## Day 5：理解 Repository 层

重点文件：

- `TodoRepository.java`
- `InMemoryTodoRepository.java`

现在的结构是：

```text
TodoRepository           接口，定义数据访问能力
InMemoryTodoRepository   实现，把数据存在内存 Map 里
```

这样做的好处：

```text
以后换 MySQL，只新增 MySQLTodoRepository 或 JPA Repository。
Service 不需要大改。
```

今日重点：

看懂接口：

```java
public interface TodoRepository {
    List<Todo> findAll();
    Optional<Todo> findById(Long id);
    Todo save(Todo todo);
    boolean deleteById(Long id);
}
```

## Day 6：理解错误处理和状态码

重点文件：

- `TodoNotFoundException.java`
- `GlobalExceptionHandler.java`
- `ApiError.java`

你要掌握这些状态码：

```text
200 OK           请求成功
201 Created      创建成功
204 No Content   删除成功，无响应体
400 Bad Request  参数错误
404 Not Found    资源不存在
500 Internal Server Error 服务端错误
```

`@RestControllerAdvice` 的作用：

```text
统一处理 Controller 抛出来的异常。
```

比如 Service 抛出：

```java
throw new TodoNotFoundException(id);
```

最终会变成：

```text
HTTP 404
```

## Day 7：测试和复盘

重点文件：

- `TodoApiApplicationTests.java`

测试使用：

```text
MockMvc
```

它可以不打开浏览器、不启动真实前端，直接模拟 HTTP 请求。

运行：

```bash
mvn test
```

你要看懂测试里这些断言：

```java
.andExpect(status().isCreated())
.andExpect(jsonPath("$.title").value("学习 Spring Boot API"))
```

它们的意思是：

```text
响应状态码应该是 201
响应 JSON 里的 title 应该是指定内容
```

## 本周结束你应该能说清楚

- Spring Boot 项目怎么启动
- Controller 是什么
- Service 是什么
- Repository 是什么
- 前端传 JSON 后端怎么接
- Java 对象怎么自动变成 JSON
- 什么情况下返回 400
- 什么情况下返回 404
- 如何用 curl 测接口
- 如何用测试验证接口

## 本周复盘问题

1. 为什么要从控制台程序升级成 REST API？
2. Controller、Service、Repository 分别负责什么？
3. `@RequestBody` 的作用是什么？
4. 为什么要有统一异常处理？
5. 为什么第四周先用内存 Repository，而不是直接上数据库？
6. MockMvc 测试验证的是什么？

## 本周复盘问题参考答案

### 1. 为什么要从控制台程序升级成 REST API？

控制台程序主要给人通过终端使用，而 REST API 可以给前端、移动端或其他服务通过 HTTP 调用。

升级成 REST API 后，Java 程序从“本地交互程序”变成了“后端服务”。

调用链也变成：

```text
客户端 -> HTTP 请求 -> Spring Boot 后端 -> JSON 响应
```

这是真实 Web 后端项目的基础形态。

### 2. Controller、Service、Repository 分别负责什么？

Controller 负责 HTTP：

- 接收请求
- 读取路径参数、query 参数、请求体
- 返回状态码和 JSON

Service 负责业务逻辑：

- 新增 Todo
- 修改 Todo
- 校验业务规则
- 判断 Todo 是否存在

Repository 负责数据访问：

- 查询
- 保存
- 删除

一句话：

```text
Controller 管入口，Service 管业务，Repository 管数据。
```

### 3. `@RequestBody` 的作用是什么？

`@RequestBody` 用来把 HTTP 请求体里的 JSON 转成 Java 对象。

例如请求体：

```json
{
  "title": "学习 Spring Boot"
}
```

可以被转换成：

```java
CreateTodoRequest
```

这样 Controller 就不用自己手动解析 JSON。

### 4. 为什么要有统一异常处理？

因为后端内部异常不应该直接暴露给客户端。

统一异常处理可以把 Java 异常转换成稳定的 HTTP 响应。

例如：

```java
throw new TodoNotFoundException(id);
```

最终返回：

```text
HTTP 404
```

并带上统一格式的错误 JSON。

### 5. 为什么第四周先用内存 Repository，而不是直接上数据库？

因为第四周的重点是 Spring Boot、HTTP、JSON、Controller、Service、Repository 和测试。

如果一开始就加入数据库、JPA、SQL，学习负担会变大。

先用内存 Repository 可以快速跑通 API 主链路：

```text
HTTP -> Controller -> Service -> Repository -> JSON
```

第五周再把底层存储替换成数据库。

### 6. MockMvc 测试验证的是什么？

MockMvc 用来模拟 HTTP 请求，验证接口行为。

它可以验证：

- 状态码是否正确
- 响应 JSON 是否正确
- 参数错误是否返回 400
- 资源不存在是否返回 404
- 创建、修改、删除流程是否正常

MockMvc 不需要真实浏览器，也不需要前端页面。

## 建议学习节奏

每天 2 小时：

```text
20 分钟：读当天相关代码
60 分钟：运行、修改、调接口
25 分钟：看测试/补测试
15 分钟：写笔记
```

别怕 Spring Boot 一开始“自动得有点玄学”。你先抓住请求链路，其他注解会慢慢变得顺眼。

## 更多复盘问题

1. `@RestController` 和普通 Java 类有什么区别？
2. `@PathVariable` 和 `@RequestBody` 分别读取什么？
3. 为什么创建成功要返回 `201 Created`？
4. 为什么删除成功可以返回 `204 No Content`？
5. `CreateTodoRequest` 为什么不直接使用 `Todo`？
6. 为什么 API 错误响应要保持统一格式？
7. curl 测试接口和 MockMvc 测试接口有什么区别？

## 更多复盘问题参考答案

### 1. `@RestController` 和普通 Java 类有什么区别？

普通 Java 类只是一个普通对象，Spring Boot 不会自动把它当成接口入口。

加上 `@RestController` 后，Spring 会把这个类注册为 Controller，并把方法返回值自动转换成 HTTP 响应。

例如返回一个对象时，Spring 会把它序列化成 JSON。

所以：

```text
@RestController = HTTP Controller + JSON 响应
```

### 2. `@PathVariable` 和 `@RequestBody` 分别读取什么？

`@PathVariable` 读取 URL 路径上的变量。

例如：

```http
GET /api/todos/1
```

对应：

```java
@PathVariable Long id
```

`@RequestBody` 读取请求体里的 JSON。

例如：

```json
{
  "title": "学习 Spring Boot"
}
```

对应：

```java
@RequestBody CreateTodoRequest request
```

### 3. 为什么创建成功要返回 `201 Created`？

`201 Created` 表示服务端成功创建了一个新资源。

相比 `200 OK`，它更精确地表达了语义。

创建 Todo 后，接口还可以通过 `Location` 响应头告诉客户端新资源地址：

```text
Location: /api/todos/1
```

这更符合 REST API 的设计习惯。

### 4. 为什么删除成功可以返回 `204 No Content`？

`204 No Content` 表示请求成功，但响应体为空。

删除接口通常不需要再返回被删除的数据。

所以：

```text
DELETE 成功 -> 204 No Content
```

这比返回一个空 JSON 更清晰。

### 5. `CreateTodoRequest` 为什么不直接使用 `Todo`？

因为请求模型和业务/数据模型职责不同。

创建 Todo 时，前端只需要传：

```json
{
  "title": "学习 Java"
}
```

但 `Todo` 里可能有：

```text
id
completed
createdAt
updatedAt
```

这些字段不应该由前端创建时随便传。

所以用 `CreateTodoRequest` 明确接口允许传什么。

### 6. 为什么 API 错误响应要保持统一格式？

统一错误格式可以让前端更容易处理错误。

例如所有错误都包含：

```text
timestamp
status
error
message
path
```

前端就可以用同一套逻辑展示错误信息，而不是每个接口单独适配。

这也是 API 工程化的一部分。

### 7. curl 测试接口和 MockMvc 测试接口有什么区别？

curl 是手动调用接口，适合开发时快速验证。

MockMvc 是自动化测试，适合长期保护接口行为。

区别：

```text
curl     人手动执行
MockMvc  测试代码自动执行
```

项目越复杂，越需要 MockMvc 这类自动化测试。
