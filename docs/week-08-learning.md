# 第八周学习说明：用户注册、登录认证、JWT 和 Todo 数据隔离

第八周的目标是把 Todo API 从“单人练习项目”升级成“多用户后端项目”。

第七周结束时，所有人访问的是同一批 Todo：

```text
GET /api/todos
  -> 返回数据库里的 Todo
```

第八周升级后，Todo 接口必须先登录：

```text
用户注册
用户登录
后端返回 token
前端请求 Todo 接口时带 token
后端根据 token 知道当前用户是谁
只返回当前用户自己的 Todo
```

你是前端开发，可以这样类比：

```text
登录页
  -> 调用 /api/auth/login
  -> 拿到 token
  -> 存到 localStorage / cookie
  -> Axios 请求拦截器加 Authorization header
  -> 后端根据 token 判断当前用户
```

## 本周最终改动总览

| 改动 | 文件 | 目的 |
|---|---|---|
| 添加 Spring Security | `pom.xml` | 引入认证和密码加密能力 |
| 新增用户 Entity | `model/AppUser.java` | 用 Java 对象表示 users 表 |
| 新增用户 Repository | `repository/UserRepository.java` | 查询用户名、判断用户名是否存在 |
| 新增认证 DTO | `RegisterRequest` / `LoginRequest` / `LoginResponse` / `UserResponse` | 定义注册、登录接口入参和出参 |
| 新增认证 Service | `service/AuthService.java` | 实现注册、登录、密码加密、密码验证 |
| 新增认证 Controller | `controller/AuthController.java` | 提供 `/api/auth/register` 和 `/api/auth/login` |
| 新增 JWT 服务 | `security/JwtService.java` | 生成和校验 token |
| 新增 JWT 过滤器 | `security/JwtAuthenticationFilter.java` | 从请求头解析 token，设置当前登录用户 |
| 新增 Security 配置 | `security/SecurityConfig.java` | 配置哪些接口放行，哪些接口需要登录 |
| Todo 关联用户 | `model/Todo.java` | 一个 Todo 属于一个用户 |
| Repository 查询加 userId | `TodoRepository.java` | 所有 Todo 查询都限制在当前用户下 |
| Service 方法加 userId | `TodoService.java` | 业务逻辑按当前用户隔离数据 |
| Controller 注入当前用户 | `TodoController.java` | 通过 `@AuthenticationPrincipal` 获取登录用户 |
| 新增 Flyway V3 | `V3__create_users_and_link_todos.sql` | 创建 users 表，并给 todos 表增加 user_id |
| 扩展接口测试 | `TodoApiApplicationTests.java` | 覆盖注册、登录、401、用户数据隔离 |

## 第一步：为什么要引入 Spring Security？

第七周之前，Todo 接口是匿名接口。

任何请求都可以调用：

```http
GET /api/todos
POST /api/todos
DELETE /api/todos/1
```

这在真实项目里是不安全的。

第八周引入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

它带来几类能力：

- 密码加密，例如 `BCryptPasswordEncoder`
- 请求拦截，例如 Todo 接口必须登录
- 当前用户上下文，例如 `SecurityContextHolder`
- 认证过滤器链，也就是请求进入 Controller 之前先经过 Security 检查

简单理解：

```text
Spring Security 是后端项目的门卫系统。
```

请求进入 Controller 之前，会先经过它。

## 第二步：新增 AppUser，为什么不叫 User？

文件：

```text
src/main/java/com/zading/todoapi/model/AppUser.java
```

核心代码：

```java
@Entity
@Table(name = "users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
}
```

为什么类名叫 `AppUser`，不是 `User`？

因为 Java 和 Spring Security 里已经有很多 `User` 相关类型。如果也叫 `User`，读代码时容易混淆：

```text
java.lang.User?        没有这个
Spring Security User?  有
我们自己的 User?      也有
```

所以项目里使用 `AppUser`，意思是：

```text
这是本应用自己的用户模型。
```

字段解释：

```java
private Long id;
```

数据库主键。

```java
private String username;
```

用户名，设置了唯一约束。

