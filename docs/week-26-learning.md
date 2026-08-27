# 第 26 周：Redis 深入——分布式锁、限流和幂等

本周把 Redis 从“缓存数据库”扩展为“并发控制和请求保护组件”。项目保留了默认的 H2 + 本地内存实现，所以当前没有安装 Redis 也可以运行和测试；启用 `redis` profile 后，业务接口会切换为真实 Redis 实现。

## 一、本周实现结果

| 能力 | 接入位置 | 解决的问题 |
| --- | --- | --- |
| Redis 分布式锁 | Todo 过期扫描任务 | 多个应用实例不要同时执行同一批任务 |
| 固定窗口限流 | 登录、Todo 创建接口 | 防止短时间内请求过多 |
| 请求幂等 | Todo 创建接口 | 网络重试或重复点击时只创建一次 |
| TTL | 锁、限流 Key、幂等 Key | 防止临时 Key 永久存在 |
| 本地降级实现 | 默认 profile 和测试 | 没有 Redis 时仍然可以学习和验证流程 |

## 二、代码结构

```text
com.zading.todoapi
├── config/properties
│   └── RedisProtectionProperties.java
├── redis
│   ├── DistributedLock.java
│   ├── RateLimiter.java
│   ├── IdempotencyStore.java
│   ├── RedisDistributedLock.java
│   ├── RedisRateLimiter.java
│   ├── RedisIdempotencyStore.java
│   └── InMemory...（没有 Redis 时的实现）
├── controller
│   ├── AuthController.java
│   └── TodoController.java
├── job
│   └── TodoOverdueJob.java
└── service
    └── TodoService.java
```

业务代码依赖 `DistributedLock`、`RateLimiter` 和 `IdempotencyStore` 接口，而不是直接依赖 `StringRedisTemplate`。这样做有两个好处：

1. 业务代码不需要关心当前使用 Redis 还是内存；
2. 测试可以使用内存实现或 Mock，不需要启动外部 Redis。

## 三、Profile 如何选择实现

Redis 实现使用：

```java
@Component
@Profile("redis")
public class RedisRateLimiter implements RateLimiter {
}
```

没有 Redis 时使用：

```java
@Component
@Profile("!redis")
public class InMemoryRateLimiter implements RateLimiter {
}
```

`@Profile("redis")` 表示只有启用 `redis` profile 时注册这个 Bean；`@Profile("!redis")` 表示没有启用 `redis` profile 时注册本地实现。

默认启动：

```bash
mvn spring-boot:run
```

使用真实 Redis：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

真实 Redis profile 的连接配置位于 `src/main/resources/application-redis.properties`。如果没有 Redis，启用该 profile 后访问相关接口会连接失败，因此当前环境应使用默认 profile。

## 四、配置类解析

配置文件：`src/main/resources/application.properties`

```properties
app.redis.lock-lease=5m
app.redis.idempotency-ttl=10m
app.redis.rate-limit-window=1m
app.redis.login-limit=5
app.redis.todo-create-limit=30
```

配置绑定类：

```java
@ConfigurationProperties(prefix = "app.redis")
public record RedisProtectionProperties(
        Duration lockLease,
        Duration idempotencyTtl,
        Duration rateLimitWindow,
        int loginLimit,
        int todoCreateLimit
) {
}
```

Spring 会把：

```text
app.redis.lock-lease -> lockLease
app.redis.login-limit -> loginLimit
```

绑定成 Java 对象。业务代码调用 `redisProperties.loginLimit()`，比到处读取字符串配置 Key 更容易维护，也能在启动时校验数值不能小于 1。

## 五、分布式锁详细解析

### 1. 为什么 `synchronized` 不够

`synchronized` 只能锁住当前 JVM 的线程。如果部署两个 Java 应用实例：

```text
实例 A：自己的 JVM 锁
实例 B：自己的 JVM 锁
```

A 和 B 并不知道对方已经加锁，因此仍然可能同时执行定时任务。

Redis 锁把锁状态放在多个实例都能访问的 Redis 中：

