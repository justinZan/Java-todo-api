# 第 28 周：综合项目复盘和工程化重构

本周是当前 Java Todo API 学习路线的综合收尾周。前 27 周已经完成了从 Java 基础、Spring Boot、数据库、认证授权、缓存、定时任务、附件管理到 Kafka/RabbitMQ 的学习。本周不再引入新的基础设施，而是把已有代码整理成更容易阅读、测试和维护的工程。

## 一、本周目标

完成本周后，你应该能够：

- 从 HTTP 请求一路追踪到数据库和异步消息处理；
- 区分 Controller、Service、Repository、Mapper、DTO、Entity 的职责；
- 识别“能运行但不容易维护”的代码；
- 在不改变接口行为的前提下进行小步重构；
- 为重构补充单元测试和回归测试；
- 解释事务、缓存、幂等、消息队列和权限之间的关系；
- 用 Maven 完成编译、测试和打包；
- 对项目的生产限制提出下一步改进方案。

本周最重要的原则是：

> 重构首先要保持行为不变，然后再改善结构。测试是重构的安全网。

## 二、重构前后的整体结构

### 1. 一次同步 HTTP 请求

```text
浏览器 / 前端
  -> Controller 接收 HTTP 参数
  -> DTO 校验
  -> Service 执行业务规则
  -> Repository 访问数据库
  -> Mapper 转换为响应 DTO
  -> 返回统一 ApiResponse
```

### 2. 带操作日志的 Todo 请求

```text
TodoController
  -> TodoService
  -> TodoRepository 保存 Todo
  -> TodoEventPublisher 发布内部事件
  -> 事务提交成功 AFTER_COMMIT
  -> TodoMessagePublisher
  -> 内存 / Kafka / RabbitMQ
  -> TodoActionLogMessageHandler
  -> TodoActionLogService
  -> TodoActionLogRepository 保存日志
```

`AFTER_COMMIT` 很重要：如果 Todo 主事务回滚，就不应该产生一条“Todo 已创建”的操作日志。

## 三、本周实际实现的代码变化

### 1. 把排序参数解析从 Controller 中拆出

新增：

```text
src/main/java/com/zading/todoapi/controller/TodoSortParser.java
```

之前排序规则直接写在 `TodoController` 内部。Controller 同时负责：

- 接收请求；
- 创建分页对象；
- 解析排序字段；
- 校验排序方向；
- 补充稳定排序字段。

职责太多后，Controller 会越来越难读。因此现在由 `TodoSortParser` 专门负责排序规则：

```java
public Sort parse(String sort) {
    String[] parts = sort.split(",");
    String field = parts[0].trim();
    String direction = parts.length > 1 ? parts[1].trim() : "asc";

    if (!ALLOWED_SORT_FIELDS.contains(field)) {
        throw new BusinessException(
                ErrorCode.BAD_REQUEST,
                "不支持的排序字段: " + field
        );
    }

    Sort.Direction sortDirection = parseDirection(direction);
    Sort requestedSort = Sort.by(sortDirection, field);

    if (!"id".equals(field)) {
        requestedSort = requestedSort.and(
                Sort.by(Sort.Direction.ASC, "id")
        );
    }

    return requestedSort;
}
```

解析过程：

1. 把 `title,desc` 拆成字段和方向；
2. 检查字段是否在白名单内；
3. 检查方向是否是 `asc` 或 `desc`；
4. 创建 Spring Data 的 `Sort`；
5. 如果不是按唯一的 `id` 排序，就追加 `id`。

为什么要追加 `id`？

假设多个 Todo 的 `title` 都是“学习 Java”，只按 `title` 排序时，它们的相对顺序可能不稳定。分页查询翻页时，某条数据可能重复出现或被跳过。追加 `id` 后，排序变成：

```text
先按 title 排序
title 相同时再按 id 升序排序
```

这样分页顺序更加稳定。

