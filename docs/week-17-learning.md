# 第十七周学习说明：定时任务和后台批处理

第十七周的目标是：让系统具备“按时间自动执行任务”的能力。

前面我们已经学过三种触发方式：

```text
接口请求：用户触发
异步事件：业务动作触发
定时任务：时间触发
```

这周实现的是：

```text
每天自动扫描已经过期但还没有完成的 Todo，并记录一条 OVERDUE 操作日志。
```

一句话：

```text
第十七周学的是：让系统从“被用户调用”进化到“自己按时间干活”。
```

## 本周最终改动总览

| 改动 | 文件 | 目的 |
|---|---|---|
| 开启定时任务能力 | `SchedulingConfig.java` | 使用 `@EnableScheduling` 开启 Spring 定时任务 |
| 新增定时任务入口 | `TodoOverdueJob.java` | 按 cron 表达式每天触发过期扫描 |
| 新增过期扫描服务 | `TodoOverdueService.java` | 分页扫描过期 Todo，并发布日志事件 |
| 新增操作类型 | `TodoAction.java` | 增加 `OVERDUE` 操作日志类型 |
| 新增 Repository 查询 | `TodoRepository.java` | 查询已过期、未完成、未删除的 Todo |
| 新增日志去重查询 | `TodoActionLogRepository.java` | 判断某个 Todo 是否已经记录过 OVERDUE |
| 新增任务配置 | `application.properties` | 配置开关、cron、时区、批量大小 |
| 测试环境关闭定时任务 | `application-test.properties` | 避免测试时后台任务自动运行 |
| 补充测试 | `TodoApiTests.java` | 验证过期扫描只记录符合条件的 Todo |
| 更新 README | `README.md` | 补充工程化说明 |

## 第十六周和第十七周有什么关系？

第十六周学的是事件和异步：

```text
业务发生后，发布事件，异步处理附属任务。
```

第十七周学的是定时任务：

```text
到了某个时间点，系统主动执行后台逻辑。
```

这周没有丢掉第十六周的能力。

过期扫描发现 Todo 已经过期后，并不是直接写日志，而是继续复用事件机制：

```text
TodoOverdueService
  -> 发布 TodoActionLogEvent
  -> TodoActionLogEventListener 异步监听
  -> TodoActionLogService 写入日志
```

这说明一个工程化原则：

```text
新功能尽量复用已有稳定能力，而不是复制一套逻辑。
```

## 当前执行链路

完整链路是：

```text
系统时间到达每天 09:00
  -> TodoOverdueJob.scanOverdueTodos()
  -> TodoOverdueService.recordOverdueTodos()
  -> TodoRepository 分页查询过期 Todo
  -> 判断是否已经有 OVERDUE 日志
  -> 发布 TodoActionLogEvent
  -> 清理 todoLogs 缓存
  -> 事务提交
  -> TodoActionLogEventListener 异步处理事件
  -> TodoActionLogService 写入 todo_action_logs 表
```

用户没有发请求，系统自己会做这件事。

这就是后台任务。

## 第一步：SchedulingConfig 是什么？

文件：

```text
src/main/java/com/zading/todoapi/config/SchedulingConfig.java
```

代码：