```java
private String passwordHash;
```

加密后的密码，不保存原始密码。

```java
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
```

创建时间和更新时间。

## 第三步：为什么密码字段叫 passwordHash？

注册时，用户提交的是：

```json
{
  "username": "zading",
  "password": "123456"
}
```

但数据库绝对不能保存：

```text
123456
```

原因很简单：如果数据库泄露，所有用户密码都会直接暴露。

所以保存前会调用：

```java
passwordEncoder.encode(password)
```

生成类似这样的字符串：

```text
$2a$10$...
```

这就是 BCrypt hash。

登录时也不是把 hash 解密，而是：

```java
passwordEncoder.matches(password, user.getPasswordHash())
```

意思是：

```text
用同一种算法验证“用户输入的密码”和“数据库里的 hash”是否匹配。
```

这点很重要：

```text
密码 hash 通常不能解密，只能校验。
```

## 第四步：UserRepository 为什么不用实现？

文件：

```text
src/main/java/com/zading/todoapi/repository/UserRepository.java
```

代码：

```java
public interface UserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByUsername(String username);

    Optional<AppUser> findByUsername(String username);
}
```

这两个方法没有实现，是因为 Spring Data JPA 会根据方法名自动生成 SQL。

```java
existsByUsername
```

大概等价于：

```sql
SELECT EXISTS (
  SELECT 1 FROM users WHERE username = ?
);
```

```java
findByUsername
```

大概等价于：

```sql
SELECT * FROM users WHERE username = ?;
```

这个机制叫：

```text
Query Method
```

也就是“根据方法名推导查询”。

## 第五步：注册接口怎么读？

接口：

```http
POST /api/auth/register
```

请求体：

```json
{
  "username": "zading",
  "password": "123456"
}
```

入口在：

```text
src/main/java/com/zading/todoapi/controller/AuthController.java
```

代码：

```java
@PostMapping("/register")
@ResponseStatus(HttpStatus.CREATED)
public UserResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request.getUsername(), request.getPassword());
}
```

逐句解释：

```java
@PostMapping("/register")
```

表示这个方法处理：

```http
POST /api/auth/register
```

因为类上已经有：

```java
@RequestMapping("/api/auth")
```

所以完整路径是 `/api/auth/register`。

```java
@ResponseStatus(HttpStatus.CREATED)
```

表示注册成功返回：

```text
201 Created
```

```java
@Valid @RequestBody RegisterRequest request
```

表示：

```text
把 JSON 请求体转换成 RegisterRequest
并触发 RegisterRequest 里的参数校验
```

`RegisterRequest` 里有：

```java
@NotBlank(message = "用户名不能为空")
@Size(min = 3, max = 50, message = "用户名长度必须在 3 到 50 个字符之间")
private String username;
```

所以空用户名或者太短的用户名会直接返回 400。

## 第六步：AuthService.register 做了什么？

文件：

```text
src/main/java/com/zading/todoapi/service/AuthService.java
```

核心代码：

```java
public UserResponse register(String username, String password) {
    String normalizedUsername = normalizeUsername(username);

    if (userRepository.existsByUsername(normalizedUsername)) {
        throw new IllegalArgumentException("用户名已存在");
    }

    AppUser user = new AppUser(normalizedUsername, passwordEncoder.encode(password));
    AppUser savedUser = userRepository.save(user);

    return toResponse(savedUser);
}
```

它做了四件事：

```text
1. 清理用户名空格
2. 检查用户名是否已存在
3. 加密密码并保存用户
4. 返回 UserResponse
```

为什么返回 `UserResponse`，而不是返回 `AppUser`？

因为 `AppUser` 里有：

```java
private String passwordHash;
```

这个字段不能返回给前端。

所以响应 DTO 只返回：

```java
id
username
createdAt
```

这是第六周 DTO 思想的延续：

```text
Entity 是数据库模型，Response DTO 是接口模型。
```

## 第七步：登录接口怎么读？

接口：

```http
POST /api/auth/login
```

请求体：

```json
{
  "username": "zading",
  "password": "123456"
}
```

Controller：

