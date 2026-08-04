# 第十周学习说明：测试体系重构、集成测试思维和 CI 准备

第十周的目标是把测试从“能跑”升级成“更容易维护的质量保护网”。

第九周结束时，项目已经有了：

```text
用户注册 / 登录
JWT 鉴权
Todo 数据隔离
Flyway 迁移
OpenAPI 文档
请求日志
Docker Compose PostgreSQL 配置
```

但测试仍然集中在一个大文件里。

第十周做的事情是：

```text
拆分测试类
提取测试辅助类
准备 CI 配置
补充测试体系文档
```

本周没有新增业务功能。它更像一次“测试代码重构”。

这很重要，因为真实项目里，测试代码也是代码，也需要结构清晰、职责明确、重复可控。

## 本周最终改动总览

| 改动 | 文件 | 目的 |
|---|---|---|
| 删除大测试类 | `TodoApiApplicationTests.java` | 避免所有测试堆在一个类里 |
| 新增冒烟测试 | `ApplicationSmokeTests.java` | 验证应用基础接口可用 |
| 新增认证测试 | `AuthApiTests.java` | 专门测试注册、登录、认证错误 |
| 新增 Todo 测试 | `TodoApiTests.java` | 专门测试 Todo 业务接口 |
| 新增 OpenAPI 测试 | `OpenApiTests.java` | 专门测试接口文档可访问 |
| 新增测试基类 | `support/AbstractApiTest.java` | 统一 Spring 测试配置和数据清理 |
| 新增认证测试客户端 | `support/AuthTestClient.java` | 封装注册、登录、bearer token |
| 新增 Todo 测试客户端 | `support/TodoTestClient.java` | 封装创建 Todo、读取 id |
| 新增 CI 配置 | `.github/workflows/ci.yml` | 为 GitHub Actions 准备自动测试流程 |
| 更新 README | `README.md` | 说明测试结构和 CI |

## 第一步：为什么要拆测试类？

之前所有测试都在：

```text
TodoApiApplicationTests.java
```

它同时测试：

```text
/hello
/api/auth/register
/api/auth/login
/api/todos
/v3/api-docs
```

早期这样没问题，因为项目小。

但随着功能增加，一个测试类会越来越长，问题也越来越明显：

- 找某个测试要滚很久。
- 认证测试和 Todo 测试混在一起。
- 辅助方法越来越多，不知道谁在用。
- 修改一个功能时，不容易知道应该看哪个测试区域。

所以第十周拆成：

```text
ApplicationSmokeTests.java
AuthApiTests.java
TodoApiTests.java
OpenApiTests.java
```

这对应一个工程原则：

```text
测试类应该按功能边界组织。
```

## 第二步：新的测试结构怎么读？

现在测试目录是：

```text
src/test/java/com/zading/todoapi/
├── ApplicationSmokeTests.java
├── AuthApiTests.java
├── OpenApiTests.java
├── TodoApiTests.java
└── support/
    ├── AbstractApiTest.java
    ├── AuthTestClient.java
    └── TodoTestClient.java
```

每个类的职责：

```text
ApplicationSmokeTests  验证应用基础启动和 /hello
AuthApiTests           验证注册、登录、重复用户名、错误密码
TodoApiTests           验证 Todo CRUD、筛选、分页、数据隔离
OpenApiTests           验证 /v3/api-docs 可访问
support/               放测试辅助代码
```

这就像前端项目里把测试拆成：

```text
auth.spec.ts
todo.spec.ts
router.spec.ts
```

而不是全部塞进一个 `app.spec.ts`。

## 第三步：什么是冒烟测试？

文件：

```text
src/test/java/com/zading/todoapi/ApplicationSmokeTests.java
```

代码：

```java
class ApplicationSmokeTests extends AbstractApiTest {
    @Test
    void shouldReturnHelloMessage() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Hello Spring Boot"));
    }
}
```

