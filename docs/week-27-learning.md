# 第 27 周：Kafka / RabbitMQ 消息队列——生产者、消费者、重试和死信

本周把第 16 周的 Spring Event 从“应用内部异步通知”继续升级为“可接入外部消息中间件的消息链路”。本周同时实现 Kafka 和 RabbitMQ 两套适配器，但默认仍使用内存实现，因此你本机没有安装 Kafka、RabbitMQ、Docker 也可以运行和测试。

## 一、本周完成后的能力

完成本周后，你应该能够解释并实现：

```text
生产者 Producer
消费者 Consumer
Topic / Queue
Partition / Consumer Group
Offset
Exchange / Binding / Routing Key
ACK 确认
失败重试
死信 Topic / 死信 Queue
重复消费和幂等
```

当前 Todo 操作日志的链路是：

```text
TodoService
  -> 发布 TodoActionLogEvent
  -> 事务提交后 EventListener 接收
  -> TodoMessagePublisher 发布消息
  -> 内存 / Kafka / RabbitMQ
  -> TodoActionLogMessageHandler 消费消息
  -> TodoActionLogService 保存日志
```

## 二、第 26 周和第 27 周的关系

第 26 周主要解决“请求保护”和“并发控制”：

```text
Redis 分布式锁
接口限流
HTTP 请求幂等
```

第 27 周主要解决“业务动作如何异步传递”：

```text
消息发布
消息消费
消费确认
失败重试
死信处理
消费者幂等
```

两周会共同使用幂等思想：

```text
HTTP 请求重复提交 -> 第 26 周的 Idempotency-Key
消息重复投递     -> 第 27 周的 messageId + 消费幂等存储
```

消息队列并不会自动保证业务代码只执行一次。实际系统通常采用“至少一次投递 + 消费者幂等”。

## 三、本周代码结构

```text
src/main/java/com/zading/todoapi/
├── config/properties/
│   └── MessagingProperties.java
├── messaging/
│   ├── TodoActionLogMessage.java
│   ├── TodoMessagePublisher.java
│   ├── MessagePublishException.java
│   ├── TodoActionLogMessageHandler.java
│   ├── InMemoryTodoMessagePublisher.java
│   ├── kafka/
│   │   ├── KafkaMessagingConfig.java
│   │   ├── KafkaTodoMessagePublisher.java
│   │   └── KafkaTodoMessageConsumer.java
│   └── rabbitmq/
│       ├── RabbitMessagingConfig.java
│       ├── RabbitTodoMessagePublisher.java
│       └── RabbitTodoMessageConsumer.java
└── event/
    └── TodoActionLogEventListener.java
```

配置文件：

```text
application.properties             公共配置和内存默认值
application-kafka.properties       Kafka 连接和序列化配置
application-rabbitmq.properties    RabbitMQ 连接和监听配置
```

## 四、先理解消息队列到底解决什么问题

### 1. 原来的同步调用

```text
HTTP 请求
  -> TodoService 保存 Todo
  -> 直接保存操作日志
  -> 返回响应
```

如果日志写入慢，用户创建 Todo 的接口也会变慢。如果日志服务暂时不可用，主业务也可能受到影响。

### 2. 消息队列调用

```text
HTTP 请求
  -> TodoService 保存 Todo
  -> 发布操作日志消息
  -> 返回响应

消费者
  -> 读取操作日志消息
  -> 保存日志
```

生产者和消费者通过消息解耦。生产者只需要知道“消息发到哪里”，不需要知道消费者具体如何保存日志。

### 3. 消息队列的三个主要价值

#### 解耦

```text
TodoService 不直接依赖 TodoActionLogService
```

未来同一个 Todo 消息可以被多个消费者使用：

```text
操作日志消费者
通知消费者
搜索索引消费者
统计消费者
```

#### 削峰

短时间内产生大量消息时，生产者可以快速写入队列，消费者按照自己的处理能力逐步消费。

#### 异步

不要求用户等待所有附属动作完成。主业务完成后，附属业务在后台执行。

## 五、公共消息抽象详细解析

### 1. `TodoActionLogMessage`

```java
public record TodoActionLogMessage(
        UUID messageId,
        Long todoId,
        Long userId,
        TodoAction action,
        String description,
        Instant occurredAt
) {
}
```

逐个字段理解：

