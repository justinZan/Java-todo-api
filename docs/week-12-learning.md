# 第十二周学习说明：统一响应结构、错误码和参数校验

第十二周的目标是把接口从“功能能用”升级成“前端接起来舒服、错误排查清楚、结构更像公司项目”。

前面几周我们已经有：

```text
Todo CRUD
数据库持久化
用户注册 / 登录
JWT 鉴权
分页 / 排序 / 筛选
软删除 / 恢复
测试体系
```

这一周不主要新增业务功能，而是优化接口体验。

最核心的一句话：

```text
接口不仅要能返回数据，还要稳定、可预期、方便前端处理。
```

## 本周最终改动总览

| 改动 | 文件 | 目的 |
|---|---|---|
| 新增统一响应类 | `ApiResponse.java` | 所有业务接口成功 / 失败都返回统一结构 |
| 新增错误码枚举 | `ErrorCode.java` | 用业务 code 表达具体错误类型 |
| 新增业务异常基类 | `BusinessException.java` | 让业务异常天然携带错误码 |
| 改造 TodoNotFoundException | `TodoNotFoundException.java` | Todo 不存在时返回 `TODO_NOT_FOUND` |
| 改造 UnauthorizedException | `UnauthorizedException.java` | 登录失败时返回 `INVALID_CREDENTIALS` |
| 改造 GlobalExceptionHandler | `GlobalExceptionHandler.java` | 集中处理异常并统一错误响应 |
| 改造 SecurityConfig | `SecurityConfig.java` | 未登录也返回统一错误结构 |
| 改造 AuthController | `AuthController.java` | 注册 / 登录成功响应包进 `data` |
| 改造 TodoController | `TodoController.java` | Todo API 成功响应包进 `data` |
| 增强请求参数校验 | `CreateTodoRequest.java` / `UpdateTodoRequest.java` | 校验标题长度、空标题、截止日期 |
| 改造测试辅助类 | `AuthTestClient.java` / `TodoTestClient.java` | 从 `data` 中读取 token 和 id |
| 扩展接口测试 | `AuthApiTests.java` / `TodoApiTests.java` | 覆盖统一响应、错误码和新校验规则 |
| 更新 README | `README.md` | 说明新的接口响应格式 |

## 第一步：为什么要统一响应结构？

之前接口响应大概是这样：

注册成功：

```json
{
  "id": 1,
  "username": "zading",
  "createdAt": "2026-08-12T10:00:00"
}
```

Todo 列表成功：

```json
{
  "items": [],
  "page": 0,
  "size": 10
}
```

错误响应：

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Todo 不存在，id = 999"
}
```

这些都能用，但是格式不统一。

前端接入时就会变成：

```text
成功时，有的接口直接取 response.data.id
成功时，有的接口取 response.data.items
错误时，又要取 response.data.message
未登录时，可能还是另一种格式
```

项目一大，这会变乱。

所以本周统一成：

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {},
  "path": null
}
```

错误时：

```json
{
  "success": false,
  "code": "TODO_NOT_FOUND",
  "message": "Todo 不存在，id = 999",
  "data": null,
  "path": "/api/todos/999"
}
```

前端现在可以形成稳定规则：

```text
success 判断成功失败
code 判断具体业务状态
message 展示错误提示
data 获取业务数据
path 辅助定位哪个接口出错
```

## 第二步：新增 ApiResponse

文件：

```text
src/main/java/com/zading/todoapi/dto/ApiResponse.java
```

核心字段：

```java
private boolean success;
private String code;
private String message;
private T data;
private String path;
```

这里最值得理解的是：

```java
public class ApiResponse<T>
```

`<T>` 是 Java 泛型。

它表示：

```text
ApiResponse 可以包装不同类型的数据。
```

例如：

```java
ApiResponse<TodoResponse>
ApiResponse<LoginResponse>
ApiResponse<PageResponse<TodoResponse>>
ApiResponse<Void>
```

