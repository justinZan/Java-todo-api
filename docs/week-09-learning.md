# 第九周学习说明：Docker Compose、PostgreSQL、OpenAPI 和请求日志

第九周的目标不是继续堆业务功能，而是把项目继续往“可运行、可交付、可排查”的后端工程推进。

第八周结束时，项目已经有：

```text
用户注册
用户登录
JWT 鉴权
Todo 数据按用户隔离
```

第九周做的是工程化能力：

```text
提供 PostgreSQL 一键启动配置
保留默认 H2 本地运行能力
提供 Swagger / OpenAPI 接口文档
增加请求日志
补充工程 README
```

注意：你当前本机没有安装 PostgreSQL 和 Docker，所以本周没有真正启动 PostgreSQL 容器。我们只是先把配置文件、代码和文档准备好。这样以后安装 Docker 后，可以直接使用。

## 本周最终改动总览

| 改动 | 文件 | 目的 |
|---|---|---|
| 新增 Docker Compose 配置 | `docker-compose.yml` | 以后可以一键启动 PostgreSQL |
| 新增环境变量示例 | `.env.example` | 统一数据库和 JWT 配置示例 |
| 新增 Springdoc OpenAPI 依赖 | `pom.xml` | 自动生成接口文档 |
| 新增 OpenAPI 配置类 | `config/OpenApiConfig.java` | 设置文档标题、版本和 JWT 鉴权说明 |
| Security 放行文档路径 | `security/SecurityConfig.java` | 未登录也能查看接口文档 |
| 新增请求日志 Filter | `logging/RequestLoggingFilter.java` | 记录请求方法、路径、状态码和耗时 |
| 增加日志配置 | `application.properties` | 控制请求日志和应用日志级别 |
| 测试 OpenAPI 文档 | `TodoApiApplicationTests.java` | 保证 `/v3/api-docs` 可访问 |
| 更新 README | `README.md` | 工程化使用说明 |

## 第一步：为什么先写 Docker Compose，但不安装 Docker？

因为工程化项目不只是“我电脑上能跑”，还要让别人知道项目依赖什么、怎么启动。

现在你本地还没有 Docker 和 PostgreSQL，但我们仍然可以先添加：

```text
docker-compose.yml
```

它是一份运行说明书：

```text
这个项目需要一个 PostgreSQL
数据库名是什么
用户名是什么
密码是什么
端口是多少
数据放在哪里
健康检查怎么做
```

这不会安装 Docker，也不会启动 PostgreSQL。

只有你以后主动执行：

```bash
docker compose up -d postgres
```

才会真正启动容器。

## 第二步：docker-compose.yml 怎么读？

文件：

```text
docker-compose.yml
```

核心内容：

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: java-todo-api-postgres
    restart: unless-stopped
```

解释：

```yaml
services:
```

表示要启动哪些服务。

这里目前只有一个：

```yaml
postgres:
```

也就是 PostgreSQL 数据库服务。

```yaml
image: postgres:16-alpine
```

表示使用 PostgreSQL 16 的 Alpine 镜像。

Alpine 版本通常更小，适合本地开发。

```yaml
container_name: java-todo-api-postgres
```

容器名字固定，方便你以后查看：

```bash
docker ps
```

```yaml
restart: unless-stopped
```

表示容器异常退出时自动重启，除非你手动停止它。

## 第三步：environment 是什么？

Compose 里有：

```yaml
environment:
  POSTGRES_DB: ${POSTGRES_DB:-java_todo_api}
  POSTGRES_USER: ${POSTGRES_USER:-postgres}
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
```

这是给 PostgreSQL 容器传环境变量。

语法：

```text
${变量名:-默认值}
```

例如：

```yaml
POSTGRES_DB: ${POSTGRES_DB:-java_todo_api}
```

意思是：

```text
如果环境变量 POSTGRES_DB 存在，就用它；
如果不存在，就用 java_todo_api。
```

这样做的好处是：

```text
默认值适合本地学习
真正部署时可以用环境变量覆盖
```

## 第四步：ports 和 volumes 是什么？

Compose 中：

```yaml
ports:
  - "${POSTGRES_PORT:-5432}:5432"