冒烟测试的意思是：

```text
先做一个非常基础的检查，确认应用没有完全坏掉。
```

名字来自硬件测试：设备通电后，如果冒烟了，说明基本就坏了。

在后端项目里，冒烟测试通常验证：

```text
应用能启动
健康检查接口正常
基础路由可访问
```

这个测试不负责覆盖复杂业务。

它的价值是快速告诉你：

```text
项目最基本的启动链路还活着。
```

## 第四步：AuthApiTests 为什么单独拆出来？

文件：

```text
src/test/java/com/zading/todoapi/AuthApiTests.java
```

它只关心认证接口：

```java
class AuthApiTests extends AbstractApiTest {
    @Test
    void shouldRegisterAndLoginUser() throws Exception {
        ...
    }

    @Test
    void shouldRejectDuplicateUsernameAndWrongPassword() throws Exception {
        ...
    }
}
```

这类测试回答的问题是：

```text
用户能不能注册？
用户能不能登录？
重复用户名是否失败？
密码错误是否返回 401？
```

它不关心 Todo。

这样做的好处是：以后注册逻辑变了，你知道优先看 `AuthApiTests`。

## 第五步：TodoApiTests 负责什么？

文件：

```text
src/test/java/com/zading/todoapi/TodoApiTests.java
```

它负责 Todo 业务行为：

```text
创建 Todo
查询 Todo 列表
修改 Todo
切换完成状态
删除 Todo
按 completed 筛选
按 keyword 搜索
分页和排序
默认 priority
标题为空返回 400
Todo 不存在返回 404
未登录返回 401
用户数据隔离
```

Todo 测试都需要登录，所以里面经常先写：

```java
String token = authClient.registerAndLogin("zading", "123456");
```

然后请求业务接口时带：

```java
.header("Authorization", authClient.bearer(token))
```

这和真实前端调用接口的流程一致：

```text
注册 / 登录
拿 token
带 token 请求 Todo
```

## 第六步：OpenApiTests 为什么也要单独存在？

文件：

```text
src/test/java/com/zading/todoapi/OpenApiTests.java
```

代码：

```java
class OpenApiTests extends AbstractApiTest {
    @Test
    void shouldExposeOpenApiDocsWithoutLogin() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Java Todo API"));
    }
}
```

它测试的是工程配置，不是业务逻辑。

为什么配置也要测试？

因为真实项目里很多问题不是业务代码写错，而是配置写错：

```text
Swagger 路径没有放行
Security 拦截了文档
OpenAPI 配置没生效
```

这个测试能保护第九周新增的接口文档能力。

## 第七步：AbstractApiTest 是做什么的？

文件：

```text
src/test/java/com/zading/todoapi/support/AbstractApiTest.java
```

核心代码：

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class AbstractApiTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected AuthTestClient authClient;
    protected TodoTestClient todoClient;
}
```

这是测试基类。

它把所有 API 测试都需要的配置集中起来：

```text
启动完整 Spring Boot 测试环境
启用 MockMvc
使用 test profile
注入 MockMvc
注入 ObjectMapper
创建测试辅助客户端
清理测试数据
```

为什么要抽基类？

因为如果每个测试类都重复写：

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
```

重复会很多。

更重要的是，未来如果测试配置要调整，只需要改一个地方。

## 第八步：AbstractApiTest 如何清理数据？

代码：

```java
@BeforeEach
void setUpApiTest() {
    todoRepository.deleteAll();
    userRepository.deleteAll();
    authClient = new AuthTestClient(mockMvc, objectMapper);
    todoClient = new TodoTestClient(mockMvc, objectMapper);
}
```

`@BeforeEach` 表示：

```text
每个测试方法执行前都会执行。
```

为什么先删 Todo，再删 User？

因为 Todo 通过 `user_id` 关联 User。

数据库里有外键约束：

```text
Todo -> User
```