如果没有泛型，你可能要写很多重复类：

```text
TodoApiResponse
LoginApiResponse
PageApiResponse
UserApiResponse
```

泛型让一个响应壳子可以装很多种业务数据。

## 第三步：为什么加静态工厂方法？

`ApiResponse` 里有这些方法：

```java
public static <T> ApiResponse<T> success(T data)
public static <T> ApiResponse<T> success(String message, T data)
public static <T> ApiResponse<T> created(T data)
public static ApiResponse<Void> error(ErrorCode errorCode, String message, String path)
```

这些方法叫静态工厂方法。

你可以这样创建响应：

```java
return ApiResponse.success(todoResponse);
```

而不是每次都写：

```java
return new ApiResponse<>(true, "OK", "成功", todoResponse, null);
```

这样做有三个好处：

```text
1. Controller 代码更短
2. 成功 code / message 不容易写错
3. 响应结构以后要改，只主要改 ApiResponse
```

这就是工程化里很常见的“把重复规则集中起来”。

## 第四步：为什么需要 ErrorCode？

HTTP 状态码只能表达大类：

```text
400 Bad Request
401 Unauthorized
404 Not Found
500 Internal Server Error
```

但是业务错误往往更细：

```text
用户名已存在
用户名或密码错误
Todo 不存在
参数校验失败
排序字段不支持
```

这些如果只靠 HTTP 状态码，会不够清楚。

所以新增：

```text
src/main/java/com/zading/todoapi/exception/ErrorCode.java
```

里面定义：

```java
TODO_NOT_FOUND
DUPLICATE_USERNAME
INVALID_CREDENTIALS
UNAUTHORIZED
VALIDATION_FAILED
BAD_REQUEST
INTERNAL_ERROR
```

每个错误码还绑定了 HTTP 状态码：

```java
TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "Todo 不存在")
```

意思是：

```text
业务 code = TODO_NOT_FOUND
HTTP status = 404
默认消息 = Todo 不存在
```

前端可以根据 `code` 做更精细的处理。

例如：

```ts
if (error.code === "INVALID_CREDENTIALS") {
  showToast("用户名或密码错误");
}
```

## 第五步：为什么加 BusinessException？

文件：

```text
src/main/java/com/zading/todoapi/exception/BusinessException.java
```

它的作用是：

```text
让业务异常天然携带 ErrorCode。
```

核心代码：

```java
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
}
```

以前异常只有 message：

```java
throw new RuntimeException("用户名已存在");
```

现在异常既有 message，也有 code：

```java
throw new BusinessException(ErrorCode.DUPLICATE_USERNAME, "用户名已存在");
```

这样全局异常处理器就能知道：

```text
这是什么错误？
应该返回什么 HTTP 状态码？
应该给前端什么 code？
```

## 第六步：改造 TodoNotFoundException

文件：

```text
src/main/java/com/zading/todoapi/exception/TodoNotFoundException.java
```

现在它继承：

```java
public class TodoNotFoundException extends BusinessException
```

构造方法：

```java
public TodoNotFoundException(Long id) {
    super(ErrorCode.TODO_NOT_FOUND, "Todo 不存在，id = " + id);
}
```

含义是：

```text
只要抛出 TodoNotFoundException
最终响应 code 就是 TODO_NOT_FOUND
HTTP 状态码就是 404
message 里带具体 id
```

这比散落在 Controller 里手动返回 404 更清晰。

## 第七步：改造 GlobalExceptionHandler

文件：

```text
src/main/java/com/zading/todoapi/exception/GlobalExceptionHandler.java
```

这一周最重要的注解：

```java
@RestControllerAdvice
```

你可以把它理解成：

```text
所有 Controller 的统一异常出口。
```