```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

`@Configuration` 表示：

```text
这是一个 Spring 配置类。
```

`@EnableScheduling` 表示：

```text
开启 Spring 定时任务能力。
```

没有 `@EnableScheduling`，即使你写了 `@Scheduled`，定时任务也不会自动执行。

这和第十六周的 `@EnableAsync` 很像：

| 注解 | 作用 |
|---|---|
| `@EnableAsync` | 开启异步方法 |
| `@EnableScheduling` | 开启定时任务 |

## 第二步：TodoOverdueJob 是什么？

文件：

```text
src/main/java/com/zading/todoapi/job/TodoOverdueJob.java
```

这个类是定时任务入口。

它不负责复杂业务，只负责：

```text
到了时间
调用 TodoOverdueService
记录一条任务执行日志
```

核心代码：

```java
@Component
@ConditionalOnProperty(prefix = "app.todo.overdue-job", name = "enabled", havingValue = "true")
public class TodoOverdueJob {
}
```

`@Component` 表示：

```text
交给 Spring 管理。
```

`@ConditionalOnProperty` 表示：

```text
只有配置 app.todo.overdue-job.enabled=true 时，这个任务 Bean 才会生效。
```

为什么要加开关？

因为后台任务不是所有环境都应该自动跑。

比如：

```text
本地开发可以开
测试环境一般关
生产环境按实际需要开
```

这就是工程化里的“可控”。

## 第三步：@Scheduled 是什么？

代码：

```java
@Scheduled(cron = "${app.todo.overdue-job.cron}", zone = "${app.todo.overdue-job.zone:Asia/Shanghai}")
public void scanOverdueTodos() {
}
```

`@Scheduled` 表示：

```text
这个方法由 Spring 按时间自动调用。
```

它不是 Controller。

它没有 HTTP 请求。

它是系统后台自己跑的。

## 第四步：cron 表达式是什么？

配置：

```properties
app.todo.overdue-job.cron=0 0 9 * * *
```

Spring 的 cron 通常是 6 位：

```text
秒 分 时 日 月 星期
```

所以：

```text
0 0 9 * * *
```

意思是：

```text
每天 09:00:00 执行。
```

拆开看：

| 位置 | 值 | 含义 |
|---|---|---|
| 秒 | `0` | 第 0 秒 |
| 分 | `0` | 第 0 分钟 |
| 时 | `9` | 上午 9 点 |
| 日 | `*` | 每天 |
| 月 | `*` | 每月 |
| 星期 | `*` | 每周任意一天 |

## 第五步：为什么要配置时区？

配置：

```properties
app.todo.overdue-job.zone=Asia/Shanghai
```

原因是：

```text
服务器可能不在中国时区。
```

如果不指定时区，定时任务可能按服务器默认时区执行。

比如你以为是北京时间 9 点，结果服务器用 UTC，就会差 8 小时。

所以任务配置里明确写：

```text
Asia/Shanghai
```

学习项目也要养成这个习惯。

## 第六步：TodoOverdueService 做什么？

文件：

```text
src/main/java/com/zading/todoapi/service/TodoOverdueService.java
```

它是本周真正的业务核心。

核心方法：

```java
@Transactional
public int recordOverdueTodos(LocalDate today, int pageSize) {
}
```

这个方法做几件事：

```text
1. 分页查询已过期 Todo
2. 跳过已经记录过 OVERDUE 的 Todo
3. 发布 OVERDUE 操作日志事件
4. 清理 todoLogs 缓存
5. 返回本次新记录的数量
```

为什么返回数量？

因为定时任务需要知道：

```text
这次到底处理了多少条数据。
```

以后写监控、日志、告警都会用到。

## 第七步：什么样的 Todo 算过期？

Repository 方法：

```java
Page<Todo> findByCompletedFalseAndDeletedFalseAndDueDateBefore(LocalDate dueDate, Pageable pageable);
```

方法名有点长，但它的意思非常清楚：

```text
completed = false
deleted = false
dueDate < today
```

也就是：

```text
未完成
未删除
截止日期早于今天
```

注意：

```text
dueDate 等于今天，不算过期。
```

今天截止的任务还可以今天完成。

## 第八步：为什么要分页扫描？

代码：

```java
Pageable pageable = PageRequest.of(0, normalizedPageSize, Sort.by("id").ascending());
```

如果一次性查出所有过期 Todo，数据量大时可能会：

```text
占用太多内存
SQL 查询太慢
任务执行太久
```

所以我们分页处理。

默认配置：

```properties
app.todo.overdue-job.page-size=50
```

意思是：

```text
每批最多处理 50 条。
```

这就是后台批处理的基本思想：

```text
大任务拆成小批次。
```

## 第九步：为什么要判断是否已经记录过 OVERDUE？

代码：

```java
private boolean hasOverdueLog(Todo todo) {
    return todoActionLogRepository.existsByTodoIdAndAction(todo.getId(), TodoAction.OVERDUE);
}
```

因为定时任务每天都会跑。

如果不判断，某个 Todo 过期 10 天，就会记录 10 条：

```text
Todo 已过期
Todo 已过期
Todo 已过期
...
```

这不是我们想要的。

当前规则是：

```text
一个 Todo 只记录一次 OVERDUE 日志。
```

以后如果你想做“每天提醒一次”，可以单独设计提醒表或提醒日志，而不是复用操作日志。

## 第十步：为什么过期扫描也发布事件？

代码：

```java
todoEventPublisher.publishActionLog(todo, todo.getUser(), TodoAction.OVERDUE, "Todo 已过期");
```

这里没有直接保存 `TodoActionLog`。

原因是：

```text
第十六周已经建立了操作日志事件机制。
```

所以第十七周复用它。

好处是：

```text
日志写入方式统一
仍然是事务提交后异步处理
以后换 Kafka 更容易
```

这就是“不要复制逻辑”的工程习惯。

## 第十一步：为什么要清理 todoLogs 缓存？

代码：

```java
private void evictTodoLogsCache(Todo todo) {
    Cache cache = cacheManager.getCache(CacheNames.TODO_LOGS);

    if (cache != null) {
        cache.evict(todo.getUser().getId() + ":" + todo.getId());
    }
}
```

第十四、十五周我们给 Todo 操作日志加了缓存。

如果用户之前查过日志，`todoLogs` 里可能已经缓存了旧结果。

现在定时任务新增了一条 `OVERDUE` 日志，如果不清缓存，用户可能继续看到旧日志。

所以发布过期日志事件时，需要清理对应 Todo 的日志缓存。

这说明一个重要点：

```text
只要写操作会改变被缓存的数据，就要考虑缓存失效。
```

## 第十二步：为什么测试环境关闭定时任务？

测试配置：

```properties
app.todo.overdue-job.enabled=false
```

原因是：

```text
测试应该可控。
```

如果测试过程中后台任务突然自动运行，可能会影响测试数据。

所以测试不等真实定时器。

测试方式是：

```text
直接调用 TodoOverdueService.recordOverdueTodos(...)
```

这样可以明确验证业务逻辑，又不会被时间影响。

## 第十三步：测试验证了什么？

测试方法：

```text
shouldRecordOverdueTodoActionLogByBatchJob
```

它准备了 5 类 Todo：

```text
1. 昨天截止，未完成，未删除 -> 应该记录 OVERDUE
2. 今天截止 -> 不算过期
3. 未来截止 -> 不算过期
4. 已完成但过期 -> 不处理
5. 已删除但过期 -> 不处理
```

然后调用：

```java
int recordedCount = todoOverdueService.recordOverdueTodos(LocalDate.now(), 2);
```

断言：

```text
本次只记录 1 条 OVERDUE
日志接口能查到 OVERDUE
重复执行不会重复记录
```

其中 `pageSize=2` 是故意的。

目的是让测试覆盖分页逻辑。

## 本周你需要真正掌握的点

### 1. 定时任务是什么？

定时任务是：

```text
系统按时间自动执行的方法。
```

### 2. `@EnableScheduling` 做什么？

开启 Spring 定时任务能力。

### 3. `@Scheduled` 做什么？

标记某个方法按固定时间规则自动执行。

### 4. cron 是什么？

cron 是一种描述时间规则的表达式。

例如：

```text
0 0 9 * * * = 每天 09:00:00
```

### 5. 为什么后台任务要有开关？

因为不同环境可能不希望任务自动运行，尤其是测试环境。

### 6. 为什么后台任务要分页？

避免一次性读取太多数据，降低内存和数据库压力。

### 7. 为什么过期任务只记录一次？

避免每天重复写同一类日志，导致日志污染。

### 8. 为什么还要复用事件？

因为操作日志写入已经通过事件异步处理，复用它可以保持架构一致。

## 本周复盘问题和参考答案

### 1. 第十七周最大的变化是什么？

新增 Spring Scheduling 定时任务，每天自动扫描过期 Todo，并记录 `OVERDUE` 操作日志。

### 2. `@EnableScheduling` 的作用是什么？

开启 Spring 的定时任务能力。没有它，`@Scheduled` 不会自动执行。

### 3. `@Scheduled` 和 Controller 有什么区别？

Controller 由 HTTP 请求触发；`@Scheduled` 由时间规则触发，不需要用户请求。

### 4. `0 0 9 * * *` 是什么意思？

每天 09:00:00 执行一次。

### 5. 为什么要设置 `zone=Asia/Shanghai`？

避免服务器默认时区不同导致任务执行时间和预期不一致。

### 6. 什么 Todo 会被记录为 OVERDUE？

未完成、未删除、并且 `dueDate` 早于今天的 Todo。

### 7. 今天截止的 Todo 算过期吗？

不算。当前规则是 `dueDate < today` 才算过期。

### 8. 为什么要分页扫描？

避免一次查询和处理太多数据，提升后台任务稳定性。

### 9. 为什么要判断是否已经有 OVERDUE 日志？

避免定时任务每天重复记录同一个 Todo 的过期日志。

### 10. 为什么过期扫描要清理 `todoLogs` 缓存？

因为新增 OVERDUE 日志会改变日志列表，如果不清缓存，用户可能看到旧日志。

### 11. 为什么测试不真的等每天 09:00 的定时任务？

测试要快速、稳定、可控。直接调用 Service 可以验证核心逻辑，不受时间影响。

### 12. 定时任务和异步事件怎么配合？

定时任务负责发现需要处理的数据；异步事件负责把附属动作解耦出去，例如写操作日志。

### 13. 第十七周最重要的工程化思想是什么？

后台任务要可配置、可分页、可测试，并且要复用已有业务能力。