```

意思是：

```text
把你电脑的 5432 端口映射到容器内部的 5432 端口。
```

PostgreSQL 默认监听 5432。

所以应用可以通过：

```text
localhost:5432
```

连接容器里的 PostgreSQL。

再看：

```yaml
volumes:
  - postgres_data:/var/lib/postgresql/data
```

这表示数据库数据不要只放在容器内部，而是放到一个 Docker volume。

为什么？

如果数据只放在容器里，容器删除后数据可能丢失。

使用 volume 后：

```text
容器可以删
数据还在 volume 里
```

这就是持久化。

## 第五步：healthcheck 是什么？

Compose 中：

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-java_todo_api}"]
  interval: 10s
  timeout: 5s
  retries: 5
```

`healthcheck` 用来检查 PostgreSQL 是否真的准备好了。

容器启动不等于数据库马上可连接。

PostgreSQL 启动时需要初始化数据库、加载配置。

`pg_isready` 是 PostgreSQL 自带的检查命令。

这段配置的意思是：

```text
每 10 秒检查一次
每次最多等 5 秒
失败 5 次后认为不健康
```

## 第六步：为什么加 `.env.example`？

文件：

```text
.env.example
```

内容：

```properties
POSTGRES_DB=java_todo_api
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_PORT=5432

DB_URL=jdbc:postgresql://localhost:5432/java_todo_api
DB_USERNAME=postgres
DB_PASSWORD=postgres

JWT_SECRET=replace-with-a-long-random-secret-at-least-32-chars
JWT_EXPIRATION_MINUTES=120
```

`.env.example` 是模板，不是机密文件。

团队协作时，通常不会提交真正的 `.env`，因为里面可能有密码、密钥。

正确做法：

```bash
cp .env.example .env
```

然后你本地修改 `.env`。

`.env.example` 的价值是：

```text
告诉别人这个项目需要哪些环境变量。
```

## 第七步：为什么加入 OpenAPI / Swagger？

前端开发很熟悉一个痛点：

```text
后端接口到底有哪些？
请求体怎么传？
响应字段是什么？
哪些接口需要 token？
```

Swagger / OpenAPI 解决这个问题。

本周新增依赖：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

它会扫描 Spring MVC Controller，自动生成接口文档。

启动项目后可以访问：

```text
http://localhost:8080/swagger-ui.html
```

也可以访问机器可读的 JSON：

```text
http://localhost:8080/v3/api-docs
```

简单理解：

```text
OpenAPI = 接口说明数据
Swagger UI = 把接口说明渲染成可视化页面
```

## 第八步：OpenApiConfig 做了什么？

文件：

```text
src/main/java/com/zading/todoapi/config/OpenApiConfig.java
```

核心代码：

```java
@Configuration
public class OpenApiConfig {
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI todoApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java Todo API")
                        .version("1.0.0")
                        .description("带用户注册、JWT 登录认证和用户数据隔离的 Todo REST API"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }
}
```

逐句解释：

```java
@Configuration
```

表示这是一个 Spring 配置类。

```java
@Bean
```

表示把方法返回的 `OpenAPI` 对象交给 Spring 管理。

```java
.title("Java Todo API")
```

设置文档标题。

```java
.version("1.0.0")
```

设置接口文档版本。

```java
.addSecurityItem(...)
```

告诉 OpenAPI：这个接口项目使用认证。

```java
.scheme("bearer")
.bearerFormat("JWT")
```

告诉 Swagger UI：认证方式是：

```http
Authorization: Bearer <token>
```

这样在 Swagger 页面里就能看到 Authorize 按钮，可以填 JWT token。

## 第九步：为什么 SecurityConfig 要放行 Swagger？

