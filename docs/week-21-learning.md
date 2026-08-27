# 第 21 周：RBAC 角色权限控制与管理端接口

## 一、本周目标

前面的认证功能已经解决了“你是谁”，本周继续解决“你能做什么”。

本周实现基于角色的访问控制（RBAC，Role-Based Access Control）：

```text
USER
  -> 只能访问自己的 Todo、日志和附件

ADMIN
  -> 可以访问管理员接口，查看全局用户、Todo 和统计数据
```

本周完成后，项目具备三个重要的安全结果：

1. 未登录访问受保护接口返回 `401 Unauthorized`。
2. 已登录但角色不满足要求返回 `403 Forbidden`。
3. 新注册用户只能获得 `USER` 角色，不能通过公开注册接口成为管理员。

## 二、RBAC 的核心概念

RBAC 可以拆成三层：

```text
用户 User
  -> 拥有角色 Role
  -> 角色拥有权限 Permission
```

当前项目先实现最容易理解的两级角色：

| 角色 | 能力 |
|---|---|
| `USER` | 访问自己的 Todo、操作日志和附件 |
| `ADMIN` | 访问 `/api/admin/**` 管理接口 |

当前没有把每一个接口都建成独立 Permission，是因为项目规模还比较小。等角色数量和业务模块增加后，可以继续拆成 `TODO_READ`、`USER_MANAGE` 等细粒度权限。

## 三、第一步：数据库增加 role 字段

新增迁移脚本：

```text
src/main/resources/db/migration/V7__add_user_role.sql
```

内容：

```sql
ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
```

### 为什么使用 DEFAULT

项目原来已经存在用户数据。给已有表新增一个 `NOT NULL` 字段时，旧数据也必须有值。

```sql
DEFAULT 'USER'
```

可以保证：

- 旧用户自动成为普通用户
- 新用户在没有特别指定时也是普通用户
- 数据库不会产生 `role = NULL`

这体现了数据库迁移的一个原则：新增必填字段时，需要同时考虑历史数据。

## 四、第二步：使用枚举表达角色

文件：

```text
src/main/java/com/zading/todoapi/model/UserRole.java
```

```java
public enum UserRole {
    USER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
```

这里有两个名称：

```text
数据库角色：USER
Spring Security 权限：ROLE_USER
```

Spring Security 对 `hasRole("ADMIN")` 有一个约定：它实际检查的是 `ROLE_ADMIN`。所以实体层保存简单的 `ADMIN`，适配 Security 时再转换成 `ROLE_ADMIN`。

用户实体中的字段：

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private UserRole role = UserRole.USER;
```

如果使用枚举序号保存角色，未来调整枚举顺序可能改变数字含义。使用字符串后，数据库中直接保存 `USER` 或 `ADMIN`，可读性和稳定性更好。

## 五、第三步：注册用户默认是 USER

`AppUser` 的 `role` 字段初始化为：

```java
private UserRole role = UserRole.USER;
```

注册请求没有接收 `role` 参数。这样可以避免任何人提交：

```json
{
  "username": "new-user",
  "password": "123456",
  "role": "ADMIN"
}
```

从而直接绕过管理员创建流程。

## 六、第四步：JWT 增加角色声明

登录成功时，JWT payload 增加：

```json
{
  "sub": "zading",
  "role": "USER",
  "iat": 1720000000,
  "exp": 1720007200
}
```

认证过滤器验证三个条件：

```text
用户名一致
Token 中的角色和数据库当前角色一致
Token 没有过期且签名正确
```

角色和数据库进行比较的意义是：管理员被降级后，旧的 ADMIN Token 不能继续使用管理员接口。用户需要重新登录得到新的 Token。

## 七、第五步：创建 Spring Security 权限

认证成功后，过滤器创建：

```java
new SimpleGrantedAuthority(user.getRole().authority())
```

如果用户角色是 `ADMIN`，最终权限是 `ROLE_ADMIN`；如果角色是 `USER`，最终权限是 `ROLE_USER`。

之后 Spring Security 才能执行：

```java
hasRole("ADMIN")
```

需要区分：

```text
Authentication
  -> 认证结果，说明当前是谁

GrantedAuthority
  -> 权限集合，说明当前用户能做什么
