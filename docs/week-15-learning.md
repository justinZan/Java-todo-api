# 第十五周学习说明：Redis 缓存入门和外部缓存配置

第十五周的目标是：在第十四周 Spring Cache 的基础上，把缓存底层扩展为可选 Redis。

注意，不是强制使用 Redis。

当前项目仍然保持：

```text
默认启动：Simple Cache，本地内存缓存，不需要 Redis
启用 redis profile：Redis Cache，连接外部 Redis 服务
```

最核心的一句话：

```text
第十四周学的是缓存抽象，第十五周学的是把缓存实现从本地内存切换到外部 Redis。
```

## 本周最终改动总览

| 改动 | 文件 | 目的 |
|---|---|---|
| 引入 Redis 依赖 | `pom.xml` | 让项目具备连接 Redis 的能力 |
| 新增 Redis profile | `application-redis.properties` | 只有启用 redis profile 时才使用 Redis |
| 新增 Redis 缓存配置 | `RedisCacheConfig.java` | 配置 Redis 缓存 TTL 和序列化方式 |
| 保留默认 Simple Cache | `application.properties` | 没装 Redis 时项目仍然能直接运行 |
| 测试环境继续 Simple Cache | `application-test.properties` | 单元 / 集成测试不依赖 Redis |
| 关闭 Redis Repository 扫描 | `application.properties` / `application-test.properties` / `application-redis.properties` | 项目只把 Redis 当缓存，不把 Redis 当 Repository 数据库 |
| 固定 Mockito 测试 mock maker | `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` | 避免当前 JDK 21 环境下 Mockito 动态 attach 失败 |
| 更新 README | `README.md` | 补充 Redis profile 启动方式和 TTL 说明 |

## 第十四周和第十五周到底有什么区别？

第十四周做的是：

```text
引入 Spring Cache
开启 @EnableCaching
使用 @Cacheable 缓存 Todo 详情和日志
使用 @CacheEvict 在写操作后清缓存
底层使用 Simple Cache
```

第十五周做的是：

```text
保留第十四周所有缓存注解
新增 Redis 依赖
新增 redis profile
新增 RedisCacheConfig
给 Redis 缓存设置 TTL
让缓存底层可以从本地内存切换成 Redis
```

对照表：

| 对比项 | 第十四周 | 第十五周 |
|---|---|---|
| 学习重点 | 缓存注解和失效规则 | Redis 外部缓存和 TTL |
| 默认缓存 | Simple Cache | 仍然是 Simple Cache |
| 是否需要安装 Redis | 不需要 | 默认不需要，启用 redis profile 才需要 |
| 业务代码是否大改 | 是，给 Service 加缓存注解 | 不大改，复用已有缓存注解 |
| 缓存位置 | Java 应用内存 | 可选 Redis 外部服务 |
| 应用重启后缓存 | 消失 | Redis 中可继续存在，直到 TTL 过期或被清理 |
| 多实例共享缓存 | 不支持 | 支持 |
| 是否有 TTL | Simple Cache 默认没有统一 TTL | Redis 配置了 TTL |

一句话：

```text
第十四周解决“怎么用缓存”，第十五周解决“缓存放在哪里、存多久”。
```

## 第一步：为什么要引入 Redis？

第十四周的 Simple Cache 是本地内存缓存。

它的优点是：

```text
简单
不需要安装额外服务
适合学习
启动快
```

但它也有缺点：

```text
应用重启后缓存消失
多个后端实例之间不能共享缓存
不好统一设置过期时间
不适合真实多实例部署
```

Redis 是独立缓存服务。

它的特点是：

```text
运行在应用外部
多个后端实例可以共享
支持 TTL 过期时间
读写速度很快
以后还能做验证码、限流、分布式锁等功能
```

所以本周不是为了“必须使用 Redis”，而是让项目具备 Redis 能力。

## 第二步：引入 Redis 依赖

文件：

```text
pom.xml
```

新增：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

这个依赖带来两类能力：

```text
1. Redis 连接能力
2. Redis 作为 Spring Cache 底层实现的能力
```

注意：

```text
引入依赖不等于默认就要连接 Redis。
```

项目默认仍然是：

```properties
spring.cache.type=simple
```