```java
@PostMapping("/login")
public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.getUsername(), request.getPassword());
}
```

Service：

```java
public LoginResponse login(String username, String password) {
    String normalizedUsername = normalizeUsername(username);
    AppUser user = userRepository.findByUsername(normalizedUsername)
            .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
        throw new UnauthorizedException("用户名或密码错误");
    }

    return new LoginResponse(jwtService.generateToken(user.getUsername()));
}
```

逻辑是：

```text
1. 根据 username 查用户
2. 用户不存在 -> 401
3. 用户存在但密码不匹配 -> 401
4. 密码正确 -> 生成 token
5. 返回 token 给前端
```

为什么用户名不存在和密码错误都返回：

```text
用户名或密码错误
```

而不是明确说“用户名不存在”？

因为如果明确返回“用户名不存在”，攻击者可以批量试探哪些用户名存在。

统一错误信息更安全。

## 第八步：JWT 是什么？

JWT 可以先理解为：

```text
后端签发给前端的一张临时通行证。
```

本项目返回：

```json
{
  "token": "xxxxx.yyyyy.zzzzz",
  "tokenType": "Bearer"
}
```

JWT 通常由三段组成：

```text
header.payload.signature
```

例如：

```text
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ6YWRpbmcifQ.signature
```

三段含义：

```text
header     说明签名算法，比如 HS256
payload    存业务信息，比如用户名、签发时间、过期时间
signature  用密钥签名，防止 token 被篡改
```

注意：

```text
JWT 的 header 和 payload 只是 Base64Url 编码，不是加密。
不要在 payload 里放密码、手机号、身份证号等敏感信息。
```

本项目 payload 只放：

```java
sub  用户名
iat  签发时间
exp  过期时间
```

## 第九步：JwtService 怎么生成 token？

文件：

```text
src/main/java/com/zading/todoapi/security/JwtService.java
```

核心方法：

```java
public String generateToken(String username) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(expirationMinutes * 60);

    Map<String, Object> header = new LinkedHashMap<>();
    header.put("alg", "HS256");
    header.put("typ", "JWT");

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sub", username);
    payload.put("iat", now.getEpochSecond());
    payload.put("exp", expiresAt.getEpochSecond());

    String unsignedToken = base64UrlEncode(toJson(header)) + "." + base64UrlEncode(toJson(payload));
    return unsignedToken + "." + sign(unsignedToken);
}
```

逐步看：

```java
Instant now = Instant.now();
```

当前时间。

```java
Instant expiresAt = now.plusSeconds(expirationMinutes * 60);
```

计算过期时间。

```java
header.put("alg", "HS256");
```

表示使用 HMAC-SHA256 签名。

```java
payload.put("sub", username);
```

`sub` 是 subject，表示这个 token 属于谁。

```java
String unsignedToken = base64UrlEncode(headerJson) + "." + base64UrlEncode(payloadJson);
```

生成前两段。

```java
return unsignedToken + "." + sign(unsignedToken);
```

用密钥给前两段签名，得到第三段。

## 第十步：JwtAuthenticationFilter 是做什么的？

文件：

```text
src/main/java/com/zading/todoapi/security/JwtAuthenticationFilter.java
```

它不是 Controller，而是过滤器。

请求链路变成：

```text
HTTP 请求
  -> JwtAuthenticationFilter
  -> Spring Security
  -> Controller
```

核心代码：

```java
String authHeader = request.getHeader("Authorization");

if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);
    return;
}
```

意思是：

```text
读取 Authorization 请求头。
如果没有 token，就继续往后走。
后面 Spring Security 会判断这个接口需不需要登录。
```

为什么没有 token 不直接报错？

因为有些接口本来就允许匿名访问：

```text
/hello
/api/auth/register
/api/auth/login
```

所以过滤器只负责“如果有 token，就尝试识别用户”。

接下来：

```java
String token = authHeader.substring(7);
```

去掉：

```text
Bearer 
```

留下真正的 token。

为什么是 7？

```text
B e a r e r 空格
1 2 3 4 5 6 7
```

再往下：

```java
String username = jwtService.extractUsername(token);
```

