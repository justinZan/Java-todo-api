# 第十四周学习说明：缓存、接口性能和查询优化

第十四周的目标是理解缓存，并在 Todo API 里实现第一层缓存优化。

前面项目已经有：

```text
Todo CRUD
用户注册 / 登录
JWT 鉴权
分页 / 排序 / 筛选
软删除 / 恢复
统一响应
操作日志
事务
测试
CI
```

这一周开始接触“性能意识”。

最核心的一句话：

```text
缓存不是为了炫技，而是为了减少重复查询；但只要数据会变，就必须处理缓存失效。
```

## 本周最终改动总览

| 改动 | 文件 | 目的 |
|---|---|---|
| 引入 Spring Cache | `pom.xml` | 使用 Spring 提供的缓存抽象 |
| 开启缓存功能 | `TodoApiApplication.java` | 让 `@Cacheable` / `@CacheEvict` 生效 |
| 新增缓存名常量 | `CacheNames.java` | 集中管理缓存名称，避免字符串写散 |
| 配置 Simple Cache | `application.properties` / `application-test.properties` | 使用本地内存缓存，不依赖 Redis |
| 缓存 Todo 详情 | `TodoService.java` | `getTodo` 查询结果进入缓存 |
| 缓存 Todo 操作日志 | `TodoService.java` | `getTodoLogs` 查询结果进入缓存 |
| 写操作清理缓存 | `TodoService.java` | 修改、完成、删除、恢复后清理旧缓存 |
| 测试缓存行为 | `TodoApiTests.java` | 验证查询会写入缓存，写操作会清理缓存 |
| 测试清理缓存 | `AbstractApiTest.java` | 每个测试前清理缓存，避免测试互相影响 |
| 更新 README | `README.md` | 补充工程缓存说明 |

## 第一步：缓存是什么？

缓存可以先理解成：

```text
把经常读取的数据放到更快的位置。
```

比如查询 Todo 详情：

```text
GET /api/todos/1
```

如果每次都查数据库：

```text
请求 1 -> 查数据库
请求 2 -> 查数据库
请求 3 -> 查数据库
```

如果加缓存：

```text
请求 1 -> 查数据库 -> 放入缓存
请求 2 -> 读缓存
请求 3 -> 读缓存
```

这样可以减少数据库压力。

## 第二步：缓存适合什么数据？

缓存更适合：

```text
读多写少
计算成本高
查询频率高
短时间内结果不容易变
```

比如：

```text
Todo 详情
Todo 操作日志
用户信息
配置字典
权限菜单
```

不太适合随便缓存：

```text
频繁变化的数据
强一致要求极高的数据
每个请求结果都不同的数据
```

比如余额、库存这类数据，缓存要更谨慎。

## 第三步：为什么这周不用 Redis？

Redis 是常见缓存中间件，但这周先不用。

原因是：

```text
1. 你本地还没有安装 Redis
2. 本周重点是理解缓存思想，不是先安装工具
3. Spring Cache 可以先用本地内存缓存跑起来
```

当前配置：

```properties
spring.cache.type=simple
```

这表示使用 Spring Boot 自带的 Simple Cache。

Simple Cache 是本地内存缓存：

```text
应用启动后缓存存在
应用重启后缓存消失
多个应用实例之间不共享
不需要安装额外服务
```

它适合学习缓存注解和失效规则。

等以后学 Redis，只需要把底层缓存实现换掉，很多业务代码不用大改。

## 第四步：引入 Spring Cache

文件：

```text
pom.xml
```

新增依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

这个依赖提供了 Spring 的缓存抽象。

所谓缓存抽象，就是：

```text
业务代码使用统一注解
底层可以是 Simple Cache、Redis、Caffeine 等不同实现
```

这就像前端里你写统一请求封装，底层可能是 fetch，也可能是 axios。

## 第五步：开启缓存

文件：

```text
src/main/java/com/zading/todoapi/TodoApiApplication.java
```

新增：

```java
@EnableCaching
@SpringBootApplication
public class TodoApiApplication {
}
```

`@EnableCaching` 的作用是：

```text
开启 Spring 缓存功能。
```