| 字段 | 作用 |
|---|---|
| `messageId` | 这条消息的唯一身份，用于重复消费判断 |
| `todoId` | 消费者要记录哪个 Todo 的日志 |
| `userId` | 消费者要关联哪个用户 |
| `action` | 创建、完成、删除等操作类型 |
| `description` | 给人看的动作描述 |
| `occurredAt` | 业务动作发生的时间 |

为什么不直接发送 `Todo` Entity？

- Entity 可能带有懒加载关联对象；
- Entity 属于数据库模型，不应该成为外部消息契约；
- 消息消费者只需要几个字段，不需要整个对象；
- Entity 直接序列化可能暴露内部字段或产生循环引用。

`from` 和 `toEvent` 方法负责两种模型之间的转换：

```text
TodoActionLogEvent     -> TodoActionLogMessage
TodoActionLogMessage   -> TodoActionLogEvent
```

### 2. `TodoMessagePublisher`

```java
public interface TodoMessagePublisher {
    void publish(TodoActionLogMessage message);
}
```

业务代码只依赖接口，不直接依赖：

```java
KafkaTemplate
RabbitTemplate
```

这样可以替换实现：

```text
默认 profile  -> InMemoryTodoMessagePublisher
kafka profile -> KafkaTodoMessagePublisher
rabbitmq      -> RabbitTodoMessagePublisher
```

### 3. 第 16 周的 EventListener 如何变化

现在的监听器不再直接调用日志 Service：

```java
@Async(AsyncConfig.TODO_TASK_EXECUTOR)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleTodoActionLog(TodoActionLogEvent event) {
    todoMessagePublisher.publish(TodoActionLogMessage.from(event));
}
```

这里有三层含义：

1. `AFTER_COMMIT`：Todo 数据库事务成功后才发布消息；
2. `@Async`：发布动作不阻塞主请求线程；
3. `TodoMessagePublisher`：消息可以进入内存、Kafka 或 RabbitMQ。

注意：`AFTER_COMMIT` 之后数据库事务已经结束，消息发送失败不能回滚已经提交的 Todo。因此生产环境还需要消息发送失败日志、监控和补偿机制。

## 六、消费者公共处理器：为什么两套中间件共用它

### 1. 消费者适配器和业务处理器分离

```text
KafkaTodoMessageConsumer
RabbitTodoMessageConsumer
InMemoryTodoMessagePublisher
          ↓
TodoActionLogMessageHandler
          ↓
TodoActionLogService
```

Kafka 和 RabbitMQ 的监听注解不同，但收到消息后的业务规则相同。因此把消息幂等、日志保存和失败释放集中在 `TodoActionLogMessageHandler` 中。

### 2. 消费流程

```java
IdempotencyClaim claim = idempotencyStore.tryClaim(key, ttl);

if (claim.status() != IdempotencyClaim.Status.CLAIMED) {
    return;
}

try {
    todoActionLogService.record(message.toEvent());
    idempotencyStore.complete(key, ownerToken, "ACKED", ttl);
} catch (RuntimeException ex) {
    idempotencyStore.release(key, ownerToken);
    throw ex;
}
```

逐步理解：

1. 先根据 `messageId` 抢占消费幂等 Key；
2. 已完成或正在处理时，不重复执行业务；
3. 抢占成功后保存操作日志；
4. 日志保存成功后标记 `ACKED`；
5. 处理失败时释放 claim 并重新抛出异常；
6. Kafka 或 RabbitMQ 的消费容器看到异常后执行重试。

这里的 `complete` 不是消息中间件的 ACK，而是业务幂等状态的完成标记。中间件 ACK 由 Kafka/RabbitMQ 消费容器负责，业务完成标记由应用负责，两者是不同层次。

## 七、Kafka 详细解读

### 1. Kafka 是什么

Kafka 可以理解为一个高吞吐的分布式事件流平台。它的核心不是“一个简单队列”，而是：

```text
Topic
  -> Partition
  -> Message Record
  -> Offset
```

生产者把消息写入 Topic，消费者从 Topic 中按 Offset 读取消息。

### 2. Topic 是什么

Topic 是消息的逻辑分类。

当前项目使用：

```text
todo-action-log
```

这个 Topic 表示“Todo 操作日志消息”。不同业务通常使用不同 Topic：

```text
todo-action-log
user-registered
payment-created
email-notification
```

Topic 不是消费者实例，而是消息被保存和读取的逻辑入口。

### 3. Partition 是什么