从 token 里解析用户名。

然后：

```java
userRepository.findByUsername(username).ifPresent(user -> {
    if (jwtService.isTokenValid(token, user.getUsername())) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getUsername());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
});
```

这段是第八周最抽象的一段。

你可以这样理解：

```text
token 合法
  -> 找到用户
  -> 创建一个认证对象 authentication
  -> 放进 SecurityContextHolder
```

放进去以后，Controller 就可以通过：

```java
@AuthenticationPrincipal AuthenticatedUser currentUser
```

拿到当前登录用户。

## 第十一步：SecurityConfig 怎么决定接口权限？

文件：

```text
src/main/java/com/zading/todoapi/security/SecurityConfig.java
```

核心代码：

```java
return http
        .csrf(csrf -> csrf.disable())
        .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/hello", "/api/auth/register", "/api/auth/login", "/h2-console/**").permitAll()
                .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
```

逐句解释：

```java
csrf(csrf -> csrf.disable())
```

关闭 CSRF。

前后端分离 API 通常使用 token，不使用传统表单 Session，所以这里关闭。

```java
frameOptions(frameOptions -> frameOptions.sameOrigin())
```

允许 H2 Console 页面正常显示。H2 Console 使用 iframe，默认安全策略可能会拦。

```java
SessionCreationPolicy.STATELESS
```

表示后端不保存 Session。

也就是说：

```text
每次请求都要靠 Authorization header 里的 token 判断身份。
```

```java
requestMatchers(...).permitAll()
```

这些接口允许匿名访问：

```text
/hello
/api/auth/register
/api/auth/login
/h2-console/**
```

```java
anyRequest().authenticated()
```

其他所有接口都必须登录。

```java
addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

把我们的 JWT 过滤器加入 Spring Security 过滤器链。

## 第十二步：Todo 怎么和用户关联？

文件：

```text
src/main/java/com/zading/todoapi/model/Todo.java
```

新增：

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private AppUser user;
```

意思是：

```text
多个 Todo 属于一个 User。
```

关系图：

```text
AppUser 1
  -> Todo A
  -> Todo B

AppUser 2
  -> Todo C
```

`@ManyToOne` 表示：

```text
Many Todo -> One User
```

`@JoinColumn(name = "user_id")` 表示：

```text
todos 表里用 user_id 字段关联 users 表。
```

## 第十三步：为什么 Repository 方法都加了 UserId？

文件：

```text
src/main/java/com/zading/todoapi/repository/TodoRepository.java
```

第七周之前：

```java
Page<Todo> findByCompleted(boolean completed, Pageable pageable);
```

第八周之后：

```java
Page<Todo> findByUserIdAndCompleted(Long userId, boolean completed, Pageable pageable);
```

变化是多了：

```java
UserId
```

为什么？

因为 Todo 数据必须隔离。

以前查询：

```sql
WHERE completed = ?
```

现在查询：

```sql
WHERE user_id = ?
AND completed = ?
```

这就是“用户 A 看不到用户 B 的 Todo”的根本原因。

其他方法也是同理：

```java
findByUserId
findByUserIdAndTitleContainingIgnoreCase
findByUserIdAndCompletedAndTitleContainingIgnoreCase
findByIdAndUserId
existsByIdAndUserId
```

注意这个：

```java
findByIdAndUserId(Long id, Long userId)
```

它不只是查 Todo id，还要求这个 Todo 属于当前用户。

所以用户 B 访问用户 A 的 Todo id，会返回不存在。

这比返回 403 更简单，也能避免泄露“这个 id 确实存在，只是不属于你”。

## 第十四步：TodoService 如何隔离数据？

文件：

```text
src/main/java/com/zading/todoapi/service/TodoService.java
```

第七周：

```java
public Page<Todo> getTodos(Boolean completed, String keyword, Pageable pageable)
```

第八周：

```java
public Page<Todo> getTodos(Long userId, Boolean completed, String keyword, Pageable pageable)
```

多了：

```java
Long userId
```

查询逻辑：