```

## 八、第六步：处理 401 和 403

### 401：没有登录

请求没有有效 JWT 时进入 `authenticationEntryPoint`，返回 401。

### 403：登录了但权限不足

普通用户请求管理员接口时进入 `accessDeniedHandler`，返回 403：

```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "没有权限访问该资源",
  "data": null,
  "path": "/api/admin/statistics"
}
```

权限判断发生在 Servlet Filter 链中，可能还没有进入 Controller，因此 MVC 的 `@RestControllerAdvice` 接不到这类异常，必须在 `SecurityConfig` 中配置 `AccessDeniedHandler`。

## 九、第七步：管理员接口

新增：

```text
src/main/java/com/zading/todoapi/controller/AdminController.java
src/main/java/com/zading/todoapi/service/AdminService.java
```

### 查询用户

```http
GET /api/admin/users?page=0&size=10&sort=id,asc
```

返回用户 ID、用户名、角色、创建时间和更新时间，不返回密码哈希。

### 查询全局 Todo

```http
GET /api/admin/todos?page=0&size=10&sort=id,asc
```

管理员响应会额外返回：

```json
{
  "userId": 1,
  "username": "normal-user"
}
```

默认不展示软删除数据，需要查看历史记录时显式传：

```http
GET /api/admin/todos?includeDeleted=true
```

### 查询统计

```http
GET /api/admin/statistics
```

统计字段包括用户总数、Todo 总数、未删除数量、完成数量、未完成数量和软删除数量。

## 十、权限控制的两道门

项目同时配置了 URL 权限和方法权限：

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

以及：

```java
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
}
```

URL 规则便于统一保护一组接口；方法注解可以保护具体 Controller 方法。两者形成防御式配置。

## 十一、如何在本地创建管理员

出于安全考虑，公开注册接口不会创建管理员。学习环境可以先注册用户，再通过数据库提升指定用户：

```sql
UPDATE users
SET role = 'ADMIN'
WHERE username = 'zading';
```

修改角色后需要重新登录，因为 Token 中保存了角色声明，并且过滤器会校验 Token 角色与数据库当前角色一致。

生产环境不建议直接暴露数据库修改权限，通常由部署脚本、数据库迁移、后台命令或企业 SSO 完成首个管理员初始化。

## 十二、本周测试

文件：

```text
src/test/java/com/zading/todoapi/RbacApiTests.java
```

覆盖内容：

- 新注册用户默认为 `USER`
- 未登录访问管理员接口返回 `401`
- 普通用户访问管理员接口返回 `403`
- 管理员可以查询用户、Todo 和统计数据
- 管理员可以通过 `includeDeleted=true` 查看软删除 Todo
- 管理员查询 Todo 时能看到所属用户

测试中通过 Repository 把测试用户提升为 ADMIN，这只用于构造测试数据，不会增加线上公开的提权接口。

## 十三、复盘问题与答案

### 1. 401 和 403 有什么区别？

`401` 表示没有完成身份认证，例如没有 Token 或 Token 无效；`403` 表示身份已经确认，但权限不足。

### 2. 为什么不能让注册接口接收 role？

因为任何人都可以传入 `ADMIN`，直接绕过管理员创建流程。角色授予必须由受信任的初始化流程或已有管理员完成。

### 3. 为什么数据库保存 USER，而 Security 使用 ROLE_USER？

`ROLE_` 是 Spring Security 对角色权限的命名约定。数据库保存业务角色，进入安全框架前转换成框架需要的 authority。

### 4. 为什么要把 role 放入 JWT？

JWT 可以携带本次登录的身份和角色信息，认证过滤器能据此建立权限上下文。当前实现还会和数据库角色比较，让角色变更能够让旧 Token 失效。

### 5. 为什么 AccessDeniedHandler 不写在 GlobalExceptionHandler？

权限判断发生在 Servlet Filter 链中，可能还没有进入 Controller，因此 MVC 的 `@RestControllerAdvice` 接不到这类异常。

### 6. 为什么管理员 Todo 需要单独的 DTO？

普通用户 Todo 响应不应该暴露其他用户信息。管理员需要知道 Todo 所属用户，因此使用单独的 `AdminTodoResponse` 控制字段范围。

### 7. 为什么查询管理员 Todo 默认排除软删除数据？

日常管理通常关注当前有效数据；通过 `includeDeleted=true` 显式查看历史数据，可以减少误读，也让接口意图更加清楚。

## 十四、下一步

完成本周后，可以继续学习更细粒度的 Permission 权限模型、管理员修改用户角色、操作审计日志、多租户隔离和 OAuth2 / OIDC / 企业 SSO。

## 代码精读补充

### 1. 角色从数据库到 Spring Security 的路径

```text
users.role = USER / ADMIN
  -> AppUser.getRole()
  -> UserRole.authority()
  -> SimpleGrantedAuthority("ROLE_" + role)
  -> hasRole("ADMIN")
```

数据库保存业务角色，Spring Security 使用带 `ROLE_` 前缀的权限名称。`hasRole("ADMIN")` 内部会匹配 `ROLE_ADMIN`，所以两边的命名转换必须保持一致。

### 2. 为什么注册接口不接收 role

```java
public UserResponse register(String username, String password) {
    AppUser user = new AppUser(username, encodedPassword, UserRole.USER);
    return toResponse(userRepository.save(user));
}
```

角色由服务端固定设置为 `USER`。如果把 `role` 放进公开注册请求，客户端就能直接注册管理员，权限边界会失效。管理员角色应由受保护的后台操作或数据库迁移设置。

### 3. 401 和 403 的执行位置

认证失败发生在没有有效身份时，进入 `authenticationEntryPoint`，返回 401；身份存在但权限不足时，进入 `accessDeniedHandler`，返回 403。这两个判断都可能发生在 Controller 之前。