所以你本机没装 Redis，也不影响普通启动和测试。

## 第三步：新增 Redis profile

文件：

```text
src/main/resources/application-redis.properties
```

核心配置：

```properties
spring.cache.type=redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.timeout=2s
spring.data.redis.repositories.enabled=false
```

这表示：

```text
只有启用 redis profile 时，缓存才切换成 Redis。
```

启动方式：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

如果没有 Redis，不要这么启动。

普通启动仍然是：

```bash
mvn spring-boot:run
```

这就是 profile 的价值：

```text
同一套代码，不同环境使用不同配置。
```

## 第四步：为什么配置环境变量？

配置里使用：

```properties
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
```

含义是：

```text
如果环境变量 REDIS_HOST 存在，就用环境变量
如果不存在，就用 localhost
```

这样本地可以用默认值。

服务器可以用环境变量：

```bash
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your-password
```

这比把真实地址和密码写死在代码里更安全。

## 第五步：为什么关闭 Redis Repository？

配置：

```properties
spring.data.redis.repositories.enabled=false
```

因为当前项目只是把 Redis 当缓存。

我们没有写：

```text
RedisHash Entity
Redis Repository
```

如果不关闭，Spring Data Redis 会看到项目里的 JPA Repository，然后尝试判断它们是不是 Redis Repository。

这不会一定导致错误，但日志会变吵。

所以我们明确告诉 Spring：

```text
不要扫描 Redis Repository。
```

这是工程里的“减少误判、减少噪音”。

## 第六步：新增 RedisCacheConfig

文件：

```text
src/main/java/com/zading/todoapi/config/RedisCacheConfig.java
```

类上有两个注解：

```java
@Configuration
@Profile("redis")
public class RedisCacheConfig {
}
```

`@Configuration` 表示：

```text
这是一个 Spring 配置类。
```

`@Profile("redis")` 表示：

```text
只有启用 redis profile 时，这个配置类才生效。
```

所以默认启动时：

```text
RedisCacheConfig 不生效
Simple Cache 生效
```

启用 redis profile 时：

```text
RedisCacheConfig 生效
Redis Cache 生效
```

## 第七步：RedisCacheManagerBuilderCustomizer 是什么？

代码：

```java
@Bean
public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper objectMapper) {
}
```

它的作用是：

```text
自定义 RedisCacheManager 的构建过程。
```

你可以把 `CacheManager` 理解为：

```text
缓存总管。
```

第十四周默认 Simple Cache 时，缓存总管管理的是应用内存里的缓存。

第十五周 Redis profile 时，缓存总管管理的是 Redis 里的缓存。

Spring Cache 注解本身不用改：

```java
@Cacheable(cacheNames = CacheNames.TODO_DETAIL, key = "#userId + ':' + #id")
```

变的是底层 CacheManager。

这就是 Spring Cache 抽象非常香的地方：业务代码不用被具体缓存工具绑死。

## 第八步：TTL 是什么？

TTL 是 Time To Live。

意思是：

```text
缓存最多存活多久。
```

本周配置：

```java
private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
private static final Duration TODO_DETAIL_TTL = Duration.ofMinutes(10);
private static final Duration TODO_LOGS_TTL = Duration.ofMinutes(5);
```

含义：

```text
默认缓存：10 分钟
Todo 详情：10 分钟
Todo 日志：5 分钟
```

为什么日志更短？

因为日志在创建、修改、完成、删除、恢复时都会变化。

时间线数据如果过久不更新，用户可能看到旧历史。

虽然我们已经在写操作时用 `@CacheEvict` 主动清缓存，但 TTL 仍然有价值。

TTL 像第二道保险：

```text
主动清理失败或遗漏时，缓存最终也会自动过期。
```

## 第九步：Redis 序列化是什么？

Redis 本质上存的是字节数据。

Java 对象不能原封不动塞进 Redis。

所以需要序列化：

```text
Java 对象 -> 可存储的数据
可存储的数据 -> Java 对象
```

本周配置：

```java
GenericJackson2JsonRedisSerializer valueSerializer =
        GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper.copy())
                .defaultTyping(true)
                .build();
```

意思是：

```text
缓存 value 使用 JSON 方式存入 Redis。
```