```java
if (completed != null && normalizedKeyword != null) {
    return todoRepository.findByUserIdAndCompletedAndTitleContainingIgnoreCase(userId, completed, normalizedKeyword, pageable);
}

if (completed != null) {
    return todoRepository.findByUserIdAndCompleted(userId, completed, pageable);
}

if (normalizedKeyword != null) {
    return todoRepository.findByUserIdAndTitleContainingIgnoreCase(userId, normalizedKeyword, pageable);
}

return todoRepository.findByUserId(userId, pageable);
```

可以翻译成 JS：

```js
if (completed 有值 && keyword 有值) {
  查询 当前用户 + completed + keyword
} else if (completed 有值) {
  查询 当前用户 + completed
} else if (keyword 有值) {
  查询 当前用户 + keyword
} else {
  查询 当前用户的全部 Todo
}
```

这个 `userId` 是业务边界。

只要 Service 层所有 Todo 操作都要求传 `userId`，就不容易写出“查了所有人的 Todo”的代码。

## 第十五步：创建 Todo 时为什么要 setUser？

代码：

```java
public Todo addTodo(Long userId, String title, TodoPriority priority, LocalDate dueDate) {
    AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
    String normalizedTitle = normalizeTitle(title);
    Todo todo = new Todo(null, normalizedTitle, false);
    todo.setUser(user);
    todo.setPriority(normalizePriority(priority));
    todo.setDueDate(dueDate);
    return todoRepository.save(todo);
}
```

关键是：

```java
todo.setUser(user);
```

如果不设置 user，数据库里的 `user_id` 就不知道填谁。

因为 V3 migration 里设置了：

```sql
ALTER TABLE todos
    ALTER COLUMN user_id SET NOT NULL;
```

所以每个 Todo 都必须有所属用户。

## 第十六步：Controller 如何拿到当前用户？

文件：

```text
src/main/java/com/zading/todoapi/controller/TodoController.java
```

例如列表接口：

```java
public PageResponse<TodoResponse> getTodos(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @RequestParam(required = false) Boolean completed,
        ...
)
```

重点是：

```java
@AuthenticationPrincipal AuthenticatedUser currentUser
```

这个 `currentUser` 不是前端传的。

它来自：

```text
JWT 过滤器解析 token
  -> 创建 AuthenticatedUser
  -> 放进 SecurityContextHolder
  -> Controller 通过 @AuthenticationPrincipal 拿到
```

这点非常重要：

```text
后端不要相信前端传 userId。
```

不要让前端这样创建 Todo：

```json
{
  "title": "学习认证",
  "userId": 1
}
```

因为用户可以伪造 `userId`。

正确做法是：

```text
前端只带 token
后端自己从 token 判断当前用户是谁
```

## 第十七步：V3 migration 做了什么？

文件：

```text
src/main/resources/db/migration/V3__create_users_and_link_todos.sql
```

它做了几件事：

```text
1. 创建 users 表
2. 创建一个 legacy_user
3. 给 todos 表增加 user_id
4. 把旧 Todo 绑定到 legacy_user
5. 把 user_id 改成 NOT NULL
6. 增加外键约束
```

为什么要创建 `legacy_user`？

因为你本地 H2 文件数据库里可能已经有旧 Todo。

如果直接给 `todos` 表加一个非空字段：

```sql
user_id BIGINT NOT NULL
```

旧数据没有 user_id，migration 会失败。

所以我们先：

```text
创建 legacy_user
把旧 Todo 的 user_id 填成 legacy_user 的 id
再把 user_id 改成 NOT NULL
```

这是数据库迁移里很常见的兼容旧数据思路。

## 第十八步：测试为什么要大改？

第八周后，Todo 接口都需要登录。

所以以前测试：

```java
mockMvc.perform(post("/api/todos"))
```

现在必须变成：

```java
mockMvc.perform(post("/api/todos")
        .header("Authorization", bearer(token)))
```

测试里新增了辅助方法：

```java
private String registerAndLogin(String username, String password) throws Exception
```

它做的事：

```text
1. 注册用户
2. 登录用户
3. 从响应 JSON 里读取 token
4. 返回 token
```

