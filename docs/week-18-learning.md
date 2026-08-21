# 第十八周学习说明：Actuator 可观测性和后台任务状态查询

第十八周的目标是：让系统不只是“能运行”，还要“看得见运行状态”。

前面几周我们已经让项目具备了很多后端工程能力：

```text
第 14 周：缓存
第 15 周：Redis 外部缓存
第 16 周：异步事件
第 17 周：定时任务
```

这些能力加进来之后，系统就开始变得更像真实项目了。

但真实项目里还有一个很重要的问题：

```text
服务现在是否正常？
应用信息是什么？
JVM 运行指标怎么样？
后台任务上次有没有成功？
```

第十八周解决的就是这些问题。

一句话：

```text
第十八周学的是：给系统加“观察窗口”，让开发者可以知道服务和后台任务是否健康。
```

## 本周最终改动总览

| 改动 | 文件 | 目的 |
|---|---|---|
| 引入 Actuator | `pom.xml` | 获得 Spring Boot 内置健康检查、应用信息和运行指标端点 |
| 配置 Actuator 端点 | `application.properties` | 只开放 `health`、`info`、`metrics` |
| 默认关闭 Redis health | `application.properties` / `application-test.properties` | 本地没有 Redis 时，健康检查不被 Redis 影响 |
| Redis profile 开启 Redis health | `application-redis.properties` | 真正使用 Redis 时，再检查 Redis 连接 |
| 放行 Actuator 部分端点 | `SecurityConfig.java` | 允许未登录访问基础健康检查和指标入口 |
| 新增任务状态对象 | `TodoOverdueJobStatus.java` | 描述过期扫描任务最近一次执行结果 |
| 新增任务状态服务 | `TodoOverdueJobStatusService.java` | 在内存中保存最近一次任务状态 |
| 新增内部任务接口 | `InternalJobController.java` | 查询 Todo 过期扫描任务状态 |
| 改造过期扫描服务 | `TodoOverdueService.java` | 执行成功 / 失败时记录任务状态 |
| 补充 Actuator 测试 | `ActuatorTests.java` | 验证 health、info、metrics 可访问 |
| 补充任务状态测试 | `TodoApiTests.java` | 验证批处理执行后能查询到任务状态 |
| 更新 README | `README.md` | 补充工程化说明 |

## 第十八周和第十七周有什么关系？

第十七周做了“定时任务”：

```text
每天扫描过期 Todo
```

第十八周在这个基础上继续问：

```text
这个任务最近一次有没有执行？
执行成功了吗？
处理了多少条数据？
用了多长时间？
如果失败了，错误是什么？
```

所以第十八周不是凭空新增一个功能，而是在第十七周的后台任务上增加“可观察性”。

这就是工程化里常见的演进方式：

```text
先让功能跑起来
再让功能可观测
最后再让功能可告警、可追踪、可运维
```

## 什么是可观测性？

可观测性可以简单理解为：

```text
系统内部发生了什么，外部能不能看见。
```

如果系统只是能处理请求，但出了问题没人知道，那工程上是不够的。

常见的可观测性包括：

| 类型 | 说明 | 例子 |
|---|---|---|
| Health | 服务是否健康 | `/actuator/health` |
| Info | 应用基础信息 | `/actuator/info` |
| Metrics | 运行指标 | JVM 内存、HTTP 请求数量 |
| Logs | 日志 | 请求日志、异常日志 |
| Tracing | 链路追踪 | 一次请求经过哪些服务 |

本周先做前三个：

```text
Health
Info
Metrics
```

## 第一步：为什么要引入 Actuator？

文件：

```text
pom.xml
```

新增依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

`spring-boot-starter-actuator` 是 Spring Boot 官方提供的运维监控组件。

引入之后，Spring Boot 会自动提供很多端点，例如：

```text
/actuator/health
/actuator/info
/actuator/metrics
```

你可以把 Actuator 理解成：

```text
Spring Boot 应用自带的“体检入口”。
```

如果没有 Actuator，我们就需要自己写很多基础接口，例如：

```text
写一个 health 接口
写一个 info 接口
写 JVM 指标收集逻辑
写 HTTP 指标统计逻辑
```

这不划算，而且容易不标准。

工程项目里通常会优先使用成熟框架能力，而不是重复造基础轮子。

## 第二步：为什么只开放部分 Actuator 端点？

文件：

```text
src/main/resources/application.properties
```

新增配置：

```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=never
management.info.env.enabled=true
```

这几行的意思是：

```text
只通过 Web 暴露 health、info、metrics
健康检查不展示详细内部信息
允许 info 读取配置里的应用信息
```