里面的方法：

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleBusinessException(...)
```

意思是：

```text
只要 Controller / Service 抛出了 BusinessException
就由这个方法统一处理
```

然后返回：

```java
ApiResponse.error(errorCode, exception.getMessage(), request.getRequestURI())
```

最终 JSON：

```json
{
  "success": false,
  "code": "TODO_NOT_FOUND",
  "message": "Todo 不存在，id = 999",
  "data": null,
  "path": "/api/todos/999"
}
```

这就是“异常集中治理”。

## 第八步：参数校验错误怎么处理？

请求体校验失败时，Spring 会抛：

```java
MethodArgumentNotValidException
```

比如创建 Todo 时：

```json
{
  "title": " "
}
```

因为 `title` 有：

```java
@NotBlank(message = "任务标题不能为空")
```

所以会进入：

```java
handleValidation(...)
```

最后返回：

```json
{
  "success": false,
  "code": "VALIDATION_FAILED",
  "message": "title: 任务标题不能为空",
  "data": null,
  "path": "/api/todos"
}
```

这里对前端很友好，因为前端能明确知道：

```text
哪个字段错了：title
错在哪里：任务标题不能为空
```

## 第九步：为什么 SecurityConfig 也要改？

未登录访问 Todo API 时，请求还没有进入 Controller。

也就是说：

```text
JWT 鉴权失败
  -> 被 Spring Security 拦住
  -> 不会进入 GlobalExceptionHandler
```

所以如果只改 `GlobalExceptionHandler`，未登录错误仍然可能是另一种 JSON 格式。

本周在 `SecurityConfig` 中把未登录响应也改成：

```java
ApiResponse.error(ErrorCode.UNAUTHORIZED, "请先登录", request.getRequestURI())
```

最终未登录响应：

```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "请先登录",
  "data": null,
  "path": "/api/todos"
}
```

这个点很工程化。

因为真实项目里常见问题就是：

```text
业务异常一种格式
参数异常一种格式
鉴权异常又一种格式
```

前端会很痛苦。

## 第十步：改造 AuthController

文件：

```text
src/main/java/com/zading/todoapi/controller/AuthController.java
```

注册接口现在返回：

```java
public ApiResponse<UserResponse> register(...)
```

登录接口现在返回：

```java
public ApiResponse<LoginResponse> login(...)
```

注册成功响应：

```json
{
  "success": true,
  "code": "CREATED",
  "message": "创建成功",
  "data": {
    "id": 1,
    "username": "zading",
    "createdAt": "2026-08-12T10:00:00"
  },
  "path": null
}
```

登录成功响应：

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

前端以后取 token 时要注意：

```ts
const token = response.data.data.token;
```

第一层 `data` 是 Axios 的响应体。

第二层 `data` 是我们后端统一响应里的业务数据。

## 第十一步：改造 TodoController

文件：

```text
src/main/java/com/zading/todoapi/controller/TodoController.java
```

列表接口现在返回：

```java
public ApiResponse<PageResponse<TodoResponse>> getTodos(...)
```

这个类型看起来长，但拆开很简单：

```text
ApiResponse<                 统一响应壳子
  PageResponse<              分页数据
    TodoResponse             每一条 Todo 的响应结构
  >
>
```

也就是：

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "items": [],
    "page": 0,
    "size": 10
  },
  "path": null
}
```

前端取 Todo 列表：

```ts
const items = response.data.data.items;
```

创建接口依然保留：

```text
201 Created
Location: /api/todos/{id}
```

但是响应体变成统一结构：

```json
{
  "success": true,
  "code": "CREATED",
  "message": "创建成功",
  "data": {
    "id": 1,
    "title": "实现 Todo API"
  },
  "path": null
}
```

删除接口本周也做了一个小调整：

