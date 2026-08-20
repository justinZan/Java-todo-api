# 第十六周学习说明：Spring Event 和异步处理

第十六周的目标是：把 Todo 主业务和操作日志写入解耦。

之前的代码是：

```text
TodoService 完成 Todo 主业务
TodoService 直接保存操作日志
```

现在改成：

```text
TodoService 完成 Todo 主业务
TodoService 发布事件
事务提交后，Listener 异步保存操作日志
```

一句话：

```text
第十六周学的是：主业务先做完，附属业务通过事件去处理。
```

## 本周最终改动总览

| 改动 | 文件 | 目的 |
|---|---|---|
| 新增异步配置 | `AsyncConfig.java` | 开启 `@Async`，配置 Todo 专用线程池 |
| 新增事件对象 | `TodoActionLogEvent.java` | 表达“需要记录一条 Todo 操作日志”这件事 |
| 新增事件发布器 | `TodoEventPublisher.java` | 封装 Spring 的 `ApplicationEventPublisher` |
| 新增事件监听器 | `TodoActionLogEventListener.java` | 监听事件，并在事务提交后异步处理 |
| 新增日志服务 | `TodoActionLogService.java` | 真正把操作日志写入数据库 |
| 改造 TodoService | `TodoService.java` | 从“直接写日志”改成“发布日志事件” |
| 补充 Repository 方法 | `TodoActionLogRepository.java` | 支持测试等待异步日志写入 |
| 改造测试等待逻辑 | `AbstractApiTest.java` / `TodoApiTests.java` | 因为日志异步写入，测试需要等待落库 |
| 更新 README | `README.md` | 补充事件驱动和异步说明 |

## 第十五周和第十六周有什么关系？

第十五周学的是 Redis 缓存：

```text
提高读性能
减少重复查询数据库
```

第十六周学的是事件和异步：

```text
降低主流程耦合
让附属任务不阻塞主业务
```

对比一下：

| 对比项 | 第十五周 | 第十六周 |
|---|---|---|
| 主题 | 缓存 | 事件驱动 / 异步 |
| 解决的问题 | 重复读取太多 | 主业务和附属业务耦合 |
| 核心注解 | `@Cacheable` / `@CacheEvict` | `@Async` / `@TransactionalEventListener` |
| 项目里的例子 | Todo 详情和日志缓存 | Todo 操作日志异步写入 |
| 工程思想 | 读多的数据可以缓存 | 非主线任务可以拆出去 |

## 当前代码链路

现在创建、修改、完成、删除、恢复 Todo 时，链路变成：

```text
HTTP 请求
  -> TodoController
  -> TodoService
  -> 保存 Todo 主数据
  -> 发布 TodoActionLogEvent
  -> TodoService 返回
  -> 数据库事务提交
  -> TodoActionLogEventListener 收到事件
  -> 异步线程执行
  -> TodoActionLogService 写入操作日志
```

你可以把它理解为：

```text
主线：把 Todo 改对
旁支：记录这次操作发生了什么
```

主线越干净，代码越容易维护。

## 第一步：AsyncConfig 是什么？

文件：

```text
src/main/java/com/zading/todoapi/config/AsyncConfig.java
```