这样每个 Todo 测试都能先拿 token，再访问受保护接口。

## 第十九步：本周新增了哪些测试？

新增或改造测试覆盖：

- 注册用户成功
- 登录用户成功
- 重复用户名注册失败
- 错误密码登录失败
- 未登录访问 Todo 返回 401
- 登录后可以创建 Todo
- 分页、排序、筛选仍然可用
- 用户 A 只能看到用户 A 的 Todo
- 用户 B 不能通过 id 访问用户 A 的 Todo

最关键的是这个场景：

```text
用户 A 创建 Todo A
用户 B 创建 Todo B
用户 A 查询列表，只能看到 Todo A
用户 B 查询列表，只能看到 Todo B
用户 B 访问 Todo A 的 id，返回 404
```

这证明数据隔离真的生效。

## 当前 API 使用示例

注册：

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"zading","password":"123456"}'
```

登录：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zading","password":"123456"}'
```

返回：

```json
{
  "token": "xxxxx.yyyyy.zzzzz",
  "tokenType": "Bearer"
}
```

创建 Todo：

```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer xxxxx.yyyyy.zzzzz" \
  -d '{"title":"学习 JWT","priority":"HIGH","dueDate":"2026-07-20"}'
```

查询 Todo：

```bash
curl "http://localhost:8080/api/todos?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer xxxxx.yyyyy.zzzzz"
```

## 本周复盘问题

1. 为什么数据库里不能保存用户原始密码？
2. BCrypt 的 `encode` 和 `matches` 分别做什么？
3. JWT 的三段分别是什么？
4. 为什么 JWT payload 里不能放敏感信息？
5. 为什么 Todo 请求不能让前端传 `userId`？
6. `JwtAuthenticationFilter` 为什么不直接处理业务逻辑？
7. `@AuthenticationPrincipal` 的作用是什么？
8. 为什么 TodoRepository 方法要加 `UserId`？
9. 用户 B 访问用户 A 的 Todo，为什么项目返回 404？
10. 为什么 V3 migration 要先创建 `legacy_user`？

## 参考答案

### 1. 为什么数据库里不能保存用户原始密码？

因为数据库一旦泄露，原始密码会直接暴露。

很多用户会在多个网站使用相同密码，泄露一个系统可能影响用户其他账户。

所以数据库里应该保存密码 hash，而不是原始密码。

### 2. BCrypt 的 `encode` 和 `matches` 分别做什么？

`encode` 用于注册时把原始密码变成 hash。

`matches` 用于登录时校验：

```text
用户输入的密码 是否匹配 数据库里的 passwordHash
```

它不是解密，而是校验。

### 3. JWT 的三段分别是什么？

JWT 通常是：

```text
header.payload.signature
```

- `header`：说明算法和 token 类型
- `payload`：存放业务声明，例如用户名、签发时间、过期时间
- `signature`：签名，防止 token 被篡改

### 4. 为什么 JWT payload 里不能放敏感信息？

因为 JWT 的 header 和 payload 是 Base64Url 编码，不是加密。

别人拿到 token 后，可以解码看到 payload 内容。

所以不要放密码、身份证号、手机号等敏感信息。

### 5. 为什么 Todo 请求不能让前端传 `userId`？

因为前端传来的数据可以被用户篡改。

如果允许前端传：

```json
{
  "userId": 2
}
```

用户可能伪造别人的 userId。

正确做法是：

```text
前端带 token
后端从 token 解析当前用户
```

### 6. `JwtAuthenticationFilter` 为什么不直接处理业务逻辑？

过滤器只负责认证：

```text
识别请求是谁发的
```

业务逻辑仍然放在 Controller / Service。

这样职责清晰：

```text
Filter      负责认证
Controller 负责 HTTP 参数和响应
Service    负责业务逻辑
Repository 负责数据库访问
```

### 7. `@AuthenticationPrincipal` 的作用是什么？

它可以从 Spring Security 的认证上下文里取出当前登录用户。

本项目中 Controller 使用：

```java
@AuthenticationPrincipal AuthenticatedUser currentUser
```

拿到当前用户 id 和 username。