一个 Topic 可以拆成多个 Partition：

```text
todo-action-log
  ├── partition-0
  ├── partition-1
  └── partition-2
```

Partition 带来两个能力：

- 并行消费：不同消费者可以处理不同 Partition；
- 局部有序：同一个 Partition 内的消息按照 Offset 有序。

重要：Kafka 只保证单个 Partition 内有序，不保证整个 Topic 的全局顺序。

### 4. Message Key 如何影响顺序

Kafka Producer 可以发送：

```java
kafkaTemplate.send(topic, message.messageId().toString(), message);
```

第二个参数是消息 Key。Kafka 会根据 Key 选择 Partition。当前使用 `messageId` 作为 Key，主要是让消息路由稳定；如果业务要求“同一个 Todo 的操作严格有序”，更适合使用 `todoId` 作为 Key：

```java
kafkaTemplate.send(topic, message.todoId().toString(), message);
```

这样同一个 Todo 的消息会进入同一个 Partition，但不同 Todo 仍然可以并行处理。

### 5. Offset 是什么

Offset 是消息在某个 Partition 中的位置编号：

```text
partition-0:
offset 0 -> 创建
offset 1 -> 修改
offset 2 -> 完成
```

消费者提交 Offset 后，Kafka 才知道这个消费者已经处理到哪里。Offset 不是全局消息 id，而是“Partition 内的位置”。

### 6. Consumer Group 是什么

当前配置：

```properties
spring.kafka.consumer.group-id=java-todo-api
```

同一个 Consumer Group 内：

```text
一个 Partition 同一时间只分配给一个消费者实例
```

例如 3 个 Partition、2 个消费者：

```text
消费者 A -> partition-0、partition-1
消费者 B -> partition-2
```

如果两个不同 Consumer Group 都订阅同一个 Topic：

```text
日志消费者组 -> 消费一份
通知消费者组 -> 也消费一份
```

这就是 Kafka 同一条消息可以被多个业务系统分别消费的原因。

### 7. Kafka Producer 代码

```java
kafkaTemplate.send(
        properties.kafka().topic(),
        message.messageId().toString(),
        message
).get(5, TimeUnit.SECONDS);
```

逐部分理解：

- 第一个参数是 Topic；
- 第二个参数是消息 Key；
- 第三个参数是消息 Value；
- `JsonSerializer` 把 Java record 转成 JSON；
- `get(5, TimeUnit.SECONDS)` 等待发送结果；
- 发送失败或超时就抛出 `MessagePublishException`。

等待发送结果会占用线程，在高吞吐场景可能需要异步回调。但学习阶段先明确“消息是否真的写入 Broker”，更容易理解失败边界。

### 8. Kafka Consumer 代码

```java
@RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1_000L)
)
@KafkaListener(
        topics = "${app.messaging.kafka.topic}",
        groupId = "${app.messaging.kafka.consumer-group}"
)
public void consume(TodoActionLogMessage message) {
    messageHandler.handle(message);
}
```

含义：

- `@KafkaListener` 注册 Kafka 消费者；
- `topics` 指定监听的 Topic；
- `groupId` 指定消费者组；
- 消费方法正常返回，容器才认为本次处理成功；
- `messageHandler.handle` 抛异常，触发重试；
- `@RetryableTopic` 会创建重试 Topic；
- 多次失败后进入 DLT（Dead Letter Topic）。

Kafka 的 DLT 是一个特殊 Topic，不是“把消息直接丢掉”。进入 DLT 后，运维人员可以检查原始消息、异常原因和消费上下文。

### 9. Kafka 序列化和反序列化

生产者配置：

```properties
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

消费者配置：

```properties
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.value.default.type=com.zading.todoapi.messaging.TodoActionLogMessage
```

可以理解为：

```text
Java 对象 -> JsonSerializer -> Kafka 字节数据
Kafka 字节数据 -> JsonDeserializer -> Java 对象
```

### 10. Kafka 的 ACK 和提交 Offset

当前配置：

```properties
spring.kafka.listener.ack-mode=record
spring.kafka.consumer.enable-auto-commit=false
```

含义是：应用不让 Kafka 客户端自动随意提交 Offset，而是由监听容器根据单条记录处理结果管理提交时机。

```text
处理成功 -> 提交 Offset -> 下一条消息
处理抛异常 -> 不确认当前处理结果 -> 进入重试流程
```

这不是严格的“数据库事务和 Kafka Offset 同一个事务”。如果数据库已经写成功、Offset 尚未提交，消息可能再次投递，所以消费者幂等仍然必须存在。

### 11. Kafka 适合什么场景

Kafka 更适合：

- 高吞吐日志流；
- 用户行为事件；
- 数据同步；
- 多个系统订阅同一事件；
- 需要按时间保留消息并重复读取；
- 流式处理和大数据场景。

## 八、RabbitMQ 详细解读

### 1. RabbitMQ 的核心模型

RabbitMQ 常见链路是：

```text
Producer
  -> Exchange
  -> Binding + Routing Key
  -> Queue
  -> Consumer