如果没有它，`@Cacheable`、`@CacheEvict` 这些注解不会真正生效。

简单记：

```text
引入依赖只是把能力拿进项目
@EnableCaching 才是打开开关
```

## 第六步：为什么新增 CacheNames？

文件：

```text
src/main/java/com/zading/todoapi/config/CacheNames.java
```

内容：

```java
public final class CacheNames {
    public static final String TODO_DETAIL = "todoDetail";
    public static final String TODO_LOGS = "todoLogs";

    private CacheNames() {
    }
}
```

为什么不在注解里直接写字符串？

比如：

```java
@Cacheable(cacheNames = "todoDetail")
```

这样也能跑。

但是项目大了以后，很容易出现：

```text
todoDetail
todoDetails
todo-detail
todo_detail
```

这些字符串看起来差不多，其实是不同缓存。

所以我们集中管理：

```java
CacheNames.TODO_DETAIL
CacheNames.TODO_LOGS
```

这是一种小但很实用的工程习惯。

## 第七步：缓存 Todo 详情

文件：

```text
src/main/java/com/zading/todoapi/service/TodoService.java
```

代码：

```java
@Transactional(readOnly = true)
@Cacheable(cacheNames = CacheNames.TODO_DETAIL, key = "#userId + ':' + #id")
public Todo getTodo(Long userId, Long id) {
    return todoRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
            .orElseThrow(() -> new TodoNotFoundException(id));
}
```

`@Cacheable` 的作用是：

```text
先查缓存。
缓存有：直接返回缓存结果。
缓存没有：执行方法，查数据库，然后把返回值放进缓存。
```

也就是说第一次查询：

```text
GET /api/todos/1
  -> 缓存没有
  -> 执行 getTodo
  -> 查数据库
  -> 返回结果
  -> 放进 todoDetail 缓存
```

第二次查询：

```text
GET /api/todos/1
  -> 缓存有
  -> 直接返回缓存
  -> 不执行 getTodo 方法体
```

## 第八步：缓存 key 为什么是 `userId:id`？

代码：

```java
key = "#userId + ':' + #id"
```

比如：

```text
userId = 1
todoId = 10
缓存 key = "1:10"
```

为什么要带 `userId`？

因为 Todo 是按用户隔离的。

如果只用：

```java
key = "#id"
```

虽然现在数据库 id 全局唯一，短期也能跑，但从业务语义上不够明确。

带上 `userId` 后，缓存表达的是：

```text
用户 1 的 Todo 10
用户 2 的 Todo 10
```

这对多用户系统更安全、更清楚。

## 第九步：缓存 Todo 操作日志

代码：

```java
@Transactional(readOnly = true)
@Cacheable(cacheNames = CacheNames.TODO_LOGS, key = "#userId + ':' + #id")
public List<TodoActionLog> getTodoLogs(Long userId, Long id) {
    todoRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new TodoNotFoundException(id));

    return todoActionLogRepository.findByTodoIdAndUserIdOrderByCreatedAtAscIdAsc(id, userId);
}
```

日志查询适合缓存的原因：

```text
1. 日志通常是读多写少
2. 日志列表可能越来越长
3. 前端打开详情页时可能重复查看操作历史
```

但日志会变化。

比如：

```text
完成 Todo
删除 Todo
恢复 Todo
```

这些动作都会新增日志。

所以写操作后必须清理日志缓存。

## 第十步：什么是缓存失效？

缓存失效就是：

```text
数据变了，把旧缓存删掉。
```

比如缓存里有：

```json
{
  "id": 1,
  "title": "旧标题"
}
```

然后用户修改标题：

```text
PATCH /api/todos/1
title = 新标题
```

如果不清缓存，下一次查询可能还返回：

```text
旧标题
```

这就是缓存脏数据。

所以修改后要清缓存。

## 第十一步：使用 `@CacheEvict` 清理缓存

本周写操作都加了：

```java
@Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.TODO_DETAIL, key = "#userId + ':' + #id"),
        @CacheEvict(cacheNames = CacheNames.TODO_LOGS, key = "#userId + ':' + #id")
})
```

`@CacheEvict` 的作用：