```text
之前：204 No Content，没有响应体
现在：200 OK，返回统一响应体
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

为什么不继续用 204？

因为 204 的意思就是没有响应体。

如果我们想要“所有业务接口都返回统一结构”，删除接口就更适合返回 200 + JSON。

真实项目里两种都常见：

```text
严格 REST：DELETE 成功返回 204
前端体验优先：DELETE 成功返回 200 + 统一响应
```

本项目现在选择第二种，因为第十二周的学习重点是统一接口体验。

## 第十二步：增强 CreateTodoRequest 校验

文件：

```text
src/main/java/com/zading/todoapi/dto/CreateTodoRequest.java
```

现在标题字段：

```java
@NotBlank(message = "任务标题不能为空")
@Size(max = 100, message = "任务标题最多 100 个字符")
private String title;
```

含义：

```text
title 不能为空
title 不能全是空格
title 最多 100 个字符
```

截止日期：

```java
@FutureOrPresent(message = "截止日期不能早于今天")
private LocalDate dueDate;
```

含义：

```text
Todo 的截止日期不能是过去的日期
```

这不是技术必须，而是业务规则。

业务规则应该尽量靠近请求入口校验，避免脏数据进入 Service。

## 第十三步：增强 UpdateTodoRequest 校验

文件：

```text
src/main/java/com/zading/todoapi/dto/UpdateTodoRequest.java
```

更新 Todo 和创建 Todo 不一样。

创建时 title 必填。

更新时 title 可以不传，因为你可能只想更新：

```json
{
  "completed": true
}
```

所以这里不能用 `@NotBlank`。

本周使用：

```java
@Pattern(regexp = ".*\\S.*", message = "任务标题不能为空")
@Size(max = 100, message = "任务标题最多 100 个字符")
private String title;
```

这里的关键是：

```text
title 为 null 时，不校验
title 有值时，不能是空白字符串
```

这就是“可选字段校验”的思路。

截止日期同样增加：

```java
@FutureOrPresent(message = "截止日期不能早于今天")
private LocalDate dueDate;
```

## 第十四步：测试为什么要跟着改？

统一响应以后，响应结构变了。

原来测试这样断言：

```java
jsonPath("$.title").value("学习 Spring Boot API")
```

现在要改成：

```java
jsonPath("$.data.title").value("学习 Spring Boot API")
```

原来读取 id：

```java
json.get("id").asLong()
```

现在读取：

```java
json.get("data").get("id").asLong()
```

这说明一件很重要的事：

```text
测试也是接口契约的一部分。
```

当接口格式变化，测试必须同步变化。

如果测试不改，说明它仍然在保护旧契约。

## 第十五步：本周新增和调整的测试

本周测试覆盖了：

```text
注册成功返回 success=true / code=CREATED
登录成功返回 success=true / code=OK
重复用户名返回 DUPLICATE_USERNAME
错误密码返回 INVALID_CREDENTIALS
Todo 不存在返回 TODO_NOT_FOUND
未登录返回 UNAUTHORIZED
参数校验失败返回 VALIDATION_FAILED
删除成功返回统一响应
恢复成功返回统一响应
截止日期不能早于今天
更新标题不能是空白字符串
```

最终测试结果：

```text
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
```

## 前端视角：现在怎么接接口？

以后前端可以写一个统一请求处理器。

伪代码：

```ts
type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T | null;
  path: string | null;
};
```

请求成功后：

```ts
const result = response.data as ApiResponse<Todo>;

if (!result.success) {
  throw new Error(result.message);
}