```text
实例 A ─┐
实例 B ─┼── Redis: lock:todo-overdue-job
实例 C ─┘
```

### 2. `tryLock` 的实现

```java
LockHandle handle = new LockHandle(key, UUID.randomUUID().toString());
Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
        key,
        handle.token(),
        leaseTime
);
```

`setIfAbsent` 对应 Redis 的 `SET NX`：

- Key 不存在时写入成功；
- Key 已存在时写入失败；
- 同一时刻只有一个请求能成功；
- `leaseTime` 对应过期时间，避免进程崩溃后锁永久存在。

锁的 Value 不是固定字符串，而是随机 token：

```text
lock:todo-overdue-job -> 8d7...random-token
```

这样释放锁时可以确认“这是我加的锁”。

### 3. 为什么不能直接 `delete`

假设 A 的锁过期了，B 获得了同一个 Key：

```text
A 的锁过期
B 获得新锁
A 的任务结束，直接 DEL
```

如果 A 直接删除 Key，就会把 B 的新锁删除。项目使用 Lua 脚本实现“比较 token，再删除”：

```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
end
return 0
```

这个比较和删除在 Redis 内部一次完成，A 不能误删 B 的锁。

### 4. 定时任务如何使用锁

```java
Optional<LockHandle> lock = distributedLock.tryLock(
        "lock:todo-overdue-job",
        redisProperties.lockLease()
);

if (lock.isEmpty()) {
    log.info("跳过 Todo 过期扫描：其他实例正在执行");
    return;
}

try {
    scanWithLock();
} finally {
    distributedLock.unlock(lock.get());
}
```

执行顺序：

1. 尝试获取锁；
2. 获取失败，直接跳过本轮；
3. 获取成功，执行扫描；
4. 无论扫描成功还是抛出异常，都在 `finally` 中释放锁。

## 六、固定窗口限流详细解析

### 1. 限流 Key

登录接口使用用户名作为维度：

```text
rate-limit:auth-login:username
```

Todo 创建使用用户 ID：

```text
rate-limit:todo-create:user:10
```

相同 Key 的请求共享一个计数器，不同用户之间互不影响。

### 2. Redis Lua 脚本

```lua
local current = redis.call('INCR', KEYS[1])
if current == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[1])
end
local ttl = redis.call('TTL', KEYS[1])
return {current, ttl}
```

逐句理解：

- `INCR`：计数器加 1；
- `current == 1`：说明这是当前窗口的第一次请求；
- `EXPIRE`：只在第一次请求时设置窗口过期时间；
- `TTL`：获取窗口还剩多少秒；
- 返回当前次数和剩余秒数。

把计数和过期时间放在 Lua 脚本中，是为了让它们在 Redis 中作为一个原子操作执行，避免 `INCR` 成功后应用进程突然中断，导致 Key 没有 TTL。

### 3. 接口中的判断

```java
if (!decision.allowed()) {
    throw new BusinessException(
            ErrorCode.RATE_LIMIT_EXCEEDED,
            "登录请求过于频繁，请在 " + decision.retryAfterSeconds() + " 秒后重试"
    );
}
```

当次数超过限制时，接口返回 HTTP 429：

```json
{
  "success": false,
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "登录请求过于频繁，请在 45 秒后重试"
}
```

当前实现是固定窗口。例如限制每分钟 5 次，窗口从第一次请求开始计算。它容易理解，适合学习；高流量场景还可以升级为滑动窗口或令牌桶。

## 七、幂等详细解析

### 1. 什么是幂等

客户端发送创建请求后，如果网络超时，客户端通常不知道服务器是否已经成功：

```text
客户端 -> 创建 Todo -> 服务器创建成功
                         X 响应丢失
客户端 -> 重试创建 Todo
```

没有幂等控制时会创建两个 Todo。

客户端在两次请求中使用同一个 Header：

```http
Idempotency-Key: todo-create-request-001
```

### 2. 幂等状态

项目中一个幂等 Key 有两种存储状态：