Controller 现在只需要：

```java
Pageable pageable = PageRequest.of(
        page,
        size,
        todoSortParser.parse(sort)
);
```

Controller 负责“调用”，Parser 负责“解析规则”，这就是职责分离。

### 2. 把操作日志 Entity 转换拆成 Mapper

新增：

```text
src/main/java/com/zading/todoapi/mapper/TodoActionLogMapper.java
```

操作日志的数据库对象是 `TodoActionLog`，接口返回对象是 `TodoActionLogResponse`。两者不能直接混用：

```java
public TodoActionLogResponse toResponse(TodoActionLog log) {
    return new TodoActionLogResponse(
            log.getId(),
            log.getAction(),
            log.getDescription(),
            log.getCreatedAt()
    );
}
```

为什么不直接返回 Entity？

- Entity 是数据库模型，不是接口契约；
- Entity 可能包含关联对象和懒加载关系；
- 数据库字段变化不应该直接影响前端响应；
- DTO 可以隐藏内部字段，降低数据泄露风险。

Controller 现在只负责调用 Mapper：

```java
List<TodoActionLogResponse> logs = todoActionLogMapper.toResponseList(
        todoService.getTodoLogs(currentUser.getId(), id)
);
```

批量转换也统一放进 Mapper，避免 Controller 中出现重复的 `stream().map(...)`。

### 3. 修复附件保存失败后的文件残留

附件上传同时涉及两个系统：

```text
文件系统：保存真正的文件内容
数据库：保存文件名、大小、路径等元数据
```

之前如果 `transferTo` 写入了一部分文件后发生 `IOException`，数据库不会保存元数据，但磁盘上可能残留半个文件。

现在 IO 异常也会清理目标文件：

```java
} catch (IOException ex) {
    // transferTo 可能已经写入了部分内容。
    deleteFileQuietly(targetPath);
    throw new BusinessException(
            ErrorCode.INTERNAL_ERROR,
            "保存附件失败"
    );
} catch (RuntimeException ex) {
    deleteFileQuietly(targetPath);
    throw ex;
}
```

这里有两个关键点：

- `IOException` 和 `RuntimeException` 都要清理；
- 清理动作本身不能覆盖原始异常，所以使用 `deleteFileQuietly`。

新增测试会模拟“已经写入 partial 内容，然后磁盘写入失败”，验证临时文件最终不存在。

这体现了一个重要工程思维：数据库事务不能自动回滚文件系统操作，跨资源操作必须显式设计补偿动作。

### 4. 为未处理异常增加服务端日志

全局异常处理器对前端仍然只返回：

```json
{
  "success": false,
  "code": "INTERNAL_ERROR",
  "message": "服务器内部错误"
}
```

但服务端会记录完整堆栈：

```java
log.error(
        "未处理异常，method={}, uri={}",
        request.getMethod(),
        request.getRequestURI(),
        exception
);
```

为什么不能把完整异常直接返回前端？

- 可能暴露数据库表名；
- 可能暴露服务器文件路径；
- 可能暴露依赖版本和内部实现；
- 攻击者可以利用错误信息推断系统结构。

正确做法是：

```text
前端：得到稳定、安全的错误响应
后端：通过日志、requestId 和堆栈排查真实原因
```

## 四、7 天学习和实践安排

### 第一天：画出项目架构

学习内容：

- Spring Boot 启动类；
- Bean 扫描和依赖注入；
- Controller、Service、Repository 的调用关系；
- 请求、事务、响应的边界。

实践任务：

1. 选择 `POST /api/todos`；
2. 从 `TodoController.createTodo` 开始逐行跟踪；
3. 找到参数校验、限流、幂等、事务、数据库保存的位置；
4. 找到操作日志消息从哪里产生；
5. 画出完整调用链。

### 第二天：理解 Controller 重构

学习内容：