```text
从指定缓存中删除指定 key。
```

`@Caching` 的作用：

```text
把多个缓存操作组合在一起。
```

为什么要同时清两个？

因为一次写操作可能影响两个接口：

```text
Todo 详情接口
Todo 日志接口
```

例如修改标题：

```text
Todo 详情变了
Todo 日志新增 UPDATED
```

所以要清：

```text
todoDetail
todoLogs
```

## 第十二步：哪些方法清理缓存？

当前这些方法会清缓存：

```java
updateTodo(...)
toggleTodo(...)
deleteTodo(...)
restoreTodo(...)
```

它们都会改变 Todo 或日志。

对应业务影响：

| 方法 | 改变 Todo 详情 | 新增日志 | 是否清缓存 |
|---|---:|---:|---:|
| `updateTodo` | 是 | 可能 | 是 |
| `toggleTodo` | 是 | 是 | 是 |
| `deleteTodo` | 是 | 是 | 是 |
| `restoreTodo` | 是 | 是 | 是 |

`addTodo` 当前没有清缓存。

为什么？

因为新 Todo 创建前还没有 `todoId`，一般也不会已经存在这个详情缓存。

## 第十三步：为什么没有缓存 Todo 列表？

Todo 列表接口参数比较多：

```text
completed
keyword
page
size
sort
```

如果缓存列表，就要考虑很多 key：

```text
userId:completed:keyword:page:size:sort
```

而且列表很容易被写操作影响：

```text
创建 Todo 会影响列表
修改标题会影响关键词搜索结果
完成 Todo 会影响 completed 筛选结果
删除 Todo 会影响总数
恢复 Todo 也会影响总数
```

这会让缓存失效规则变复杂。

所以第十四周先缓存：

```text
Todo 详情
Todo 操作日志
```

这是更稳妥的学习路径。

一句话：

```text
先缓存 key 简单、影响范围小的数据。
```

## 第十四步：测试怎么验证缓存？

文件：

```text
src/test/java/com/zading/todoapi/TodoApiTests.java
```

新增测试：

```java
shouldCacheTodoDetailAndEvictWhenUpdated()
shouldCacheTodoLogsAndEvictWhenActionChanges()
```

测试思路：

```text
1. 创建 Todo
2. 确认缓存里还没有 key
3. 调用查询接口
4. 确认缓存里有 key
5. 调用写接口
6. 确认缓存 key 被清掉
```

示意：

```java
Cache todoDetailCache = cacheManager.getCache(CacheNames.TODO_DETAIL);
assertNull(todoDetailCache.get(cacheKey));

mockMvc.perform(get("/api/todos/{id}", todoId));

assertNotNull(todoDetailCache.get(cacheKey));

mockMvc.perform(patch("/api/todos/{id}", todoId));

assertNull(todoDetailCache.get(cacheKey));
```

这不是在测试“速度变快多少”。

这是在测试：

```text
缓存规则是否正确生效。
```

## 第十五步：为什么测试前要清缓存？

文件：

```text
src/test/java/com/zading/todoapi/support/AbstractApiTest.java
```

新增：

```java
clearCache(CacheNames.TODO_DETAIL);
clearCache(CacheNames.TODO_LOGS);
```

原因是：

```text
缓存是内存状态。
如果不清理，前一个测试留下的缓存可能影响后一个测试。
```

测试应该尽量互相独立。

这和清数据库是一个思想：

```text
数据库要清
缓存也要清
```

## 第十六步：缓存和事务有什么关系？

本项目里写操作同时有：

```java
@Transactional
@Caching(...)
```

事务负责：

```text
数据库操作要么一起成功，要么一起失败。
```

缓存清理负责：

```text
数据库变了以后，不要继续使用旧缓存。
```

它们解决的问题不同。

可以这样理解：

```text
事务保护数据库一致性。
缓存失效保护接口读取一致性。
```

## 第十七步：前端怎么理解缓存？

前端不需要知道后端有没有缓存。

接口还是原来的接口：

```http
GET /api/todos/{id}
GET /api/todos/{id}/logs
```