为什么不用默认二进制格式？

JSON 更适合学习和排查。

以后你用 Redis CLI 查看缓存时，JSON 比一坨二进制友好多了。

## 第十步：key 序列化为什么用 StringRedisSerializer？

代码：

```java
.serializeKeysWith(
    RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
)
```

意思是：

```text
Redis key 用字符串方式保存。
```

比如缓存 key：

```text
todoDetail::1:10
```

会比二进制 key 更容易排查。

工程经验：

```text
Redis key 尽量让人能看懂。
```

## 第十一步：第十四周的缓存注解为什么不用改？

第十四周已经有：

```java
@Cacheable(cacheNames = CacheNames.TODO_DETAIL, key = "#userId + ':' + #id")
```

和：

```java
@CacheEvict(cacheNames = CacheNames.TODO_DETAIL, key = "#userId + ':' + #id")
```

第十五周没有重写它们。

原因是：

```text
@Cacheable / @CacheEvict 是 Spring Cache 抽象。
```

它们只表达：

```text
什么时候读缓存
什么时候清缓存
```

并不关心缓存到底在：

```text
本地内存
Redis
Caffeine
其他缓存系统
```

所以第十五周主要改配置，不大改业务代码。

这是一个非常重要的工程化思想：

```text
面向抽象写业务代码，把具体实现放到配置里。
```

## 第十二步：默认环境为什么还要保留 Simple Cache？

因为你当前电脑不一定安装 Redis。

如果项目默认强依赖 Redis，会出现：

```text
没启动 Redis -> 项目启动失败或接口报错
```

这对学习不友好。

所以我们保留：

```properties
spring.cache.type=simple
```

默认体验：

```text
mvn spring-boot:run
```

仍然能跑。

Redis 体验：

```text
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

只有你准备好了 Redis 才启用。

这就是“渐进增强”。

## 第十三步：测试为什么不依赖 Redis？

测试配置：

```text
src/test/resources/application-test.properties
```

保留：

```properties
spring.cache.type=simple
spring.data.redis.repositories.enabled=false
```

原因：

```text
1. 测试应该尽量少依赖外部服务
2. 没装 Redis 也应该能跑测试
3. 当前测试重点是业务缓存规则，不是 Redis 服务本身
```

以后如果要测试 Redis，可以单独加：

```text
Testcontainers Redis
或本地 Redis 集成测试 profile
```

但现在还不需要。先稳住主线。

## 第十四步：为什么新增 Mockito 测试配置？

本周验证测试时，当前 JDK 21 环境里 Mockito 默认的 inline mock maker 尝试动态挂载 Byte Buddy agent，可能出现：

```text
Could not initialize inline Byte Buddy mock maker
Could not self-attach to current VM
```

这个问题不是业务代码错误，也不是 Redis 代码错误，而是测试工具和当前 JVM attach 机制之间的兼容问题。

所以新增：

```text
src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker
```

内容：

```text
mock-maker-subclass
```

含义是：

```text
让 Mockito 使用普通 subclass mock maker，不依赖动态 agent attach。
```

这是测试环境配置，不会影响生产代码。

## 第十五步：现在怎么启动？

### 默认启动，不需要 Redis

```bash
mvn spring-boot:run
```

使用：

```text
H2 文件数据库
Simple Cache 本地内存缓存
```

### 启用 Redis profile

前提：

```text
本机或服务器已经有 Redis，并且端口可访问。
```

启动：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

指定 Redis 地址：

```bash
REDIS_HOST=localhost REDIS_PORT=6379 \
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

如果 Redis 有密码：