为什么不直接开放所有 Actuator 端点？

因为 Actuator 里有些端点可能会暴露比较敏感的信息。

例如：

```text
环境变量
Bean 信息
配置属性
线程信息
日志级别修改入口
```

所以工程里常见做法是：

```text
默认最小开放
需要什么再开放什么
```

本项目目前只开放：

```text
health：判断服务是否活着
info：查看应用基础信息
metrics：查看基础运行指标
```

## 第三步：/actuator/health 是什么？

接口：

```http
GET /actuator/health
```

正常响应类似：

```json
{
  "status": "UP"
}
```

`UP` 表示 Spring Boot 判断当前应用是健康的。

它会检查一些基础组件。

例如项目里有数据库时，它会检查数据库连接。

如果项目启用了 Redis health，它也会检查 Redis。

## 第四步：为什么默认关闭 Redis health？

本项目现在支持 Redis profile，但你的电脑不一定安装 Redis。

我们之前已经把 Redis 做成了可选能力：

```text
默认：本地内存缓存
启用 redis profile：使用 Redis 缓存
```

但是引入 Actuator 后，Spring Boot 可能发现项目里有 Redis 依赖，于是尝试检查 Redis 连接。

如果本机没有 Redis，健康检查就可能变成：

```text
DOWN
```

这会造成一个误判：

```text
应用本身能跑，但 health 因为 Redis 未安装而失败。
```

所以默认配置里加了：

```properties
management.health.redis.enabled=false
```

测试环境也加了同样配置：

```properties
management.health.redis.enabled=false
```

意思是：

```text
默认环境和测试环境不检查 Redis。
```

而在 Redis profile 中加了：

```properties
management.health.redis.enabled=true
```

意思是：

```text
只有明确启用 Redis 时，才把 Redis 纳入健康检查。
```

这个设计很重要。

它体现了一个工程原则：

```text
可选组件，不应该影响默认运行路径。
```

## 第五步：为什么要在 SecurityConfig 里放行 Actuator？

文件：

```text
src/main/java/com/zading/todoapi/security/SecurityConfig.java
```

新增放行：

```java
"/actuator/health",
"/actuator/info",
"/actuator/metrics",
"/actuator/metrics/**",
```

因为项目现在启用了 Spring Security。

如果不放行，访问：

```http
GET /actuator/health
```

也会被拦住，要求登录。

健康检查通常会被浏览器、监控系统、部署平台调用。

这些调用不一定适合走业务登录流程。

所以我们放行了基础可观测性端点。

但注意：

```text
不是所有 Actuator 端点都应该公开。
```

这也是为什么前面只暴露了 `health`、`info`、`metrics`。

## 第六步：为什么新增 TodoOverdueJobStatus？

文件：

```text
src/main/java/com/zading/todoapi/job/TodoOverdueJobStatus.java
```

代码：

```java
public record TodoOverdueJobStatus(
        String jobName,
        LocalDate lastRunDate,
        LocalDateTime lastRunAt,
        Boolean lastSuccess,
        Integer lastProcessedCount,
        Long lastDurationMs,
        String lastErrorMessage
) {
}
```

这是一个 Java `record`。

它适合表达“只存数据、不写复杂行为”的对象。

这里的每个字段代表任务最近一次执行结果：

| 字段 | 含义 |
|---|---|
| `jobName` | 任务名称 |
| `lastRunDate` | 这次任务按哪个业务日期扫描 |
| `lastRunAt` | 这次任务实际执行时间 |
| `lastSuccess` | 是否成功 |
| `lastProcessedCount` | 处理了多少条 Todo |
| `lastDurationMs` | 执行耗时，单位毫秒 |
| `lastErrorMessage` | 失败原因 |

为什么字段类型有些用了包装类型，例如 `Boolean`、`Integer`、`Long`，而不是 `boolean`、`int`、`long`？

因为任务可能还没执行过。

没执行过时，我们希望这些字段可以是：

```text
null
```

如果用基本类型，默认值会是：

```text
false
0
0
```

这会让人误以为任务执行过，只是失败了或处理了 0 条。

所以这里用包装类型，表达：

```text
还没有值
```

## 第七步：neverRun() 是什么？

代码：

```java
public static TodoOverdueJobStatus neverRun() {
    return new TodoOverdueJobStatus(
            "todo-overdue",
            null,
            null,
            null,
            null,
            null,
            null
    );
}
```

`neverRun()` 表示：

```text
任务还没有执行过。
```

为什么要写这个方法？

因为这样比在别的地方手写一堆 `null` 更清晰。

对比一下。

不推荐：