响应格式也还是统一响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "成功",
  "data": {}
}
```

缓存是后端内部优化。

前端真正关心的是：

```text
数据不能旧
接口不能乱
响应结构不能变
```

所以缓存优化应该尽量不改变接口契约。

## 第十八步：本地缓存和 Redis 有什么区别？

当前 Simple Cache：

```text
存在 Java 应用内存里
应用重启后消失
多个后端实例不共享
适合学习和单体小项目
```

Redis：

```text
独立缓存服务
应用重启后缓存仍可存在
多个后端实例可以共享
支持过期时间、更多数据结构、分布式锁等能力
```

后面如果上 Redis，学习重点会变成：

```text
缓存过期时间 TTL
序列化
缓存穿透
缓存击穿
缓存雪崩
分布式缓存一致性
```

第十四周先把基础打稳，不急着把工具箱全倒桌上。

## 本周你需要真正掌握的点

### 1. `@EnableCaching`

开启 Spring 缓存功能。

### 2. `@Cacheable`

读缓存。

缓存没有才执行方法，执行后把返回值放入缓存。

### 3. `@CacheEvict`

清缓存。

数据变化后，删除旧缓存。

### 4. `@Caching`

组合多个缓存操作。

比如一次写操作同时清理 Todo 详情缓存和日志缓存。

### 5. 缓存 key

缓存 key 要能唯一表达这份数据。

本项目使用：

```text
userId:todoId
```

### 6. 不是什么都应该缓存

缓存越多，失效规则越复杂。

先缓存最容易控制的数据。

## 本周复盘问题和参考答案

### 1. 缓存的作用是什么？

缓存的作用是减少重复查询，把经常读取的数据放到更快的位置，从而降低数据库压力、提升接口响应速度。

### 2. `@EnableCaching` 的作用是什么？

它开启 Spring 的缓存功能。没有它，`@Cacheable`、`@CacheEvict` 这些注解不会真正生效。

### 3. `@Cacheable` 是怎么工作的？

先查缓存。如果缓存存在，直接返回缓存数据；如果不存在，执行方法体，拿到返回值后写入缓存。

### 4. `@CacheEvict` 是做什么的？

它用于清理缓存。数据发生变化后，应该删除旧缓存，避免后续查询读到过期数据。

### 5. 为什么缓存 key 要带 `userId`？

因为 Todo 数据按用户隔离。带 `userId` 可以让缓存语义更清楚，避免不同用户之间的数据混淆。

### 6. 为什么本周没有缓存 Todo 列表？

列表查询参数多，受创建、修改、完成、删除、恢复等操作影响，缓存失效规则更复杂。本周先缓存详情和日志，更适合学习。

### 7. 为什么写操作要同时清理 `todoDetail` 和 `todoLogs`？

因为写操作通常既改变 Todo 当前状态，又新增操作日志。两个接口的数据都可能受影响。

### 8. Simple Cache 和 Redis 有什么区别？

Simple Cache 是本地内存缓存，不需要额外安装服务，但应用重启后消失，也不能跨实例共享。Redis 是独立缓存服务，可以被多个应用实例共享，能力更强。

### 9. 缓存和事务分别解决什么问题？

事务解决数据库写入的一致性；缓存失效解决读取时不要拿到旧数据的问题。

### 10. 为什么测试前要清缓存？

因为缓存是内存状态。如果不清理，前一个测试留下的缓存可能影响后一个测试，导致测试不独立。

### 11. 缓存是不是越多越好？

不是。缓存越多，失效规则越复杂。如果失效处理不好，用户会看到旧数据。

### 12. 什么时候适合使用缓存？

读多写少、查询频率高、短时间内结果不容易变化、计算或查询成本较高的数据适合缓存。

### 13. `@Caching` 的作用是什么？

它可以组合多个缓存注解。例如一次写操作同时清理 `todoDetail` 和 `todoLogs`。

### 14. 后端加缓存会影响前端接口格式吗？

不应该影响。缓存是后端内部优化，前端看到的接口路径、响应结构和业务字段应该保持稳定。

### 15. 本周最重要的工程化思想是什么？

缓存一定要和失效规则一起设计。只加缓存、不考虑数据变化，是非常容易制造线上 bug 的。