- HTTP 层应该做什么；
- 为什么排序参数属于 Web 层规则；
- 为什么要使用字段白名单；
- 为什么分页需要稳定排序。

实践任务：

- 阅读 `TodoSortParser`；
- 添加一个允许的排序字段；
- 为非法字段和非法方向补充测试；
- 确认原有 API 返回结果不变。

### 第三天：理解 DTO、Entity 和 Mapper

学习内容：

- 数据库模型和接口模型的区别；
- 单个对象转换和集合转换；
- 懒加载对象为什么不适合直接暴露；
- Mapper 为什么适合保持纯粹。

实践任务：

- 阅读 `TodoMapper` 和 `TodoActionLogMapper`；
- 检查所有 Controller 是否直接返回 Entity；
- 如果发现新的 Entity 响应，先设计 Response DTO。

### 第四天：理解跨资源一致性

学习内容：

- 数据库事务只能保证数据库；
- 文件系统不会自动参与数据库回滚；
- 上传失败、数据库保存失败时如何清理文件；
- 路径穿越和文件名安全。

实践任务：

- 阅读 `TodoAttachmentService.uploadAttachment`；
- 模拟文件保存失败；
- 验证数据库没有元数据，磁盘没有残留文件；
- 检查下载路径是否经过 `normalize` 和根目录校验。

### 第五天：复盘 Redis、事务和消息队列

学习内容：

```text
缓存：减少重复查询
分布式锁：避免多个实例同时执行同一任务
HTTP 幂等：避免重复创建 Todo
消息幂等：避免重复消费操作日志
事务：保证数据库操作的一致性
Kafka/RabbitMQ：解耦异步业务
```

重点思考：

```text
Todo 数据库已经提交
应用在发送消息前突然崩溃
操作日志消息会不会丢失？
```

当前项目可能丢失，这也是生产环境需要 Outbox Pattern 的原因。当前实现用于学习消息抽象、消费者幂等、重试和死信，不等于完整生产级可靠消息方案。

### 第六天：测试、构建和运行检查

运行：

```bash
mvn test
mvn package
```

检查：

- 默认 profile 不依赖外部 PostgreSQL；
- 默认 profile 不依赖 Redis；
- 默认 profile 不依赖 Kafka；
- 默认 profile 不依赖 RabbitMQ；
- Actuator 健康检查返回 UP；
- OpenAPI 文档可以访问；
- 测试上传目录不会污染仓库。

### 第七天：最终复盘和重构检查

完成一次完整流程：

```text
注册 -> 登录 -> 创建 Todo -> 查询 Todo
     -> 修改 Todo -> 完成 Todo -> 查看日志
     -> 上传附件 -> 下载附件 -> 删除附件
     -> 验证普通用户和管理员权限
```

最后检查：

- 方法名是否能够表达意图；
- Controller 是否包含业务决策；
- Service 是否有清晰事务边界；
- Repository 是否只负责数据访问；
- 对外响应是否使用 DTO；
- 异常是否有稳定错误码；
- 新增代码是否有测试保护；
- 配置是否能通过环境变量覆盖。

## 五、每天 2 小时的建议节奏

```text
20 分钟：阅读代码和学习文档
70 分钟：修改代码或补充测试
20 分钟：运行测试和接口验证
10 分钟：记录当天复盘问题
```

重构时建议一次只改一个问题。例如先拆排序解析，再运行测试；再拆日志 Mapper，再运行测试。不要同时修改 Controller、Service、数据库和消息配置，否则出了问题很难定位。

## 六、复盘问题与参考答案

### 1. 为什么 Controller 不应该承担太多业务逻辑？

Controller 属于 HTTP 适配层，应该负责接收参数、调用业务服务和组装响应。如果把权限决策、数据库操作、复杂排序和业务状态修改都写在 Controller 中，代码会难以复用，也很难进行 Service 单元测试。

### 2. 为什么排序字段必须使用白名单？