核心代码：

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
}
```

`@Configuration` 表示：

```text
这是一个 Spring 配置类。
```

`@EnableAsync` 表示：

```text
开启 Spring 的异步方法能力。
```

没有 `@EnableAsync` 时，就算你在方法上写了 `@Async`，异步也不会真正生效。

### 线程池是什么？

代码：

```java
@Bean(name = TODO_TASK_EXECUTOR)
public ThreadPoolTaskExecutor todoTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("todo-async-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(5);
    executor.initialize();
    return executor;
}
```

这段代码创建了一个专门处理 Todo 异步任务的线程池。

参数解释：

| 参数 | 含义 |
|---|---|
| `corePoolSize=2` | 平时保留 2 个工作线程 |
| `maxPoolSize=4` | 忙的时候最多扩展到 4 个线程 |
| `queueCapacity=100` | 如果线程忙，最多排队 100 个任务 |
| `threadNamePrefix=todo-async-` | 线程名以前缀开头，方便看日志排查 |
| `waitForTasksToCompleteOnShutdown=true` | 应用关闭时尽量等异步任务完成 |
| `awaitTerminationSeconds=5` | 最多等待 5 秒 |

为什么不用默认线程池？

```text
默认线程池不够可控。
```

真实项目里，异步任务最好有明确的线程池。这样你可以控制并发数量、队列大小和线程名。

## 第二步：异步异常处理

代码：

```java
@Override
public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (Throwable ex, Method method, Object... params) ->
            log.error("异步任务执行失败，method={}, params={}", method.getName(), Arrays.toString(params), ex);
}
```

普通同步代码报错，会直接返回到调用方。

异步代码不一样。

因为异步方法已经跑到另一个线程里了：

```text
主线程已经返回
异步线程后面才执行
```

所以异步任务失败时，不能像普通接口那样直接把异常返回给用户。

这里的处理方式是：

```text
异步失败时写错误日志，方便排查。
```

注意：

```text
异步任务失败，不应该影响 Todo 创建、修改这类主业务已经成功的结果。
```

这也是异步任务的一个重要取舍。

## 第三步：TodoActionLogEvent 是什么？

文件：

```text
src/main/java/com/zading/todoapi/event/TodoActionLogEvent.java
```

代码：

```java
public record TodoActionLogEvent(
        Long todoId,
        Long userId,
        TodoAction action,
        String description
) {
}
```

这是 Java 的 `record`。

你可以先简单理解为：

```text
专门用来装数据的不可变对象。
```

这个事件表达的是：

```text
某个用户对某个 Todo 做了某个动作，需要记录一条日志。
```

字段含义：

| 字段 | 含义 |
|---|---|
| `todoId` | 哪个 Todo |
| `userId` | 哪个用户 |
| `action` | 做了什么动作，例如 CREATED / UPDATED |
| `description` | 日志描述，例如“创建 Todo” |

为什么事件里只放 id，不直接放 `Todo` 和 `AppUser` 对象？

因为事件会异步处理。

异步意味着：

```text
处理事件时，已经不在原来的事务和线程里。
```

直接传 JPA Entity 容易遇到懒加载、事务边界、对象状态不一致等问题。

所以更稳的做法是：

```text
事件里只放必要的 id 和简单字段。
```

## 第四步：record 里的校验

代码：

```java
public TodoActionLogEvent {
    Objects.requireNonNull(todoId, "todoId 不能为空");
    Objects.requireNonNull(userId, "userId 不能为空");
    Objects.requireNonNull(action, "action 不能为空");
    Objects.requireNonNull(description, "description 不能为空");
}
```

这是 `record` 的紧凑构造方法。

作用是：

```text
创建事件时，关键字段不能为 null。
```

为什么要提前校验？

因为如果事件缺少关键字段，后面异步处理时才报错，排查会更麻烦。

提前失败更清晰。

## 第五步：TodoEventPublisher 是什么？

文件：

```text
src/main/java/com/zading/todoapi/event/TodoEventPublisher.java
```

核心代码：

```java
@Component
public class TodoEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishActionLog(Todo todo, AppUser user, TodoAction action, String description) {
        applicationEventPublisher.publishEvent(
                new TodoActionLogEvent(todo.getId(), user.getId(), action, description)
        );
    }
}
```

`ApplicationEventPublisher` 是 Spring 提供的事件发布器。

这段代码做的事情是：

```text
把 Todo / User / Action 转成 TodoActionLogEvent
然后交给 Spring 发布出去
```

为什么不在 `TodoService` 里直接调用 `ApplicationEventPublisher`？

为了让 `TodoService` 更干净。

`TodoService` 只需要知道：

```text
我要发布一条 Todo 操作日志事件。
```

不需要关心 Spring 底层怎么发布事件。

这就是一层小封装，未来如果事件发布方式改成 Kafka，也更容易替换。

## 第六步：TodoService 改了什么？

文件：

```text
src/main/java/com/zading/todoapi/service/TodoService.java
```

之前：

```java
private void addActionLog(Todo todo, AppUser user, TodoAction action, String description) {
    todoActionLogRepository.save(new TodoActionLog(todo, user, action, description));
}
```

现在：

```java
private void addActionLog(Todo todo, AppUser user, TodoAction action, String description) {
    todoEventPublisher.publishActionLog(todo, user, action, description);
}
```

表面上只改了一行。

但架构意义很大。

之前 `TodoService` 同时负责：

```text
1. Todo 主业务
2. 操作日志写入
```

现在 `TodoService` 负责：

```text
1. Todo 主业务
2. 发布“需要记录日志”的事件
```

真正写日志的事情交给 Listener 和 `TodoActionLogService`。

这叫：

```text
解耦。
```

## 第七步：TodoActionLogEventListener 是什么？

文件：

```text
src/main/java/com/zading/todoapi/event/TodoActionLogEventListener.java
```

核心代码：

```java
@Component
public class TodoActionLogEventListener {
    private final TodoActionLogService todoActionLogService;