第八周之后，默认规则是：

```java
anyRequest().authenticated()
```

也就是除了明确放行的接口，其他都要登录。

如果不放行 Swagger，访问接口文档也会返回：

```http
401 Unauthorized
```

所以本周把这些路径加入放行：

```java
"/v3/api-docs/**",
"/swagger-ui.html",
"/swagger-ui/**"
```

完整逻辑变成：

```text
/hello                    允许匿名
/api/auth/register        允许匿名
/api/auth/login           允许匿名
/h2-console/**            允许匿名
/v3/api-docs/**           允许匿名
/swagger-ui.html          允许匿名
/swagger-ui/**            允许匿名
其他接口                  必须登录
```

这体现了一个常见思路：

```text
文档可以看
业务数据必须登录
```

## 第十步：为什么要加请求日志？

接口出问题时，第一件事通常不是看代码，而是看日志。

比如你想知道：

```text
有没有请求进来？
请求的是哪个路径？
返回状态码是多少？
接口耗时多久？
```

所以本周新增：

```text
src/main/java/com/zading/todoapi/logging/RequestLoggingFilter.java
```

它会记录类似：

```text
HTTP GET /api/todos -> 200 (12 ms)
```

这条日志包含：

```text
请求方法：GET
请求路径：/api/todos
响应状态：200
耗时：12 ms
```

## 第十一步：RequestLoggingFilter 怎么读？

核心代码：

```java
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Value("${app.request-logging.enabled:true}")
    private boolean enabled;
}
```

解释：

```java
@Component
```

表示这是一个 Spring Bean，Spring Boot 会自动发现它。

```java
OncePerRequestFilter
```

表示每个 HTTP 请求只执行一次。

```java
@Value("${app.request-logging.enabled:true}")
```

从配置文件读取：

```properties
app.request-logging.enabled=true
```

如果配置不存在，默认值是 `true`。

再看：

```java
protected boolean shouldNotFilter(HttpServletRequest request) {
    return !enabled || request.getRequestURI().startsWith("/h2-console");
}
```

意思是：

```text
如果日志开关关闭，就不记录
如果是 H2 Console 请求，也不记录
```

因为 H2 Console 页面会加载很多资源，不过滤的话日志会比较吵。

核心执行：

```java
long startTime = System.currentTimeMillis();

try {
    filterChain.doFilter(request, response);
} finally {
    long duration = System.currentTimeMillis() - startTime;
    log.info(
            "HTTP {} {} -> {} ({} ms)",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            duration
    );
}
```

可以理解成：

```text
记录开始时间
继续执行后续过滤器和 Controller
无论成功还是失败，最后记录耗时和状态码
```

为什么用 `finally`？

因为即使请求中间抛异常，也希望记录日志。

## 第十二步：为什么测试环境关闭请求日志？

测试配置：

```text
src/test/resources/application-test.properties
```

新增：

```properties
app.request-logging.enabled=false
```

原因是测试会频繁请求接口。

如果测试也打印请求日志，输出会非常多，不利于看失败原因。

所以：

```text
开发运行：开启请求日志
自动化测试：关闭请求日志
```

这就是 profile 配置的价值。

## 第十三步：本周为什么没有真正验证 PostgreSQL？

因为你当前电脑没有 PostgreSQL 和 Docker。

我们本周遵守这个限制：

```text
不安装 PostgreSQL
不安装 Docker
不启动 Docker 容器
```

所以测试仍然使用：

```text
H2 内存数据库
```

这不影响代码质量，因为：

```text
默认 H2 能跑
PostgreSQL profile 配置已经准备好
Flyway migration 仍然能在 H2 测试里验证语法和基础结构
Docker Compose 配置作为工程运行约定存在
```

以后你安装 Docker 后，再补一次 PostgreSQL 真机验证即可。

## 第十四步：本周新增测试说明

新增测试：

```java
void shouldExposeOpenApiDocsWithoutLogin()
```