```java
new TodoOverdueJobStatus("todo-overdue", null, null, null, null, null, null)
```

更清晰：

```java
TodoOverdueJobStatus.neverRun()
```

读代码的人一眼就知道：

```text
这是初始状态。
```

## 第八步：TodoOverdueJobStatusService 是什么？

文件：

```text
src/main/java/com/zading/todoapi/service/TodoOverdueJobStatusService.java
```

核心代码：

```java
@Service
public class TodoOverdueJobStatusService {
    private final AtomicReference<TodoOverdueJobStatus> status =
            new AtomicReference<>(TodoOverdueJobStatus.neverRun());
}
```

这个 Service 的职责很单纯：

```text
保存 Todo 过期扫描任务最近一次执行状态。
```

为什么放在 Service 里？

因为 Controller 不应该自己管理状态。

Controller 负责 HTTP 输入输出。

Service 负责业务状态和业务逻辑。

这符合我们一直在练习的分层思想：

```text
Controller：接请求
Service：做业务
Repository：查数据
```

## 第九步：AtomicReference 是什么？

代码：

```java
private final AtomicReference<TodoOverdueJobStatus> status =
        new AtomicReference<>(TodoOverdueJobStatus.neverRun());
```

`AtomicReference<T>` 可以安全地保存和替换一个对象引用。

你可以先把它简单理解成：

```text
线程安全版本的“变量盒子”。
```

为什么这里不用普通字段？

比如：

```java
private TodoOverdueJobStatus status;
```

因为后台任务和 HTTP 查询可能发生在不同线程：

```text
定时任务线程：更新 status
HTTP 请求线程：读取 status
```

如果多个线程同时读写同一个变量，就需要考虑线程安全。

`AtomicReference` 可以保证：

```text
读取时拿到的是一个完整对象
更新时一次性替换整个对象
```

这里我们没有修改对象内部字段，而是直接替换整个 `TodoOverdueJobStatus`。

这和 `record` 搭配很合适：

```text
record 不可变
AtomicReference 负责整体替换
```

## 第十步：recordSuccess 做了什么？

代码：

```java
public void recordSuccess(LocalDate runDate, int processedCount, long durationMs) {
    status.set(new TodoOverdueJobStatus(
            "todo-overdue",
            runDate,
            LocalDateTime.now(),
            true,
            processedCount,
            durationMs,
            null
    ));
}
```

任务成功时，保存：

```text
业务日期
当前执行时间
成功标记
处理数量
耗时
错误信息 null
```

`status.set(...)` 表示：

```text
把旧状态整体替换成新状态。
```

## 第十一步：recordFailure 做了什么？

代码：

```java
public void recordFailure(LocalDate runDate, long durationMs, Exception exception) {
    status.set(new TodoOverdueJobStatus(
            "todo-overdue",
            runDate,
            LocalDateTime.now(),
            false,
            0,
            durationMs,
            exception.getMessage()
    ));
}
```

任务失败时，保存：

```text
业务日期
当前执行时间
失败标记
处理数量 0
耗时
异常消息
```

注意这里记录失败后，并没有把异常吞掉。

异常会继续往外抛。

这很重要：

```text
记录状态是为了可观测
继续抛异常是为了让调用方知道任务失败了
```

不要为了“看起来不报错”而悄悄吃掉异常。

这会让问题更难排查。

## 第十二步：TodoOverdueService 改了什么？

文件：

```text
src/main/java/com/zading/todoapi/service/TodoOverdueService.java
```

这次把原来的 `recordOverdueTodos` 拆成两层：

```java
@Transactional
public int recordOverdueTodos(LocalDate today, int pageSize) {
    long start = System.nanoTime();

    try {
        int recordedCount = doRecordOverdueTodos(today, pageSize);
        todoOverdueJobStatusService.recordSuccess(today, recordedCount, elapsedMs(start));
        return recordedCount;
    } catch (Exception ex) {
        todoOverdueJobStatusService.recordFailure(today, elapsedMs(start), ex);
        throw ex;
    }
}
```

外层负责：

```text
计时
记录成功状态
记录失败状态
保持事务边界
```

内层负责：

```java
private int doRecordOverdueTodos(LocalDate today, int pageSize) {
    // 原来的扫描逻辑
}
```

为什么要拆？

因为如果把所有逻辑都堆在一个方法里，方法会越来越长：

```text
分页扫描
去重判断
发布事件
清理缓存
计算耗时
记录成功
记录失败
异常处理
```

拆成两层后，职责更清楚：

```text
recordOverdueTodos：任务执行外壳
doRecordOverdueTodos：真正业务扫描
```

这是后端项目里很常见的重构手法。