如果先删 User，可能因为还有 Todo 引用它而失败。

所以顺序是：

```text
先删 Todo
再删 User
```

这就是数据库关系对测试清理顺序的影响。

## 第九步：AuthTestClient 为什么存在？

文件：

```text
src/test/java/com/zading/todoapi/support/AuthTestClient.java
```

它封装认证相关测试操作：

```java
public ResultActions register(String username, String password)
public ResultActions login(String username, String password)
public String registerAndLogin(String username, String password)
public String bearer(String token)
```

以前每个测试都要手写：

```java
mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(...))
```

现在可以写：

```java
authClient.register("zading", "123456")
```

这让测试更像业务描述：

```text
注册用户
登录用户
拿 token
```

而不是每次都陷入 JSON 拼接细节。

## 第十步：为什么 AuthTestClient 返回 ResultActions？

例如：

```java
public ResultActions register(String username, String password) throws Exception
```

它没有直接断言状态码，而是把 `ResultActions` 返回出去。

这样调用方可以自己决定期望：

注册成功时：

```java
authClient.register("zading", "123456")
        .andExpect(status().isCreated());
```

重复用户名时：

```java
authClient.register("zading", "abcdef")
        .andExpect(status().isBadRequest());
```

同一个 helper 可以支持成功和失败场景。

这是测试辅助类设计里很实用的技巧：

```text
helper 负责发请求
测试方法负责断言行为
```

## 第十一步：TodoTestClient 为什么存在？

文件：

```text
src/test/java/com/zading/todoapi/support/TodoTestClient.java
```

它封装 Todo 创建和读取 id：

```java
public ResultActions create(String token, Map<String, Object> body)
public Long createAndReadId(String token, String title)
public Long readId(MvcResult result)
```

为什么 `readId` 很常用？

因为创建 Todo 后，后续更新、删除、查询都需要真实 id。

测试不应该假设：

```text
新建的 Todo id 一定是 1
```

正确做法是：

```text
从创建响应里读取真实 id
```

这延续了第六周学过的测试原则：

```text
测试应该验证行为，不应该依赖偶然的数据库状态。
```

## 第十二步：测试辅助类是不是越多越好？

不是。

测试辅助类有一个风险：

```text
过度封装后，测试反而看不懂。
```

比如如果把所有断言都藏进 helper：

```java
todoClient.createAndAssertEverything(...)
```

测试方法可能会变得很短，但读的人不知道到底验证了什么。

所以当前项目采用克制封装：

```text
helper 封装重复请求准备
测试方法保留关键断言
```

这样既减少重复，又不牺牲可读性。

## 第十三步：CI 是什么？

CI 是 Continuous Integration，持续集成。

简单理解：

```text
把本地手动执行的检查，变成代码提交后的自动检查。
```

你本地现在会手动跑：

```bash
mvn test
```

CI 会在 GitHub 上自动跑：

```text
checkout code
setup JDK 21
mvn test
mvn package -DskipTests
```

这样每次 push 或 pull request 都能自动确认：

```text
代码能编译
测试能通过
应用能打包
```

## 第十四步：ci.yml 怎么读？

文件：

```text
.github/workflows/ci.yml
```

核心内容：

```yaml
name: Java Todo API CI

on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main
```

意思是：

```text
push 到 main 时运行
给 main 提 PR 时运行
```

再看：

```yaml
jobs:
  test:
    name: Test with JDK 21
    runs-on: ubuntu-latest
```

意思是：

```text
定义一个叫 test 的任务
在 GitHub 提供的 Ubuntu 机器上运行
```

步骤：

```yaml
- name: Checkout repository
  uses: actions/checkout@v4
```

拉取仓库代码。

```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: "21"
    cache: maven
```

安装 JDK 21，并缓存 Maven 依赖。

```yaml
- name: Run tests
  run: mvn test
```

运行测试。