```bash
REDIS_HOST=your-host REDIS_PORT=6379 REDIS_PASSWORD=your-password \
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

## 第十六步：当前项目里 Redis 做了什么？

Redis profile 启用后，下面两个缓存会存到 Redis：

```text
todoDetail
todoLogs
```

对应代码仍然在：

```text
src/main/java/com/zading/todoapi/service/TodoService.java
```

Todo 详情：

```java
@Cacheable(cacheNames = CacheNames.TODO_DETAIL, key = "#userId + ':' + #id")
public Todo getTodo(Long userId, Long id) {
}
```

Todo 日志：

```java
@Cacheable(cacheNames = CacheNames.TODO_LOGS, key = "#userId + ':' + #id")
public List<TodoActionLog> getTodoLogs(Long userId, Long id) {
}
```

写操作继续清缓存：

```java
@Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.TODO_DETAIL, key = "#userId + ':' + #id"),
        @CacheEvict(cacheNames = CacheNames.TODO_LOGS, key = "#userId + ':' + #id")
})
```

所以第十五周的变化不是：

```text
重新设计业务逻辑
```

而是：

```text
把缓存存储位置从本地内存扩展到 Redis。
```

## 本周你需要真正掌握的点

### 1. Redis 是外部缓存服务

它不是 Java 应用内部的 Map，而是独立进程。

### 2. Spring Cache 是抽象

`@Cacheable` 和 `@CacheEvict` 不绑定具体缓存实现。

### 3. Profile 用来切换环境配置

默认 Simple Cache。

`redis` profile 使用 Redis Cache。

### 4. TTL 很重要

缓存不能无限期存在。

TTL 可以让缓存自动过期，降低脏数据风险。

### 5. 序列化决定对象怎么存进 Redis

本项目使用 JSON 序列化，方便学习和排查。

### 6. 测试不应该随便依赖外部服务

所以当前测试仍然使用 Simple Cache。

## 本周复盘问题和参考答案

### 1. 第十四周和第十五周最大的区别是什么？

第十四周学习 Spring Cache 注解和缓存失效规则；第十五周把缓存底层扩展成可选 Redis，并学习 TTL、序列化和 profile 配置。

### 2. 引入 Redis 依赖后，为什么默认启动仍然不需要 Redis？

因为默认配置仍然是 `spring.cache.type=simple`，只有启用 `redis` profile 时才会使用 `spring.cache.type=redis`。

### 3. Redis profile 怎么启动？

使用：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

前提是 Redis 服务已经可访问。

### 4. TTL 是什么？

TTL 是 Time To Live，表示缓存存活时间。超过时间后，缓存会自动过期。

### 5. 为什么 `todoLogs` 的 TTL 比 `todoDetail` 短？

因为操作日志更容易随着用户操作变化，短 TTL 可以降低读取旧日志的风险。

### 6. 为什么要关闭 Redis Repository 扫描？

因为当前项目只用 Redis 做缓存，不用 Redis Repository。关闭扫描可以减少误判和无用日志。

### 7. 为什么第十五周不用大改 `TodoService`？

因为第十四周已经使用 Spring Cache 抽象注解。第十五周只更换底层缓存实现，业务注解可以继续复用。

### 8. Simple Cache 和 Redis Cache 分别适合什么？

Simple Cache 适合学习、本地开发、单实例小项目。Redis Cache 适合多实例部署、需要共享缓存、需要 TTL 和更强缓存能力的场景。

### 9. Redis 是不是替代数据库？

不是。数据库仍然保存真实数据，Redis 只是缓存高频读取的数据。

### 10. 为什么测试环境仍然使用 Simple Cache？

为了让测试不依赖 Redis 这类外部服务，保证任何时候都能稳定运行。

### 11. Redis 序列化是干什么的？

它负责把 Java 对象转换成 Redis 可以保存的数据，也负责从 Redis 数据还原成 Java 对象。

### 12. 为什么 Redis key 要用字符串序列化？

字符串 key 更容易阅读和排查。比如 `todoDetail::1:10` 比二进制 key 更直观。

### 13. 如果缓存没有正确失效，会发生什么？

用户可能读到旧数据，比如 Todo 标题已经修改，但详情接口仍返回旧标题。

### 14. 当前项目什么时候会清 Redis 缓存？

调用 `updateTodo`、`toggleTodo`、`deleteTodo`、`restoreTodo` 时，会清理对应 Todo 的详情缓存和日志缓存。

### 15. 本周最重要的工程化思想是什么？

通过 Spring Cache 面向抽象编程，默认环境轻量可运行，生产能力通过 profile 渐进增强。

### 16. 新增的 Mockito mock maker 配置是生产功能吗？

不是。它只在测试资源目录下生效，目的是让本机 JDK 21 环境下的测试更稳定，不影响应用运行和打包。