它请求：

```http
GET /v3/api-docs
```

断言：

```java
.andExpect(status().isOk())
.andExpect(jsonPath("$.openapi").exists())
.andExpect(jsonPath("$.info.title").value("Java Todo API"));
```

这证明：

```text
OpenAPI 文档接口存在
未登录也能访问
文档标题是 Java Todo API
```

之前第八周的测试仍然保留：

```text
GET /api/todos 未登录返回 401
```

所以现在安全边界是清楚的：

```text
文档接口允许匿名
业务接口必须登录
```

## 当前使用方式

默认 H2 启动：

```bash
mvn spring-boot:run
```

查看健康检查：

```bash
curl http://localhost:8080/hello
```

查看接口文档：

```text
http://localhost:8080/swagger-ui.html
```

查看 OpenAPI JSON：

```bash
curl http://localhost:8080/v3/api-docs
```

未来安装 Docker 后启动 PostgreSQL：

```bash
cp .env.example .env
docker compose up -d postgres
```

再用 PostgreSQL profile 启动应用：

```bash
DB_USERNAME=postgres DB_PASSWORD=postgres mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## 本周复盘问题

1. Docker Compose 解决的是什么问题？
2. 为什么本周没有安装 Docker，也仍然可以写 `docker-compose.yml`？
3. `.env.example` 和 `.env` 有什么区别？
4. 为什么默认环境继续使用 H2？
5. 为什么还要保留 PostgreSQL profile？
6. OpenAPI 和 Swagger UI 有什么区别？
7. 为什么 Swagger 文档路径要在 SecurityConfig 里放行？
8. 请求日志应该记录哪些信息？
9. 为什么测试环境可以关闭请求日志？
10. 为什么 README 应该写工程说明，而学习解释放在 docs？

## 参考答案

### 1. Docker Compose 解决的是什么问题？

Docker Compose 用一个配置文件描述项目依赖的外部服务。

对当前项目来说，它描述的是：

```text
PostgreSQL 镜像
数据库名
用户名
密码
端口
数据卷
健康检查
```

以后别人拿到项目，不需要猜数据库怎么启动。

### 2. 为什么本周没有安装 Docker，也仍然可以写 `docker-compose.yml`？

因为 `docker-compose.yml` 是配置文件。

写文件不会安装 Docker，也不会启动容器。

它只是把“以后如何启动 PostgreSQL”的约定放进仓库。

### 3. `.env.example` 和 `.env` 有什么区别？

`.env.example` 是示例，可以提交到 Git。

`.env` 是本地真实配置，可能包含密码和密钥，通常不提交。

### 4. 为什么默认环境继续使用 H2？

因为 H2 轻量，不需要额外安装数据库。

这样项目对学习者更友好：

```text
只要 JDK + Maven 就能启动
```

### 5. 为什么还要保留 PostgreSQL profile？

因为真实后端项目通常不会用 H2 作为生产数据库。

PostgreSQL 更接近真实环境。

默认 H2 解决学习和快速运行，PostgreSQL profile 解决工程化和真实环境适配。

### 6. OpenAPI 和 Swagger UI 有什么区别？

OpenAPI 是接口描述规范，通常表现为 JSON。

Swagger UI 是把 OpenAPI JSON 渲染成可视化页面的工具。

可以简单理解为：

```text
OpenAPI = 数据
Swagger UI = 页面
```

### 7. 为什么 Swagger 文档路径要在 SecurityConfig 里放行？

因为第八周之后，默认所有未放行接口都需要登录。

如果不放行：

```text
/v3/api-docs
/swagger-ui/**
```

访问接口文档也会返回 401。

文档允许匿名访问，但业务接口仍然必须登录。

### 8. 请求日志应该记录哪些信息？

至少记录：

```text
请求方法
请求路径
响应状态码
耗时
```

当前项目记录：

```text
HTTP GET /api/todos -> 200 (12 ms)
```

### 9. 为什么测试环境可以关闭请求日志？

测试请求很多。

如果每个请求都打印日志，测试输出会很吵。

关闭请求日志可以让失败信息更清晰。

### 10. 为什么 README 应该写工程说明，而学习解释放在 docs？

README 是给使用项目的人看的，要快速说明：

```text
项目是什么
怎么启动
怎么配置
怎么测试
有哪些接口
```

学习文档是给学习过程看的，可以解释：

```text
为什么这么写
每段代码怎么理解
踩坑点是什么
复盘问题是什么
```

把两者分开，项目更专业，学习也更清晰。

## 更多复盘问题

1. `ports` 中 `"5432:5432"` 左右两边分别代表什么？
2. 为什么 PostgreSQL 数据要放到 Docker volume？
3. `healthcheck` 为什么不能省？
4. 为什么 JWT 密钥应该用环境变量注入？
5. `@Configuration` 和 `@Component` 有什么相似点？
6. `@Bean` 的作用是什么？
7. 为什么请求日志不应该打印 Authorization token？
8. OpenAPI 文档对前端开发有什么帮助？
9. 为什么工程项目需要同时考虑“代码”和“运行方式”？
10. 如果以后要在 CI 里测 PostgreSQL，你会怎么做？

## 更多复盘问题参考答案

### 1. `ports` 中 `"5432:5432"` 左右两边分别代表什么？

左边是宿主机端口，右边是容器内部端口。

```text
宿主机端口:容器端口
```

应用访问 `localhost:5432`，实际上会连接到容器里的 PostgreSQL 5432。

### 2. 为什么 PostgreSQL 数据要放到 Docker volume？

为了持久化数据。

如果数据只放在容器里，删除容器可能导致数据丢失。

放到 volume 后，容器删除再重建，数据仍然可以保留。

### 3. `healthcheck` 为什么不能省？

容器启动不代表服务已经可用。

PostgreSQL 可能还在初始化。

`healthcheck` 可以告诉我们数据库是否真的准备好接受连接。

### 4. 为什么 JWT 密钥应该用环境变量注入？

因为密钥属于敏感配置。

如果写死在代码或提交到 Git，别人拿到代码就能伪造 token。

用环境变量可以让不同环境使用不同密钥。

### 5. `@Configuration` 和 `@Component` 有什么相似点？

它们都会让类成为 Spring Bean。

区别是：

```text
@Component      表示普通组件
@Configuration  表示配置类，里面通常定义 @Bean
```

### 6. `@Bean` 的作用是什么？

`@Bean` 标在方法上，表示：

```text
把这个方法返回的对象交给 Spring 容器管理
```

本周 `OpenApiConfig` 里用 `@Bean` 创建了 `OpenAPI` 对象。

### 7. 为什么请求日志不应该打印 Authorization token？

因为 token 是身份凭证。

如果日志里打印 token，日志泄露就等于登录凭证泄露。

所以当前请求日志只打印方法、路径、状态码和耗时。

### 8. OpenAPI 文档对前端开发有什么帮助？

前端可以直接查看：

```text
接口路径
请求方法
请求参数
请求体
响应结构
认证方式
```

以后还可以基于 OpenAPI 生成 TypeScript 类型或 API client。

### 9. 为什么工程项目需要同时考虑“代码”和“运行方式”？

因为真实项目不是只看代码是否正确。

还要考虑：

```text
别人怎么启动
依赖服务怎么准备
配置怎么传
出错怎么排查
接口怎么协作
```

这些都是工程化的一部分。

### 10. 如果以后要在 CI 里测 PostgreSQL，你会怎么做？

可以在 CI 里启动 PostgreSQL 服务，然后使用 postgres profile 跑测试。

大致流程：

```text
启动 PostgreSQL
设置 DB_URL / DB_USERNAME / DB_PASSWORD
运行 mvn test 或专门的集成测试
```

这可以验证 Flyway migration 和 PostgreSQL 兼容性。