排序字段最终会参与数据库查询。如果直接信任前端传入的字段，可能导致非法字段错误，甚至形成查询注入风险。白名单只允许项目明确支持的字段。

### 3. 为什么分页排序要追加 id？

非唯一字段存在相同值，只按该字段排序时数据库返回顺序可能不稳定。追加唯一字段 `id` 作为第二排序条件，可以减少分页重复和遗漏。

### 4. DTO 和 Entity 的核心区别是什么？

Entity 代表数据库中的持久化对象，DTO 代表接口输入或输出的数据契约。Entity 可以有数据库关联和内部字段，DTO 只暴露当前接口需要的内容。

### 5. 为什么文件操作不能只依赖数据库事务？

数据库事务只能回滚数据库变化，不能自动删除已经写入磁盘的文件。因此文件保存失败、数据库保存失败和删除失败都需要显式补偿或异步清理。

### 6. 为什么全局异常处理器不能把 exception.getMessage() 全部返回？

系统异常消息可能包含敏感信息。对外应该使用稳定的错误码和安全提示，对内通过日志记录完整异常堆栈。

### 7. `AFTER_COMMIT` 的作用是什么？

它让监听器只在主数据库事务成功提交后执行。如果主事务回滚，监听器不会继续发布操作日志消息，避免记录不存在的业务动作。

### 8. Kafka 的 Offset 和 RabbitMQ 的 ACK 有什么区别？

Kafka 用 Offset 表示消费者在 Partition 中读到的位置；RabbitMQ 用 ACK 表示某条 Queue 消息已经被消费者成功处理。Kafka 更强调可持久化日志和位置管理，RabbitMQ 更强调任务确认和队列投递。

### 9. 为什么消息消费还必须做幂等？

消费者可能在业务执行成功后、确认消息之前崩溃，Broker 会再次投递消息。使用 `messageId` 和幂等存储，可以让重复消息不再重复写入业务数据。

### 10. 当前项目是否已经实现了绝对不丢消息？

没有。当前项目在事务提交后的异步发布阶段仍可能遇到应用崩溃，生产者和数据库之间存在时间窗口。更可靠的方案是 Outbox：在同一个数据库事务中保存业务数据和待发送事件，再由独立发布器投递消息。

### 11. 重构时为什么要保持接口行为不变？

接口已经被前端或其他客户端使用。重构的目标是改善内部结构，而不是让调用方被迫适应无关变化。行为不变可以降低风险，也能通过回归测试验证重构是否安全。

### 12. 为什么新增代码一定要配测试？

测试不仅验证“现在能不能运行”，还保护未来的重构。比如 `TodoSortParserTest` 锁定了非法排序字段、非法排序方向和稳定分页规则，后续修改时可以快速发现行为变化。

## 七、本周验证结果

新增测试：

```text
TodoSortParserTest
TodoAttachmentServiceTest.shouldDeletePartialFileWhenStorageFails
```

完整测试命令：

```bash
mvn test
```

本次实际全量测试结果为：

```text
Tests run: 85
Failures: 0
Errors: 0
Skipped: 0
```

如果测试数量因后续新增测试发生变化，以 Maven 最终输出为准。

## 八、完成本周后你获得的能力

你现在不只是“会写几个 Java 类”，而是能够理解一个后端项目的完整生命周期：

```text
需求
  -> API 设计
  -> 参数校验
  -> 业务实现
  -> 事务和数据库
  -> 缓存和并发控制
  -> 异步消息
  -> 权限和安全
  -> 测试
  -> 日志和监控
  -> 重构和维护
```

后续如果继续深入，建议学习顺序是：

1. Outbox Pattern 和可靠消息；
2. Kafka/RabbitMQ 的真实环境集成测试；
3. PostgreSQL 生产部署和备份；
4. Redis Cluster；
5. CI/CD 和云部署；
6. 服务拆分和微服务通信。