## 第十三步：为什么使用 System.nanoTime()？

代码：

```java
long start = System.nanoTime();
```

计算耗时时，更推荐使用 `System.nanoTime()`，而不是 `System.currentTimeMillis()`。

简单理解：

| 方法 | 适合做什么 |
|---|---|
| `System.currentTimeMillis()` | 获取当前时间戳 |
| `System.nanoTime()` | 计算一段代码运行耗时 |

所以这里用：

```java
private long elapsedMs(long start) {
    return (System.nanoTime() - start) / 1_000_000;
}
```

把纳秒转换成毫秒。

## 第十四步：InternalJobController 是什么？

文件：

```text
src/main/java/com/zading/todoapi/controller/InternalJobController.java
```

代码：

```java
@RestController
@RequestMapping("/api/internal/jobs")
public class InternalJobController {
}
```

这个 Controller 用来提供内部管理接口。

为什么路径里有 `/internal`？

因为这个接口不是普通用户功能。

普通 Todo 用户关心的是：

```text
创建 Todo
修改 Todo
查询 Todo
```

开发者或运维人员关心的是：

```text
后台任务有没有正常执行
```

所以放在：

```text
/api/internal/jobs/todo-overdue
```

这样从路径上就能看出：

```text
这是内部管理类接口。
```

## 第十五步：为什么这个内部接口仍然需要登录？

`SecurityConfig` 里只放行了：

```text
/actuator/health
/actuator/info
/actuator/metrics
```

没有放行：

```text
/api/internal/jobs/todo-overdue
```

所以查询任务状态仍然需要：

```http
Authorization: Bearer <token>
```

为什么？

因为任务状态属于系统内部信息。

虽然它不是特别敏感，但也不应该随便公开。

这一点也是工程习惯：

```text
基础健康检查可以公开
内部业务运维接口需要认证
```

## 第十六步：ActuatorTests 测了什么？

文件：

```text
src/test/java/com/zading/todoapi/ActuatorTests.java
```

测试内容：

```java
mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
```

这验证：

```text
health 端点不用登录也能访问
应用健康状态是 UP
```

然后测试：

```java
mockMvc.perform(get("/actuator/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.app.name").value("java-todo-api"));
```

这验证：

```text
info 端点能返回应用信息
```

最后测试：

```java
mockMvc.perform(get("/actuator/metrics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.names", hasItem("jvm.memory.used")));
```

这验证：

```text
metrics 端点可访问
并且包含 JVM 内存指标
```

## 第十七步：TodoApiTests 新增了什么？

在原来的过期扫描测试里，我们已经验证：

```text
过期 Todo 会产生 OVERDUE 操作日志
```

本周继续补充：

```java
mockMvc.perform(get("/api/internal/jobs/todo-overdue")
                .header("Authorization", authClient.bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.jobName").value("todo-overdue"))
        .andExpect(jsonPath("$.data.lastSuccess").value(true))
        .andExpect(jsonPath("$.data.lastProcessedCount").value(1));
```

这验证：

```text
过期扫描任务执行后
任务状态会被正确记录
内部接口可以查询到最近一次执行结果
```

这类测试不只是测接口是否能访问。

它测试的是一条完整链路：

```text
执行批处理
  -> 写操作日志
  -> 更新任务状态
  -> 通过内部接口读取状态
```

## 本周新增接口

### 1. 应用健康检查

```http
GET /actuator/health
```

响应示例：

```json
{
  "status": "UP"
}
```

### 2. 应用信息

```http
GET /actuator/info
```

响应示例：

```json
{
  "app": {
    "name": "java-todo-api",
    "version": "1.0.0",
    "description": "Spring Boot Todo REST API",
    "java-version": "21"
  }
}
```

### 3. 运行指标列表

```http
GET /actuator/metrics
```

响应中会包含很多指标名称，例如：

```text
jvm.memory.used
http.server.requests
process.uptime
```

### 4. Todo 过期扫描任务状态

```http
GET /api/internal/jobs/todo-overdue
Authorization: Bearer <token>
```

响应示例：

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {
    "jobName": "todo-overdue",
    "lastRunDate": "2026-08-21",
    "lastRunAt": "2026-08-21T09:00:00.123456",
    "lastSuccess": true,
    "lastProcessedCount": 1,
    "lastDurationMs": 12,
    "lastErrorMessage": null
  },
  "path": null
}
```

## 本周你需要重点理解的代码关系

```text
Actuator
  -> 提供系统级健康检查和指标

SecurityConfig
  -> 决定哪些端点可以公开访问

TodoOverdueService
  -> 执行过期扫描
  -> 成功 / 失败时记录任务状态