    @Async(AsyncConfig.TODO_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTodoActionLog(TodoActionLogEvent event) {
        todoActionLogService.record(event);
    }
}
```

这一段非常关键，我们拆开看。

### @Component

表示：

```text
这个类交给 Spring 管理。
```

只有成为 Spring Bean，Spring 才能发现这个 Listener。

### @TransactionalEventListener

表示：

```text
监听事务事件。
```

和普通 `@EventListener` 不同，它可以和事务绑定。

我们这里写的是：

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
```

意思是：

```text
等当前数据库事务成功提交以后，再处理这个事件。
```

为什么要等提交后？

因为 Todo 主业务如果失败了，事务会回滚。

如果 Todo 没创建成功，却提前写了“创建 Todo”的日志，就会出现假日志。

所以正确顺序是：

```text
Todo 主业务提交成功
再记录操作日志
```

### @Async

代码：

```java
@Async(AsyncConfig.TODO_TASK_EXECUTOR)
```

意思是：

```text
这个监听方法放到 todoTaskExecutor 线程池里异步执行。
```

也就是：

```text
主请求线程不负责写日志
异步线程负责写日志
```

这就是本周的核心味道：主线和旁支分开。

## 第八步：TodoActionLogService 是什么？

文件：

```text
src/main/java/com/zading/todoapi/service/TodoActionLogService.java
```

核心代码：

```java
@Service
public class TodoActionLogService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(TodoActionLogEvent event) {
        Todo todo = todoRepository.getReferenceById(event.todoId());
        AppUser user = userRepository.getReferenceById(event.userId());

        todoActionLogRepository.save(new TodoActionLog(
                todo,
                user,
                event.action(),
                event.description()
        ));
    }
}
```

这个类负责真正写数据库。

为什么单独拆成一个 Service？

因为 Listener 只应该负责：

```text
接事件
调处理逻辑
```

真正的业务写入放到 Service，更清晰，也更容易测试。

## 第九步：REQUIRES_NEW 是什么？

代码：

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

意思是：

```text
记录日志时开启一个新的事务。
```

为什么需要新事务？

因为事件监听发生在主事务提交之后。

这时原来的事务已经结束了。

日志写入需要自己的事务，才能安全地保存到数据库。

所以这里用：

```text
REQUIRES_NEW
```

可以理解为：

```text
主业务一个事务
日志写入另一个事务
```

这也是异步事件里很常见的写法。

## 第十步：getReferenceById 是什么？

代码：

```java
Todo todo = todoRepository.getReferenceById(event.todoId());
AppUser user = userRepository.getReferenceById(event.userId());
```

`getReferenceById` 会拿到一个 JPA 引用对象。

这里我们只是为了创建关联关系：

```text
TodoActionLog.todo_id = event.todoId
TodoActionLog.user_id = event.userId
```

不需要立刻完整查询 Todo 和 User 的所有字段。

所以用 `getReferenceById` 比 `findById` 更轻一点。

简单记：

```text
要完整读取对象，用 findById
只为了建立外键引用，可以用 getReferenceById
```

## 第十一步：为什么测试要等异步日志？

之前日志是同步写的。

也就是说：

```text
接口返回时，日志已经在数据库里了。
```

现在日志是异步写的。

也就是说：

```text
接口返回时，日志可能还在队列里，马上查不一定查得到。
```

所以测试里新增了：

```java
protected void waitUntilActionLogCount(Long todoId, Long userId, long expectedCount) {
}
```

它会在最多 3 秒内反复检查：

```text
指定 Todo 的日志数量是否达到预期
```

达到就继续测试。

如果一直没达到，就测试失败。

这不是“随便 sleep 一下”，而是：

```text
带条件的等待。
```

工程里测试异步逻辑时，这种方式很常见。

## 第十二步：为什么 Repository 增加 count 方法？

文件：

```text
src/main/java/com/zading/todoapi/repository/TodoActionLogRepository.java
```

新增：

```java
long countByTodoIdAndUserId(Long todoId, Long userId);
```

Spring Data JPA 会根据方法名自动生成查询。

这个方法表示：

```text
统计某个用户某个 Todo 的操作日志数量。
```

它主要服务于测试里的等待逻辑。

## 第十三步：这和 Kafka 有什么关系？

当前我们用的是 Spring Event。

它的特点：

```text
应用内部事件
不需要安装额外服务
适合学习事件驱动思想
```

Kafka 是外部消息队列。

它的特点：

```text
跨服务传递消息
消息可以持久化
适合微服务之间解耦
需要单独部署服务
```

关系可以这样理解：

```text
Spring Event 是同一个应用里的“小型事件机制”
Kafka 是多个应用之间的“消息系统”
```

所以我们先学 Spring Event，再学 Kafka，会更顺。

## 本周你需要真正掌握的点

### 1. 什么是事件？

事件就是：

```text
系统里发生了一件事。
```

比如：

```text
Todo 被创建了
Todo 被修改了
Todo 被删除了
```

### 2. 什么是发布事件？

发布事件就是告诉系统：

```text
这件事发生了，谁关心谁来处理。
```

### 3. 什么是监听事件？

监听事件就是：

```text
某个组件订阅这种事件，事件发生后执行对应逻辑。
```

### 4. 什么是异步？

异步就是：

```text
当前线程不等任务全部做完，把任务交给另一个线程处理。
```

### 5. 为什么要 AFTER_COMMIT？

因为只有主事务提交成功以后，日志才应该被记录。

### 6. 为什么事件里只放 id？

因为异步处理跨线程、跨事务，传简单字段更稳定。

### 7. 异步有什么风险？

异步任务失败时，主接口可能已经成功返回。

所以异步任务需要：

```text
日志记录
重试机制
监控告警
```

当前项目先做日志记录。

## 本周复盘问题和参考答案

### 1. 第十六周最大的变化是什么？

把 Todo 操作日志从 `TodoService` 直接写入，改成通过 Spring Event 发布事件，再由 Listener 异步写入。

### 2. 为什么要用事件？

为了让主业务和附属业务解耦。`TodoService` 不需要关心日志怎么写，只需要发布“发生了某个操作”的事件。

### 3. `@EnableAsync` 的作用是什么？

开启 Spring 的异步方法能力。没有它，`@Async` 不会真正异步执行。

### 4. `@Async` 的作用是什么？

让被标记的方法放到线程池中执行，不阻塞当前调用线程。

### 5. 为什么 Listener 使用 `AFTER_COMMIT`？

因为只有 Todo 主业务事务提交成功后，操作日志才是真实可信的。否则主业务回滚但日志保存成功，会出现假日志。

### 6. 为什么日志写入要 `REQUIRES_NEW`？

因为 Listener 在主事务提交后执行，原事务已经结束。日志写入需要开启自己的新事务。

### 7. 为什么事件里只传 `todoId` 和 `userId`，不传完整 Entity？

因为异步跨线程、跨事务，传 JPA Entity 容易遇到懒加载或对象状态问题。传 id 更简单稳定。

### 8. 异步任务失败会影响主接口返回吗？

一般不会。主接口可能已经成功返回了，所以异步失败需要通过日志、重试、告警来处理。

### 9. 当前项目的异步线程叫什么？

线程名前缀是 `todo-async-`，方便在日志里看出任务来自 Todo 异步线程池。

### 10. Spring Event 和 Kafka 的区别是什么？

Spring Event 是应用内部事件机制，不需要额外服务。Kafka 是外部消息队列，适合多个服务之间传递消息。

### 11. 为什么测试要等待异步日志？

因为接口返回时，异步日志可能还没写入数据库。测试需要等待日志数量达到预期后再断言。

### 12. 第十六周最重要的工程化思想是什么？

主业务和附属业务分离。主业务保证核心数据正确，附属业务通过事件异步处理。