```text
PROCESSING:<owner-token>
COMPLETED:<todo-id>
```

状态流转：

```text
不存在
   |
   | SET NX
   v
PROCESSING
   |
   | 数据库事务提交成功
   v
COMPLETED:todo-id
```

如果第二个请求发现 `PROCESSING`，返回 409；如果发现 `COMPLETED:10`，就读取并返回 Todo 10，不再创建新数据。

### 3. 为什么完成状态要在事务提交后写入

错误顺序：

```text
写入幂等完成状态
保存 Todo
数据库事务回滚
```

这会产生“Redis 说已完成，但数据库没有 Todo”的错误状态。

项目通过 `TransactionSynchronizationManager` 注册 `afterCommit` 回调：

```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        completeIdempotency(key, ownerToken, todoId);
    }
});
```

只有数据库事务真正提交后，幂等 Key 才会变成 `COMPLETED`。如果事务回滚，则在 `afterCompletion` 中释放处理状态。

### 4. 为什么完成和释放也需要 token

和分布式锁一样，幂等处理也有超时场景。如果第一个请求处理时间超过 TTL，第二个请求可能重新获得 Key。第一个请求结束时不能覆盖第二个请求的状态，因此 `complete` 和 `release` 都使用 Lua 比较 owner token。

### 5. 幂等 Key 的作用范围

项目实际保存的 Key 会包含用户 ID：

```text
idempotency:todo-create:user:10:todo-create-request-001
```

这样用户 10 和用户 20 使用相同 Header，也不会互相影响。

## 八、请求示例

### 1. 创建 Todo 并使用幂等 Key

```bash
curl -X POST http://localhost:8080/api/todos \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: todo-create-request-001' \
  -d '{"title":"学习 Redis"}'
```

再次执行完全相同的命令，应该返回相同的 Todo ID，数据库中不会增加第二条记录。

### 2. 启用真实 Redis

确认 Redis 已经运行后：

```bash
REDIS_HOST=localhost \
REDIS_PORT=6379 \
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

本机没有 Redis 时不要执行这个命令，使用默认启动方式即可。

## 九、当前实现的边界

- 内存实现只在单个 JVM 内有效，多实例部署必须使用 Redis；
- 固定窗口限流存在窗口边界突发流量问题；
- Redis 不可用时，锁、幂等和限流不会自动切换到内存实现，避免生产环境误把分布式保护降级为单机保护；
- 当前幂等结果只保存 Todo ID，没有保存完整 HTTP 响应；
- 真正接入生产前，还应增加 Redis 集成测试和监控指标。

## 十、每天 2 小时学习安排

### 第一天：Redis 数据结构和 TTL

- 复习 String、Hash、Set、Sorted Set；
- 理解 Key 命名空间；
- 用 `TTL` 查看临时 Key 生命周期；
- 阅读 `RedisProtectionProperties`。

### 第二天：分布式锁

- 对比 `synchronized` 和 Redis 锁；
- 阅读 `RedisDistributedLock`；
- 理解 `SET NX EX`；
- 解释为什么释放锁必须校验 token。

### 第三天：限流

- 阅读固定窗口限流器；
- 理解 `INCR + EXPIRE`；
- 阅读 Lua 脚本；
- 练习说明 HTTP 429 的含义。

### 第四天：接口幂等

- 理解网络重试产生重复请求的原因；
- 阅读 `IdempotencyClaim` 状态；
- 阅读 Todo 创建的 `afterCommit` 逻辑；
- 使用相同 `Idempotency-Key` 调用两次接口。

### 第五天：Profile 和降级策略

- 对比 Redis 实现和 InMemory 实现；
- 理解为什么测试默认不连接 Redis；
- 思考哪些场景可以降级，哪些场景不能降级。

### 第六天：测试和异常场景

- 阅读 `RedisProtectionTests`；
- 思考锁过期、Redis 宕机、请求超时等场景；
- 为限流和幂等增加自己的测试。

### 第七天：综合复盘

- 画出登录限流、Todo 幂等、过期扫描加锁的流程图；
- 运行全量测试；
- 总结缓存、锁、限流、幂等的区别。

## 十一、复盘问题与答案

### 1. TTL 是什么？

TTL 是 Time To Live，表示 Key 还能存活多长时间。时间到后 Redis 自动删除 Key。锁、限流计数器和幂等 Key 都需要 TTL。

### 2. 为什么 Redis 锁必须有过期时间？

持锁进程可能崩溃。如果没有 TTL，其他实例将永远无法获取这把锁。

### 3. 为什么不能用 `setnx` 后再单独设置过期时间？

两条命令之间如果程序崩溃，Key 会永久存在。应该使用带过期时间的 `SET NX EX`，或使用 Lua 保证原子性。

### 4. 限流和幂等有什么区别？

限流限制单位时间内允许多少请求；幂等保证同一个业务请求重复执行时只产生一次业务结果。

### 5. 为什么幂等 Key 要包含用户 ID？

避免不同用户使用相同 Header 时互相影响，也避免用户读取到其他用户的业务结果。

### 6. 为什么默认不把 Redis 故障自动降级为内存实现？

单机内存实现无法在多实例之间共享状态。分布式锁或幂等降级后可能产生重复任务、重复数据，所以生产环境应该明确选择降级策略。

## 代码精读补充

### 1. 为什么先设计接口，再写 Redis 实现

```java
public interface DistributedLock {
    Optional<LockHandle> tryLock(String key, Duration leaseTime);