return result.data;
```

遇到业务错误：

```ts
if (error.response?.data?.code === "UNAUTHORIZED") {
  redirectToLogin();
}
```

这就是统一响应结构给前端带来的价值。

## 本周你需要真正掌握的点

### 1. HTTP 状态码和业务错误码不是一回事

HTTP 状态码回答：

```text
这类请求整体是什么结果？
```

业务错误码回答：

```text
具体发生了什么业务问题？
```

例如：

```text
HTTP 401 + INVALID_CREDENTIALS = 登录失败
HTTP 401 + UNAUTHORIZED = 未登录访问受保护接口
```

它们都是 401，但业务含义不同。

### 2. Controller 应该少写重复包装逻辑

不要每个接口都手动拼 JSON。

用：

```java
ApiResponse.success(data)
```

统一处理成功响应。

### 3. 异常应该集中处理

不要在每个 Controller 里写：

```java
try {
    ...
} catch (...) {
    ...
}
```

统一使用：

```java
@RestControllerAdvice
@ExceptionHandler
```

### 4. 参数校验应该靠近请求入口

请求 DTO 是很适合放校验规则的地方。

例如：

```java
@NotBlank
@Size
@FutureOrPresent
```

这样 Service 层可以更专注业务逻辑。

## 本周复盘问题和参考答案

### 1. 为什么要统一 API 响应结构？

为了让前端处理接口更稳定。无论是注册、登录、Todo 列表还是错误响应，都可以按同一套字段读取：`success`、`code`、`message`、`data`。

### 2. `ApiResponse<T>` 中的 `<T>` 是什么？

`T` 是泛型，表示 `data` 可以是任意业务类型，例如 `TodoResponse`、`LoginResponse`、`PageResponse<TodoResponse>`。

### 3. 为什么错误响应里要有 `code`？

HTTP 状态码只能表达大类，业务 `code` 可以表达更具体的问题，例如 `TODO_NOT_FOUND`、`INVALID_CREDENTIALS`、`VALIDATION_FAILED`。

### 4. `@RestControllerAdvice` 的作用是什么？

它是所有 Controller 的统一异常处理入口。Controller 或 Service 抛出的异常可以在这里集中转换成统一 JSON 响应。

### 5. `BusinessException` 解决了什么问题？

它让业务异常携带 `ErrorCode`。这样异常处理器不仅知道错误消息，还知道应该返回什么业务 code 和 HTTP 状态码。

### 6. 为什么未登录错误不能只靠 `GlobalExceptionHandler`？

因为未登录请求通常会被 Spring Security 在进入 Controller 之前拦截，请求不会走到 Controller，也不会进入普通的全局异常处理流程。所以需要在 `SecurityConfig` 中单独统一响应格式。

### 7. 为什么删除接口从 204 改成 200？

204 表示没有响应体。如果希望删除接口也返回统一结构，就应该返回 200 + JSON。本项目第十二周选择统一接口体验，所以改为 200。

### 8. `@NotBlank` 和 `@Size` 有什么区别？

`@NotBlank` 关注是否为空或全是空格；`@Size` 关注长度范围。它们经常组合使用。

### 9. 为什么 UpdateTodoRequest 的 title 没有用 `@NotBlank`？

因为更新接口里 title 是可选字段。用户可以只更新 completed，不传 title。如果用 `@NotBlank`，不传 title 也会失败。

### 10. `@FutureOrPresent` 的作用是什么？

它要求日期必须是今天或未来日期。本项目用它来避免创建或更新一个截止日期已经过去的 Todo。

### 11. 为什么测试里的 JSON 路径要从 `$.title` 改成 `$.data.title`？

因为业务数据现在被统一包装到了 `data` 字段中。测试必须跟随接口契约变化，否则测试还在保护旧接口格式。

### 12. 前端如何判断登录失败和未登录？

它们 HTTP 状态码都可能是 401，但业务 code 不同：

```text
INVALID_CREDENTIALS：用户名或密码错误
UNAUTHORIZED：未登录或没有携带有效 token
```

### 13. 为什么不直接返回 Spring Boot 默认错误格式？

默认错误格式能用，但不一定适合业务系统。统一响应结构可以让前端、测试、接口文档和后端异常处理都围绕同一套契约工作。

### 14. `ApiResponse<Void>` 适合什么场景？

适合没有具体业务数据、但仍然想返回统一结构的接口，例如删除成功：

```json
{
  "success": true,
  "code": "OK",
  "message": "删除成功",
  "data": null
}
```

### 15. 本周最重要的工程化思想是什么？

把重复规则集中管理。

统一响应、统一错误码、统一异常处理，本质都是为了减少散落在各处的重复判断和重复 JSON 结构。