```

RabbitMQ 中生产者通常不直接把消息发给 Queue，而是先发给 Exchange；Exchange 根据 Binding 和 Routing Key 把消息路由到一个或多个 Queue。

### 2. Exchange 是什么

当前使用 Direct Exchange：

```properties
app.messaging.rabbit.exchange=todo.events
```

Direct Exchange 按 routing key 精确匹配：

```text
exchange: todo.events
routing key: todo.action-log
queue: todo-action-log
```

生产者发送到 `todo.events`，并使用 `todo.action-log`，绑定了这个 routing key 的 Queue 才能收到消息。

### 3. Queue 是什么

Queue 是消息真正等待消费者处理的地方：

```properties
app.messaging.rabbit.queue=todo-action-log
```

消费者监听 Queue。RabbitMQ 的 Queue 通常更贴近“任务分发”模型：多个消费者监听同一个 Queue 时，一条消息通常只交给其中一个消费者。

### 4. Binding 和 Routing Key

配置代码：

```java
BindingBuilder.bind(todoActionLogQueue)
        .to(todoActionLogExchange)
        .with(properties.rabbit().routingKey());
```

可以翻译成：

```text
把 todo-action-log Queue 绑定到 todo.events Exchange
绑定使用 todo.action-log 这个 routing key
```

如果生产者的 routing key 拼错，Exchange 找不到匹配 Binding，消息可能无法到达目标 Queue。因此 RabbitMQ 排查时要同时看 Exchange、Binding、Routing Key 和 Queue。

### 5. RabbitMQ Producer 代码

```java
rabbitTemplate.convertAndSend(
        rabbit.exchange(),
        rabbit.routingKey(),
        message,
        amqpMessage -> {
            amqpMessage.getMessageProperties()
                    .setMessageId(message.messageId().toString());
            return amqpMessage;
        }
);
```

含义：

- `convertAndSend` 把 Java 对象转换成 AMQP 消息并发送；
- 第一个参数是 Exchange；
- 第二个参数是 Routing Key；
- 第三个参数是消息体；
- `MessagePostProcessor` 设置消息级别的 messageId；
- `Jackson2JsonMessageConverter` 负责 JSON 转换。

### 6. RabbitMQ Consumer 代码

```java
@RabbitListener(
        queues = "${app.messaging.rabbit.queue}",
        containerFactory = "todoRabbitListenerContainerFactory"
)
public void consume(TodoActionLogMessage message) {
    messageHandler.handle(message);
}
```

当前使用自动确认模式：

```text
消费方法正常返回 -> ACK
消费方法抛异常     -> 不 ACK，进入重试策略
```

因此消费者处理失败时必须抛出异常。如果捕获异常后直接返回，容器可能认为消息处理成功，消息就不会进入重试。

### 7. RabbitMQ 重试配置

```java
factory.setAdviceChain(RetryInterceptorBuilder.stateless()
        .maxAttempts(3)
        .backOffOptions(1000, 2.0, 10_000L)
        .recoverer(new RejectAndDontRequeueRecoverer())
        .build());
```

逐项理解：

- `maxAttempts(3)`：最多处理 3 次；
- `backOffOptions`：每次失败之间等待，避免立即快速重试；
- `RejectAndDontRequeueRecoverer`：最终失败后拒绝消息且不重新放回原 Queue；
- `defaultRequeueRejected(false)`：避免失败消息无限重新入队。

### 8. RabbitMQ 死信交换机和死信队列

业务 Queue 配置了：

```java
QueueBuilder.durable(rabbit.queue())
        .withArgument("x-dead-letter-exchange", rabbit.deadLetterExchange())
        .withArgument("x-dead-letter-routing-key", rabbit.deadLetterRoutingKey())
        .build();