    void unlock(LockHandle handle);
}
```

Controller、Service 和 Job 只依赖这个接口。`InMemoryDistributedLock` 使用 `ConcurrentHashMap`，用于无 Redis 的本地学习；`RedisDistributedLock` 使用 `StringRedisTemplate`，用于多实例共享状态。抽象层让业务代码不需要知道具体存储技术。

### 2. Redis 加锁代码如何理解

```java
String token = UUID.randomUUID().toString();
Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(key, token, leaseTime);
```

`setIfAbsent` 对应 Redis 的 `SET NX`：只有 Key 不存在才写入。`leaseTime` 对应过期时间。返回 `true` 才表示当前实例拿到锁，返回 `false` 就跳过任务。

### 3. Lua 解锁脚本为什么必须比较 token

```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
end
return 0
```

假设实例 A 的锁过期后，实例 B 获得同一个 Key；这时 A 的任务才结束。如果 A 直接 `DEL`，会误删 B 的锁。脚本把“比较所有者”和“删除”放进 Redis 的原子执行中。

### 4. 固定窗口限流的原子步骤

```lua
local current = redis.call('incr', KEYS[1])
if current == 1 then
    redis.call('expire', KEYS[1], ARGV[2])
end
local ttl = redis.call('ttl', KEYS[1])
return {current, ttl}
```

第一次请求创建计数并设置窗口过期时间，后续请求只增加计数。`INCR`、首次 `EXPIRE` 和 `TTL` 放在同一个 Lua 脚本中，避免并发请求看到不一致的中间状态。

### 5. 幂等状态机和事务的关系

```text
tryClaim
  -> CLAIMED：当前请求获得执行权
  -> PROCESSING：其他请求正在执行
  -> COMPLETED：直接返回历史结果

数据库事务提交成功
  -> complete(key, todoId)
```

不能在 `todoRepository.save` 调用后就立即写 `COMPLETED`，因为事务可能随后回滚。当前实现通过 `afterCommit` 保存 Todo id；事务失败则释放 claim，避免把失败请求永久当成成功。

### 6. 当前实现的边界要记住

```text
默认内存实现：只在单 JVM 内有效
Redis 实现：需要真实 Redis 才能验证多实例行为
固定窗口：窗口边界处可能出现流量突刺
幂等结果：当前保存的是 Todo id，不是完整响应快照
```

这些边界不是代码错误，而是当前学习版本的明确取舍。生产系统还需要 Redis 集群、监控、重试、Key 清理和更完整的集成测试。