```yaml
- name: Build package
  run: mvn package -DskipTests
```

打包应用，但跳过重复测试。

## 第十五步：为什么 CI 里 package 要用 `-DskipTests`？

因为前一步已经执行了：

```bash
mvn test
```

如果后面再执行：

```bash
mvn package
```

Maven 默认会再次跑测试。

为了避免重复，使用：

```bash
mvn package -DskipTests
```

意思是：

```text
只验证能否打包，不重复执行测试。
```

## 第十六步：本周为什么不引入 Testcontainers？

Testcontainers 很适合后端集成测试。

它可以在测试时自动启动 PostgreSQL 容器：

```text
测试开始
  -> 启动 PostgreSQL 容器
  -> 应用连接真实 PostgreSQL
  -> 跑测试
  -> 测试结束销毁容器
```

但你当前本机没有 Docker。

如果现在引入 Testcontainers，测试会因为缺少 Docker 而跑不起来。

所以第十周先做：

```text
测试结构重构
CI 准备
Testcontainers 概念学习
```

等以后有 Docker，再单独加：

```text
PostgresIntegrationTests
```

这叫循序渐进，不让工具链反过来卡住学习。

## 本周复盘问题

1. 为什么要拆分 `TodoApiApplicationTests`？
2. 冒烟测试验证的是什么？
3. 为什么认证测试和 Todo 测试要分开？
4. `AbstractApiTest` 解决了什么重复问题？
5. 为什么清理数据时要先删 Todo，再删 User？
6. `AuthTestClient` 为什么返回 `ResultActions`？
7. 为什么测试辅助类不能过度封装？
8. CI 解决了什么问题？
9. 为什么 CI 要设置 JDK 21？
10. 为什么本周暂时不引入 Testcontainers？

## 参考答案

### 1. 为什么要拆分 `TodoApiApplicationTests`？

因为项目功能越来越多，一个大测试类会变得难读、难维护。

拆分后，每个测试类只关心一类行为：

```text
AuthApiTests     认证
TodoApiTests     Todo 业务
OpenApiTests     接口文档
SmokeTests       基础健康检查
```

### 2. 冒烟测试验证的是什么？

冒烟测试验证项目最基础链路是否可用。

当前项目验证：

```text
GET /hello 返回 Hello Spring Boot
```

它不是完整业务测试，而是快速确认应用没有完全坏掉。

### 3. 为什么认证测试和 Todo 测试要分开？

因为它们属于不同功能边界。

认证测试关心：

```text
注册
登录
密码错误
重复用户名
```

Todo 测试关心：

```text
创建任务
查询任务
修改任务
删除任务
用户数据隔离
```

拆开后定位问题更快。

### 4. `AbstractApiTest` 解决了什么重复问题？

它集中处理所有 API 测试都需要的配置：

```text
SpringBootTest
MockMvc
test profile
数据清理
测试辅助客户端
```

这样每个测试类不用重复写相同配置。

### 5. 为什么清理数据时要先删 Todo，再删 User？

因为 Todo 通过 `user_id` 外键关联 User。

如果先删 User，数据库可能发现还有 Todo 引用它，删除失败。

所以顺序是：

```text
先删子表 Todo
再删父表 User
```

### 6. `AuthTestClient` 为什么返回 `ResultActions`？

因为同一个请求可能用于不同测试场景。

注册可能成功：

```java
.andExpect(status().isCreated())
```

也可能因为重复用户名失败：

```java
.andExpect(status().isBadRequest())
```

返回 `ResultActions` 可以让测试方法自己决定断言。

### 7. 为什么测试辅助类不能过度封装？

因为测试最重要的是可读性。

如果 helper 把所有请求和断言都藏起来，测试看起来短了，但读者不知道到底验证了什么。

所以当前策略是：

```text
helper 封装重复准备
测试保留关键断言
```

### 8. CI 解决了什么问题？

CI 把本地手动检查变成自动检查。