```

含义是：如果消息最终被拒绝，RabbitMQ 把它转发到死信 Exchange，再通过死信 Routing Key 路由到死信 Queue。

```text
todo-action-log
  -> 消费失败
  -> 重试 3 次
  -> RejectAndDontRequeueRecoverer
  -> todo.events.dlx
  -> todo-action-log.dlq
```

死信队列的价值是保留问题消息，方便人工检查、修复后重新投递，而不是让坏消息一直阻塞正常 Queue。

### 9. RabbitMQ 适合什么场景

RabbitMQ 更适合：

- 业务任务分发；
- 订单、支付、通知等业务消息；
- 需要灵活路由的消息；
- 工作队列；
- 需要 ACK、重试、死信的任务处理；
- 消息吞吐量中等但路由规则较复杂的系统。

## 九、Kafka 和 RabbitMQ 对比

| 对比项 | Kafka | RabbitMQ |
|---|---|---|
| 核心入口 | Topic | Exchange |
| 消息落点 | Partition | Queue |
| 路由方式 | Topic + Partition Key | Exchange + Binding + Routing Key |
| 消费进度 | Offset | ACK / 未 ACK 状态 |
| 并行模型 | Consumer Group 分配 Partition | 多消费者竞争 Queue 中的消息 |
| 顺序保证 | 单 Partition 内有序 | 单 Queue 也要结合消费者并发理解顺序 |
| 消息保留 | 按 retention 保留，可重复读取 | 通常确认后移除，需额外配置保留策略 |
| 重试方式 | Retry Topic / DLT | Spring Retry + DLX / DLQ |
| 擅长方向 | 高吞吐事件流、日志、数据管道 | 业务任务、路由、工作队列 |
| 典型关注点 | Partition、Offset、Consumer Group | Exchange、Binding、Routing Key、ACK |

不要简单地说“Kafka 一定比 RabbitMQ 快”或“RabbitMQ 更简单所以一定更好”。应该根据业务问题选择：

```text
需要事件流、多个系统重复读取、高吞吐 -> Kafka
需要业务任务路由、ACK、重试、死信 -> RabbitMQ
```

## 十、为什么本项目实现两套适配器

不是让生产环境同时连接两个中间件，而是用同一业务抽象演示两种模型：

```text
TodoService / EventListener / Handler
             不变
                ↓
       TodoMessagePublisher
          ↙          ↘
       Kafka        RabbitMQ