TodoOverdueJobStatusService
  -> 保存最近一次任务执行状态

InternalJobController
  -> 把任务状态暴露成 HTTP 接口
```

## 和前端开发经验怎么类比？

你是前端开发，可以这样类比：

| Java 后端概念 | 前端里的类似概念 |
|---|---|
| `/actuator/health` | 前端项目的部署健康检查 |
| `/actuator/info` | 构建版本、commit 信息、环境信息 |
| `/actuator/metrics` | 性能监控数据 |
| `TodoOverdueJobStatus` | 页面里展示的任务状态数据模型 |
| `AtomicReference` | 安全保存最新状态的 store |
| `InternalJobController` | 给管理后台用的内部 API |

有点像你做前端时，不只是把页面写出来，还会关心：

```text
页面有没有正常加载？
接口有没有报错？
当前版本是什么？
后台任务有没有同步成功？
```

后端工程也是一样。

## 本周复盘问题和参考答案

### 1. Actuator 的作用是什么？

Actuator 用来给 Spring Boot 应用提供健康检查、应用信息、运行指标等运维接口。

它让我们可以从外部观察应用是否正常运行。

### 2. 为什么不能随便开放所有 Actuator 端点？

因为有些端点可能暴露环境变量、配置、Bean、线程等内部信息。

工程上应该最小化开放，只开放当前确实需要的端点。

### 3. `/actuator/health` 和 `/hello` 有什么区别？

`/hello` 是我们自己写的普通接口，只能说明 Controller 能响应。

`/actuator/health` 是 Spring Boot 的健康检查接口，它可以综合检查数据库、Redis 等组件状态。

### 4. 为什么默认关闭 Redis health？

因为 Redis 是当前项目的可选组件。

如果本地没有安装 Redis，但 health 强制检查 Redis，就会导致应用健康检查失败。

默认关闭 Redis health，可以保证不启用 Redis profile 时项目正常运行。

### 5. 为什么 Redis profile 里又打开 Redis health？

因为一旦启用 Redis profile，就说明应用真的依赖 Redis。

这时 Redis 连接失败应该反映到健康检查里。

### 6. `record` 适合用在什么场景？

适合用来表达简单的数据载体。

例如请求响应对象、状态对象、只读配置对象。

### 7. 为什么 `TodoOverdueJobStatus` 里的部分字段用包装类型？

因为任务还没执行过时，这些字段需要表达“没有值”。

包装类型可以是 `null`，基本类型不能。

### 8. `AtomicReference` 的作用是什么？

它是一个线程安全的对象引用容器。

这里用它保存后台任务最近一次状态，避免定时任务线程和 HTTP 请求线程同时读写普通字段造成线程安全问题。

### 9. 为什么记录失败后还要继续 `throw ex`？

因为记录失败状态只是为了可观测。

异常继续抛出，才能让调用方、日志或测试知道任务真的失败了。

如果吞掉异常，问题会被隐藏。

### 10. 为什么内部任务状态接口需要登录？

因为它属于系统内部运维信息，不是公开业务接口。

基础健康检查可以公开，但内部业务状态应该受认证保护。

### 11. 为什么要测试 Actuator？

因为 Spring Security、Actuator 配置、Redis health 都可能影响端点可访问性。

测试可以保证健康检查、应用信息、指标端点在默认环境下可用。

### 12. 本周代码最重要的工程思想是什么？

功能上线后，要能观察它是否正常工作。

工程不是只写业务逻辑，还要考虑：

```text
能否监控
能否排查
能否确认后台任务状态
能否在缺少可选组件时保持默认可运行
```

## 本周建议练习

你可以按下面顺序自己操作一遍：

1. 启动项目。
2. 访问 `GET /actuator/health`，确认返回 `UP`。
3. 访问 `GET /actuator/info`，查看应用信息。
4. 访问 `GET /actuator/metrics`，看看有哪些指标。
5. 注册并登录一个用户。
6. 携带 Token 访问 `GET /api/internal/jobs/todo-overdue`。
7. 手动在测试里调用 `todoOverdueService.recordOverdueTodos(...)`。
8. 再查询任务状态，看状态是否发生变化。

## 本周小结

第十八周完成后，项目多了两类能力：

```text
系统级可观测性：Actuator health / info / metrics
业务级可观测性：Todo 过期扫描任务状态查询
```

这一步很关键。

因为从真实项目角度看，一个后台任务不是“写完就结束”。

你还需要知道：

```text
它有没有跑
跑得是否成功
处理了多少数据
失败时错误是什么
```

这就是从“会写功能”继续往“会做工程”走的一步。
