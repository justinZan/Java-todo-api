# 第十九周学习说明：生产化配置、启动方式和日志排查

第十九周的目标是：让项目从“能在本地跑”继续往“更像真实后端工程”靠近。

前面几周我们已经做了：

```text
第 16 周：异步事件
第 17 周：定时任务
第 18 周：Actuator 可观测性
```

这周继续补一个真实项目很关键的能力：

```text
不同环境用不同配置
敏感信息不写死
配置用 Java 对象管理
日志能用 requestId 追踪
应用可以打包成 jar 后启动
健康检查区分 liveness 和 readiness
```

一句话：

```text
第十九周学的是：让项目更适合部署、排查和长期维护。
```

## 本周最终改动总览

| 改动 | 文件 | 目的 |
|---|---|---|
| 开启配置类扫描 | `TodoApiApplication.java` | 让 `@ConfigurationProperties` 生效 |
| 新增 JWT 配置类 | `JwtProperties.java` | 把 `app.jwt.*` 绑定成 Java 对象 |
| 新增请求日志配置类 | `RequestLoggingProperties.java` | 管理请求日志开关和 requestId 请求头名称 |
| 新增过期任务配置类 | `TodoOverdueJobProperties.java` | 管理定时任务 pageSize、cron、zone 等配置 |
| 重构 JWT 服务 | `JwtService.java` | 不再直接使用 `@Value` 读取配置 |
| 重构定时任务入口 | `TodoOverdueJob.java` | 使用配置对象读取定时任务配置 |
| 改造请求日志过滤器 | `RequestLoggingFilter.java` | 给每个请求加 `X-Request-Id` |
| 新增 dev 配置 | `application-dev.properties` | 本地开发 profile 示例 |
| 新增 prod 配置 | `application-prod.properties` | 生产环境 profile 示例 |
| 增强 Actuator 配置 | `application.properties` | 开启 liveness / readiness 探针 |
| 放行 Actuator 探针 | `SecurityConfig.java` | 允许访问 `/actuator/health/liveness` 和 `/actuator/health/readiness` |
| 补充测试 | `ActuatorTests.java` / `ApplicationSmokeTests.java` | 验证探针和 requestId 响应头 |
| 更新 README | `README.md` | 补充工程运行说明 |

## 第一天：为什么要有多环境配置？

真实项目通常不会只有一个环境。

常见环境有：

| 环境 | 作用 | 特点 |
|---|---|---|
| dev | 本地开发 | 日志多、方便调试 |
| test | 自动化测试 | 稳定、可重复 |
| prod | 生产环境 | 安全、少暴露、配置外部化 |

如果所有环境都用同一份配置，会出现很多问题。

例如：

```text
本地想看 SQL，但生产不应该打印大量 SQL
本地可以用 H2，但生产应该用 PostgreSQL
本地可以使用学习版 JWT secret，但生产必须用强密钥
测试环境不应该真的每天跑定时任务
```

所以第十九周新增了：

```text
application-dev.properties
application-prod.properties
```

## 第二天：application-dev.properties 做什么？

文件：

```text
src/main/resources/application-dev.properties
```

dev profile 面向本地开发。

它保留了方便调试的配置：

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.h2.console.enabled=true

app.request-logging.enabled=true