每次 push 或 PR 时自动执行：

```text
mvn test
mvn package -DskipTests
```

这样可以更早发现编译失败和测试失败。

### 9. 为什么 CI 要设置 JDK 21？

因为项目使用 Java 21。

如果 CI 使用 Java 17 或 Java 11，可能编译失败，或者和本地环境不一致。

CI 环境应该和项目要求保持一致。

### 10. 为什么本周暂时不引入 Testcontainers？

因为 Testcontainers 依赖 Docker。

你当前本机没有 Docker。

如果现在引入，会导致本地测试无法运行。

所以先学习概念，等 Docker 准备好后再加入真实 PostgreSQL 集成测试。

## 更多复盘问题

1. `@SpringBootTest` 和 MockMvc 搭配测试的是什么层级？
2. 为什么测试代码也需要重构？
3. 为什么测试 helper 不应该放到 `src/main/java`？
4. 为什么 `support` 包适合放测试辅助类？
5. `mvn test` 和 `mvn package` 的关系是什么？
6. 为什么 CI 要缓存 Maven 依赖？
7. 什么情况下应该新增一个测试类，而不是继续往旧测试类里加方法？
8. H2 测试和 PostgreSQL 测试分别能发现什么问题？
9. 为什么配置类也值得测试？
10. 如果一个测试失败了，你会按什么顺序排查？

## 更多复盘问题参考答案

### 1. `@SpringBootTest` 和 MockMvc 搭配测试的是什么层级？

它测试的是接近完整应用的接口行为。

Spring Boot 会启动应用上下文，MockMvc 模拟 HTTP 请求进入 Controller。

所以它不是纯单元测试，而是偏集成的接口测试。

### 2. 为什么测试代码也需要重构？

因为测试代码也会长期维护。

如果测试代码重复、混乱、难读，后面改业务时会不敢改测试，甚至开始忽略测试。

好的测试结构能提升维护信心。

### 3. 为什么测试 helper 不应该放到 `src/main/java`？

因为它只服务测试，不属于正式应用代码。

放到 `src/test/java` 可以避免被打包进正式应用。

### 4. 为什么 `support` 包适合放测试辅助类？

`support` 表示“支持测试运行的工具代码”。

它不是一个业务测试类，而是被多个测试类复用的辅助层。

### 5. `mvn test` 和 `mvn package` 的关系是什么？

`mvn test` 只跑测试。

`mvn package` 会编译、测试，并打包应用。

如果前面已经跑过测试，后面可以用：

```bash
mvn package -DskipTests
```

避免重复。

### 6. 为什么 CI 要缓存 Maven 依赖？

因为 Maven 第一次会下载很多依赖。

缓存后，后续 CI 运行可以复用依赖，加快速度。

### 7. 什么情况下应该新增一个测试类，而不是继续往旧测试类里加方法？

当测试关注点已经变成另一个功能区域时，就应该新增测试类。

例如：

```text
认证相关 -> AuthApiTests
Todo 相关 -> TodoApiTests
文档相关 -> OpenApiTests
```

### 8. H2 测试和 PostgreSQL 测试分别能发现什么问题？

H2 测试能快速验证大部分 JPA、Controller、Service 行为。

PostgreSQL 测试能发现真实数据库兼容问题，例如 SQL 方言、字段类型、迁移脚本差异。

### 9. 为什么配置类也值得测试？

因为配置错误也会导致功能不可用。

例如 Swagger 没有放行，就算业务代码没问题，前端也看不到接口文档。

### 10. 如果一个测试失败了，你会按什么顺序排查？

可以按这个顺序：

```text
1. 看失败测试名，判断功能区域
2. 看期望值和实际值
3. 看响应状态码和响应 JSON
4. 看最近改动的 Controller / Service / Repository / 配置
5. 必要时单独运行这个测试
```

先定位行为差异，再看代码原因。