### 8. 为什么 TodoRepository 方法要加 `UserId`？

为了数据隔离。

如果只按 Todo id 查询：

```java
findById(id)
```

用户可能访问到别人的 Todo。

加上 userId：

```java
findByIdAndUserId(id, userId)
```

数据库查询就会限制：

```sql
WHERE id = ?
AND user_id = ?
```

### 9. 用户 B 访问用户 A 的 Todo，为什么项目返回 404？

因为查询使用：

```java
findByIdAndUserId(id, currentUserId)
```

如果这个 Todo 不属于当前用户，就查不到。

项目把它当作“不存在”处理。

这样可以避免泄露资源是否存在。

### 10. 为什么 V3 migration 要先创建 `legacy_user`？

因为本地数据库可能已经有旧 Todo。

新增 `user_id NOT NULL` 时，旧 Todo 没有 user_id，会导致迁移失败。

所以先创建一个 `legacy_user`，把旧 Todo 归到这个用户下面，再把 `user_id` 改成非空。

这是兼容已有数据的一种迁移策略。

## 更多复盘问题

1. `SessionCreationPolicy.STATELESS` 表示什么？
2. 为什么登录失败应该返回 401？
3. 为什么重复用户名注册返回 400？
4. `permitAll()` 和 `authenticated()` 有什么区别？
5. `Authorization: Bearer token` 里的 `Bearer` 是什么？
6. `@ManyToOne(fetch = FetchType.LAZY)` 中 `LAZY` 是什么意思？
7. 为什么 `AuthService.register` 返回 `UserResponse`，不是 `AppUser`？
8. 为什么测试里要先注册和登录，再请求 Todo？
9. 为什么 token 应该设置过期时间？
10. 如果用户修改了用户名，旧 token 会发生什么问题？

## 更多复盘问题参考答案

### 1. `SessionCreationPolicy.STATELESS` 表示什么？

表示服务端不保存登录 Session。

每次请求都必须自己携带 token。

这很适合前后端分离 API。

### 2. 为什么登录失败应该返回 401？

401 表示：

```text
Unauthorized
```

也就是认证失败。

用户名或密码错误，本质上是“你没有通过身份认证”。

### 3. 为什么重复用户名注册返回 400？

因为这是客户端提交的数据不符合业务规则。

用户名必须唯一，重复用户名属于请求无效，所以返回 400。

### 4. `permitAll()` 和 `authenticated()` 有什么区别？

`permitAll()` 表示不登录也能访问。

`authenticated()` 表示必须登录才能访问。

### 5. `Authorization: Bearer token` 里的 `Bearer` 是什么？

`Bearer` 是一种认证方案，意思是：

```text
持有这个 token 的请求，就代表某个身份。
```

所以 token 要保护好，泄露后别人也可以拿它访问接口。

### 6. `@ManyToOne(fetch = FetchType.LAZY)` 中 `LAZY` 是什么意思？

`LAZY` 表示懒加载。

查询 Todo 时，不会立刻把 User 全部查出来。

只有真正访问 `todo.getUser()` 时，JPA 才会尝试加载用户。

### 7. 为什么 `AuthService.register` 返回 `UserResponse`，不是 `AppUser`？

因为 `AppUser` 里有 `passwordHash`。

这个字段不应该返回给前端。

所以用 `UserResponse` 控制接口输出字段。

### 8. 为什么测试里要先注册和登录，再请求 Todo？

因为 Todo 接口已经受保护。

没有 token 会返回 401。

测试要模拟真实用户行为：

```text
注册 -> 登录 -> 拿 token -> 请求 Todo
```

### 9. 为什么 token 应该设置过期时间？

如果 token 永不过期，一旦泄露，攻击者可以长期使用。

设置过期时间可以降低风险。

### 10. 如果用户修改了用户名，旧 token 会发生什么问题？

当前项目的 token 里用 username 作为 `sub`。

如果用户名改了，旧 token 里的 username 就可能找不到用户。

真实项目里更常见的是把用户 id 放进 token，例如：

```text
sub = userId
```

这个可以作为后续优化点。