logging.level.com.zading.todoapi=DEBUG
logging.level.org.springframework.security=INFO
```

这表示：

```text
打印 SQL
格式化 SQL
允许访问 H2 Console
开启请求日志
业务代码日志级别更详细
```

启动方式：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

你也可以先打包，再用 jar 启动：

```bash
mvn package
java -jar target/java-todo-api-1.0.0.jar --spring.profiles.active=dev
```

## 第三天：application-prod.properties 做什么？

文件：

```text
src/main/resources/application-prod.properties
```

prod profile 面向生产环境。

它更强调安全和稳定：

```properties
spring.h2.console.enabled=false
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-minutes=${JWT_EXPIRATION_MINUTES:120}
```

这表示：

```text
关闭 H2 Console
不打印 SQL
JWT 密钥必须从环境变量读取
JWT 过期时间可以从环境变量读取，没有就默认 120 分钟
```

生产环境还要求数据库连接从环境变量读取：

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

注意这里没有默认值。

这是一种刻意设计：

```text
生产环境缺少关键配置时，应该启动失败，而不是用学习默认值偷偷跑起来。
```

## 第四天：环境变量是什么？

环境变量就是操作系统或部署平台传给应用的配置。

例如：

```bash
JWT_SECRET=your-strong-secret
```

Spring Boot 配置里可以这样读取：

```properties
app.jwt.secret=${JWT_SECRET}
```

也可以提供默认值：

```properties
app.jwt.expiration-minutes=${JWT_EXPIRATION_MINUTES:120}
```

这表示：

```text
如果有 JWT_EXPIRATION_MINUTES，就用环境变量
如果没有，就用 120
```

两种写法的区别：

| 写法 | 含义 |
|---|---|
| `${JWT_SECRET}` | 必须提供，否则启动失败 |
| `${JWT_EXPIRATION_MINUTES:120}` | 没提供就使用默认值 120 |

JWT secret、数据库密码这类敏感信息，不应该写死在代码仓库里。

## 第五天：为什么不用 @Value 到处读取配置？

以前代码里有这种写法：

```java
@Value("${app.jwt.secret}")
private String secret;
```

或者构造方法参数里：

```java
@Value("${app.jwt.expiration-minutes}") long expirationMinutes
```

这种方式简单直接，学习早期没问题。

但项目变大后有几个缺点：

```text
配置 key 散落在很多类里
配置结构不清晰
不容易统一校验
IDE 对配置提示较弱
重命名配置容易漏改
```

所以第十九周改成：

```java
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes
) {
}
```

业务代码里注入：

```java
JwtProperties jwtProperties
```

这样配置结构就变成了一个明确的 Java 类型。

## 第六天：@ConfigurationPropertiesScan 是什么？

文件：

```text
src/main/java/com/zading/todoapi/TodoApiApplication.java
```

新增：

```java
@ConfigurationPropertiesScan
```

它的作用是：

```text
告诉 Spring Boot 去扫描项目里的 @ConfigurationProperties 类。
```

没有这个注解，`JwtProperties`、`RequestLoggingProperties`、`TodoOverdueJobProperties` 这些配置类不会自动注册成 Spring Bean。

你可以把它理解成：

```text
打开“配置类自动发现”开关。
```

## 第七天：JwtProperties 做了什么？

文件：

```text
src/main/java/com/zading/todoapi/config/properties/JwtProperties.java
```

代码：

```java
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Min(1) long expirationMinutes
) {
}
```

它绑定的配置是：

```properties
app.jwt.secret=...
app.jwt.expiration-minutes=...
```

`@NotBlank` 表示：

```text
secret 不能为空。
```

`@Min(1)` 表示：

```text
expirationMinutes 至少为 1。
```

这比普通 `@Value` 更工程化。

因为配置错误时，应用会在启动阶段尽早失败。

## 第八天：JwtService 改了什么？

文件：

```text
src/main/java/com/zading/todoapi/security/JwtService.java
```

以前构造方法大概是：

```java
public JwtService(
        ObjectMapper objectMapper,
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-minutes}") long expirationMinutes
) {
}
```

现在改成：

```java
public JwtService(
        ObjectMapper objectMapper,
        JwtProperties jwtProperties
) {
    this.objectMapper = objectMapper;
    this.jwtProperties = jwtProperties;
}
```

生成 Token 时：

```java
Instant expiresAt = now.plusSeconds(jwtProperties.expirationMinutes() * 60);
```

签名时：

```java
SecretKeySpec key = new SecretKeySpec(
        jwtProperties.secret().getBytes(StandardCharsets.UTF_8),
        HMAC_ALGORITHM
);
```

这样 `JwtService` 仍然负责 JWT 业务，但配置来源变得更清晰。

## 第九天：TodoOverdueJobProperties 做了什么？

文件：

```text
src/main/java/com/zading/todoapi/config/properties/TodoOverdueJobProperties.java
```

代码：

```java
@Validated
@ConfigurationProperties(prefix = "app.todo.overdue-job")
public record TodoOverdueJobProperties(
        boolean enabled,
        @NotBlank String cron,
        @NotBlank String zone,
        @Min(1) int pageSize
) {
}
```

它绑定：

```properties
app.todo.overdue-job.enabled=true
app.todo.overdue-job.cron=0 0 9 * * *
app.todo.overdue-job.zone=Asia/Shanghai
app.todo.overdue-job.page-size=50
```

这样定时任务的配置集中在一个对象里。

`TodoOverdueJob` 里就不需要多个 `@Value` 参数。

## 第十天：RequestLoggingProperties 做了什么？

文件：

```text
src/main/java/com/zading/todoapi/config/properties/RequestLoggingProperties.java
```

代码：

```java
@ConfigurationProperties(prefix = "app.request-logging")
public record RequestLoggingProperties(
        boolean enabled,
        String requestIdHeader
) {
    public RequestLoggingProperties {
        if (requestIdHeader == null || requestIdHeader.isBlank()) {
            requestIdHeader = "X-Request-Id";
        }
    }
}
```

它绑定：

```properties
app.request-logging.enabled=true
app.request-logging.request-id-header=X-Request-Id
```

这里有一个小细节：

```java
if (requestIdHeader == null || requestIdHeader.isBlank()) {
    requestIdHeader = "X-Request-Id";
}
```

意思是：

```text
如果没有配置请求头名称，就默认使用 X-Request-Id。
```

这是一个温柔的小兜底。工程里很多稳定性就是靠这种不声不响的小兜底撑起来的。

## 第十一天：requestId 是什么？

requestId 是一次请求的唯一标识。

例如：

```text
requestId=test-request-id-001 HTTP GET /hello -> 200 (1 ms)
```

如果一次请求经过很多层代码：

```text
Filter
Controller
Service
Repository
ExceptionHandler
```

有了 requestId，就可以在日志里把同一次请求串起来。

没有 requestId 时，日志就像一地散落的拼图。

有 requestId 时，你至少知道哪些碎片属于同一幅图。

## 第十二天：RequestLoggingFilter 改了什么？

文件：

```text
src/main/java/com/zading/todoapi/logging/RequestLoggingFilter.java
```

现在请求进入时，会先解析 requestId：

```java
String requestId = resolveRequestId(request);
```

如果请求头里已经有：

```http
X-Request-Id: test-request-id-001
```

就复用这个值。

如果没有，就生成一个 UUID：

```java
UUID.randomUUID().toString()
```

然后写入响应头：

```java
response.setHeader(properties.requestIdHeader(), requestId);
```

这样前端或调用方可以在响应里看到本次请求 ID。

日志里也会打印：

```java
log.info(
        "requestId={} HTTP {} {} -> {} ({} ms)",
        requestId,
        request.getMethod(),
        request.getRequestURI(),
        response.getStatus(),
        duration
);
```

## 第十三天：MDC 是什么？

代码：

```java
MDC.put("requestId", requestId);
```

MDC 是 SLF4J 提供的日志上下文。

你可以先简单理解为：

```text
当前线程里的日志小背包。
```

把 requestId 放进去后，同一个线程后续日志也可以读取到它。

本项目目前日志格式还没有把 MDC 自动输出到每一行，但先把 requestId 放进去，是为了后续扩展日志格式做准备。

请求结束时：

```java
MDC.remove("requestId");
```

为什么要 remove？

因为 Web 服务器线程会复用。

如果不清理，下一次请求可能错误地拿到上一次请求的 requestId。

这是一个非常真实的后端坑。

## 第十四天：liveness 和 readiness 是什么？

第十八周我们学了：

```http
GET /actuator/health
```

第十九周继续开启：

```http
GET /actuator/health/liveness
GET /actuator/health/readiness
```

简单理解：

| 探针 | 含义 |
|---|---|
| liveness | 应用进程是不是还活着 |
| readiness | 应用是否准备好接收请求 |

区别很重要：

```text
liveness 更像“人有没有醒着”
readiness 更像“人现在能不能接活”
```

有些时候应用进程活着，但数据库还没连上，或者初始化还没完成。

这时：

```text
liveness 可能是 UP
readiness 可能不是 UP
```

部署系统可以据此决定：

```text
要不要重启应用
要不要把流量打给应用
```

## 第十五天：为什么 SecurityConfig 要放行 /actuator/health/**？

以前放行的是：

```java
"/actuator/health"
```

现在新增了：

```java
"/actuator/health/**"
```

因为：

```text
/actuator/health/liveness
/actuator/health/readiness
```

都是 `/actuator/health` 下面的子路径。

如果不放行，访问这些探针就会被 Spring Security 拦住。

健康探针通常要给部署平台或监控系统访问，所以基础健康端点可以公开。

但这不代表所有内部接口都公开。

例如：

```text
/api/internal/jobs/todo-overdue
```

仍然需要登录。

## 第十六天：mvn spring-boot:run 和 java -jar 有什么区别？

开发期常用：

```bash
mvn spring-boot:run
```

它适合本地开发，因为 Maven 会帮你编译并启动。

更接近部署的方式是：

```bash
mvn package
java -jar target/java-todo-api-1.0.0.jar
```

区别：

| 命令 | 使用场景 |
|---|---|
| `mvn spring-boot:run` | 本地开发 |
| `java -jar ...` | 打包后运行，更接近部署 |

学习后端时，你一定要会 `java -jar`。

因为真实服务器上通常不会打开 VSCode 点运行。

## 第十七天：本周测试补了什么？

文件：

```text
src/test/java/com/zading/todoapi/ActuatorTests.java
```

新增验证：

```java
mockMvc.perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
```

以及：

```java
mockMvc.perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
```

文件：

```text
src/test/java/com/zading/todoapi/ApplicationSmokeTests.java
```

新增验证：

```java
mockMvc.perform(get("/hello")
                .header("X-Request-Id", "test-request-id-001"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Request-Id", "test-request-id-001"));
```

这说明：

```text
请求带进来的 requestId，会被系统放回响应头。
```

## 本周你需要重点理解的代码关系

```text
application.properties
application-dev.properties
application-prod.properties
  -> 配置来源

@ConfigurationPropertiesScan
  -> 扫描配置类

JwtProperties / RequestLoggingProperties / TodoOverdueJobProperties
  -> 把配置绑定成 Java 对象

JwtService / TodoOverdueJob / RequestLoggingFilter
  -> 使用配置对象

SecurityConfig
  -> 放行 Actuator 探针

ActuatorTests / ApplicationSmokeTests
  -> 验证工程能力没有坏
```

## 本周复盘问题和参考答案

### 1. 什么是 profile？

profile 是 Spring Boot 用来区分运行环境的一套机制。

例如：

```text
dev：开发环境
test：测试环境
prod：生产环境
```

不同 profile 可以加载不同配置文件。

### 2. 为什么生产环境不能使用默认 JWT secret？

JWT secret 用来签名 Token。

如果 secret 被别人知道，对方就可能伪造 Token。

所以生产环境必须使用足够强、且不提交到代码仓库的密钥。

### 3. 环境变量有什么作用？

环境变量可以把敏感信息和环境差异从代码里拿出去。

这样同一份代码可以在不同环境使用不同配置。

### 4. `${ENV}` 和 `${ENV:default}` 有什么区别？

`${ENV}` 表示必须提供环境变量。

`${ENV:default}` 表示如果环境变量不存在，就使用默认值。

### 5. `@ConfigurationProperties` 比 `@Value` 好在哪里？

它可以把一组配置绑定成一个 Java 对象。

优点是：

```text
结构清晰
方便校验
方便测试
不容易散落配置 key
更适合大型项目
```

### 6. requestId 解决什么问题？

requestId 用来标识一次请求。

当日志很多时，可以通过 requestId 把同一次请求相关日志串起来，方便排查问题。

### 7. 为什么请求结束后要清理 MDC？

因为 Web 服务器线程会复用。

如果不清理，下一次请求可能错误使用上一次请求的 requestId。

### 8. liveness 和 readiness 有什么区别？

liveness 关注应用进程是否还活着。

readiness 关注应用是否准备好接收请求。

### 9. 为什么 `/actuator/health/**` 可以公开？

因为基础健康探针通常要给监控系统或部署平台调用。

但公开范围要控制，只公开必要端点，不代表所有内部接口都可以公开。

### 10. `mvn spring-boot:run` 和 `java -jar` 有什么区别？

`mvn spring-boot:run` 更适合开发期。

`java -jar` 更接近真实部署方式。

## 本周建议练习

按下面顺序操作：

1. 运行测试：

```bash
mvn test
```

2. 打包：

```bash
mvn package
```

3. 默认方式启动：

```bash
java -jar target/java-todo-api-1.0.0.jar
```

4. 使用 dev profile 启动：

```bash
java -jar target/java-todo-api-1.0.0.jar --spring.profiles.active=dev
```

5. 访问健康探针：

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

6. 带 requestId 访问：

```bash
curl -H "X-Request-Id: learning-001" http://localhost:8080/hello
```

然后观察响应头和控制台日志。

## 本周小结

第十九周完成后，项目新增了这些工程能力：

```text
dev / prod 多环境配置
生产环境敏感配置外部化
配置类绑定和校验
请求日志 requestId
Actuator liveness / readiness
jar 打包运行说明
```

这周没有新增复杂业务功能，但非常重要。

因为真实后端项目不只是 Controller、Service、Repository。

它还要回答：

```text
怎么配置？
怎么启动？
怎么排查？
怎么避免敏感信息泄露？
怎么让部署系统判断应用是否健康？
```

这些就是工程化能力。你已经在从“会写 Java 程序”往“会做 Java 后端工程”走了。