```

profile 互斥使用：

```text
默认         -> 内存实现
kafka        -> Kafka 实现
rabbitmq     -> RabbitMQ 实现
```

如果同时启用 `kafka,rabbitmq`，会出现两个 `TodoMessagePublisher` Bean，不建议这样运行。真实项目通常根据团队基础设施和业务场景选择其中一种。

## 十一、当前如何运行

### 1. 默认运行：不需要外部中间件

```bash
mvn spring-boot:run
```

默认使用 `InMemoryTodoMessagePublisher`。第 16 周的 Spring Event 仍会异步触发，消息处理器和消费幂等逻辑也会执行。

### 2. Kafka profile

本机已有 Kafka 时：

```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
mvn spring-boot:run -Dspring-boot.run.profiles=kafka
```

配置文件：

```text
src/main/resources/application-kafka.properties
```

当前没有安装 Kafka 时，不要启用这个 profile，否则发布消息时会连接失败。

### 3. RabbitMQ profile

本机已有 RabbitMQ 时：

```bash
RABBITMQ_HOST=localhost \
mvn spring-boot:run -Dspring-boot.run.profiles=rabbitmq
```

配置文件：

```text
src/main/resources/application-rabbitmq.properties
```

当前没有安装 RabbitMQ 时，不要启用这个 profile。

## 十二、每天 2 小时学习安排

### 第一天：消息队列基础

学习同步、异步、解耦、削峰、生产者和消费者。用当前 Todo 日志链路画出同步版和消息版的差异。

### 第二天：Kafka 基础

重点理解 Topic、Partition、Offset、Consumer Group，并回答“为什么 Kafka 只保证 Partition 内有序”。

### 第三天：RabbitMQ 基础

重点理解 Exchange、Queue、Binding、Routing Key、ACK，并画出消息路由图。

### 第四天：阅读公共代码

阅读 `TodoActionLogMessage`、`TodoMessagePublisher` 和 `TodoActionLogMessageHandler`，理解为什么两套中间件共享业务处理器。

### 第五天：阅读 Kafka 代码

阅读 Kafka 配置、Producer、Consumer 和 `@RetryableTopic`，重点理解重试 Topic、DLT 和 Offset。

### 第六天：阅读 RabbitMQ 代码

阅读 Exchange、Queue、Binding、RetryInterceptor 和 DLQ 配置，重点理解 ACK 与拒绝消息的关系。

### 第七天：测试和复盘

运行测试，模拟重复消费和消费失败，比较 Kafka 与 RabbitMQ 的适用场景。

## 十三、复盘问题与参考答案

### 1. Kafka 和 RabbitMQ 最大的模型区别是什么？

Kafka 以 Topic、Partition、Offset 和 Consumer Group 为核心，消息像一条可以按位置读取的事件流。RabbitMQ 以 Exchange、Queue、Binding、Routing Key 和 ACK 为核心，消息由 Broker 路由到等待任务的 Queue。

### 2. 为什么 Kafka 需要 Partition？

Partition 同时提供水平扩展和并行消费能力。一个 Topic 可以有多个 Partition，不同消费者处理不同 Partition。代价是全局顺序不再天然存在，只能保证单 Partition 内有序。

### 3. 为什么同一个 Todo 的 Kafka 消息可以使用 `todoId` 作为 Key？

相同 Key 会稳定路由到同一个 Partition，因此同一个 Todo 的创建、完成、删除事件可以保持相对顺序；不同 Todo 仍可以分布到不同 Partition 并行处理。

### 4. Consumer Group 是什么？

Consumer Group 是一组共同消费 Topic 的消费者。组内一个 Partition 同一时间通常只分给一个消费者；不同 Group 可以分别消费同一条消息，适合日志、通知、统计等多个下游系统订阅同一事件。

### 5. RabbitMQ 为什么需要 Exchange？

Exchange 把生产者和 Queue 解耦，并根据 Binding 和 Routing Key 路由消息。生产者不需要知道具体 Queue，后续可以增加不同 Queue 接收同一类或不同类型的消息。

### 6. ACK 是什么？

ACK 是消费者向消息中间件确认“这条消息我处理成功了”。没有 ACK 或处理抛异常时，消息可以重试或重新投递。ACK 不是业务数据库提交的替代品，消费者仍要处理数据库和消息确认之间的重复执行问题。

### 7. 为什么消费失败必须继续抛异常？

Kafka 和 RabbitMQ 的消费容器通常根据监听方法是否抛异常判断成功或失败。如果捕获异常后直接返回，容器可能认为消息已经处理成功，从而提交 Offset 或 ACK，导致消息丢失。

### 8. 重试和死信有什么区别？

重试是给临时故障机会，例如数据库连接短暂失败；死信是消息多次处理仍然失败后的隔离位置。死信队列让坏消息不再阻塞正常消息，并保留后续排查和人工补偿的机会。

### 9. 为什么消息消费还需要幂等？

消息可能因为消费者处理成功但 Offset/ACK 尚未确认、网络重试或消费者崩溃而再次投递。使用 `messageId` 抢占幂等 Key，可以让重复消息直接跳过业务处理。

### 10. Spring Event 和 Kafka 是什么关系？

Spring Event 是当前 JVM 内的事件机制，不需要外部服务；Kafka 是独立进程提供的消息平台，能在应用重启后保留消息，并让多个服务通过 Consumer Group 消费。当前项目用 Spring Event 做事务提交后的桥接，再由 Publisher 选择最终消息实现。

### 11. 为什么默认使用内存实现？

为了让项目在没有 Kafka、RabbitMQ、Docker 的电脑上仍然可以运行测试。内存实现只能覆盖单 JVM 场景，不能替代真实 Broker 的网络、持久化、分区、路由和故障行为。

### 12. 本周代码还缺少哪些生产能力？

真实生产环境通常还要补充：

- Outbox 事务消息，避免数据库已提交但消息未发布；
- 消息监控、堆积告警和消费延迟指标；
- DLT 重放和人工补偿工具；
- 消费者并发度和限流配置；
- 消息 Schema 版本管理；
- 更可靠的消息幂等记录和长期存储；
- Kafka 集群或 RabbitMQ 集群的高可用配置。

本周实现重点是理解核心模型和代码边界，下一周再进入综合项目复盘和重构。
