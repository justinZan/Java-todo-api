# 第 22 周：单元测试、Mock 和 Service 层测试

## 一、本周目标

第 10 周已经使用 MockMvc 编写过接口测试，第 22 周继续向业务层深入：

```text
接口测试：验证 HTTP 请求和响应
Service 单元测试：验证业务规则和分支逻辑
```

本周新增的测试不启动 Spring Boot，不连接 H2，也不执行 Flyway。Repository、密码编码器、JWT 服务和文件系统依赖会被 Mock 或临时目录替代。

完成本周后，可以独立验证：

- Service 是否返回正确结果
- Service 是否调用了正确的 Repository 方法
- 异常和边界条件是否覆盖
- 用户数据隔离条件是否传递正确
- 事件是否在正确的业务动作后发布
- 不启动数据库时如何测试文件业务

## 二、测试分层

当前项目有两类测试：

```text
src/test/java/com/zading/todoapi/
├── TodoApiTests.java             接口集成测试
├── AuthApiTests.java             认证接口测试
├── RbacApiTests.java             权限接口测试
├── TodoAttachmentApiTests.java   附件接口测试
└── service/
    ├── TodoServiceTest.java
    ├── AuthServiceTest.java
    ├── AdminServiceTest.java
    └── TodoAttachmentServiceTest.java
```

### 接口集成测试

以创建 Todo 为例：

```text
MockMvc
  -> Security Filter
  -> Controller
  -> Service
  -> Repository
  -> H2 数据库
```

它可以验证整个应用协作是否正常，但启动速度较慢，定位问题时需要经过多层代码。

### Service 单元测试

```text
TodoService
  -> Mock TodoRepository
  -> Mock UserRepository
  -> Mock TodoEventPublisher
```

它只关注当前 Service 的业务逻辑，速度快，失败时也更容易定位。

## 三、JUnit 5 测试结构

文件：

```text
src/test/java/com/zading/todoapi/service/TodoServiceTest.java
```

基础结构：

```java
@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @InjectMocks
    private TodoService todoService;
}
```

### `@ExtendWith(MockitoExtension.class)`

让 JUnit 5 在每个测试前初始化 Mockito 注解。如果缺少它，`@Mock` 字段不会自动创建。

### `@Mock`

创建一个假的依赖对象。例如：

```java
@Mock
private TodoRepository todoRepository;
```

这个对象不会真的查询数据库，除非测试明确告诉它应该返回什么。

### `@InjectMocks`

创建被测试的 Service，并把 Mock 依赖注入进去：

```java
@InjectMocks
private TodoService todoService;
```

Mockito 会根据构造函数把以下对象传入：

```text
todoRepository
todoActionLogRepository
userRepository
todoEventPublisher
```

## 四、Mockito 的三个核心动作

### 1. 指定依赖返回值：`when` / `thenReturn`

```java
when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L))
        .thenReturn(Optional.of(todo));
```

含义是：当 Service 调用这条 Repository 方法时，返回指定的 Todo。

### 2. 验证结果：JUnit 断言

```java
Todo actual = todoService.getTodo(1L, 10L);

assertSame(todo, actual);
```

这验证 Service 返回的对象是否是预期对象。

### 3. 验证行为：`verify`

```java
verify(todoRepository)
        .findByIdAndUserIdAndDeletedFalse(10L, 1L);
```

这验证 Service 是否使用了正确的用户 ID 和 Todo ID 查询数据。

单元测试不仅要验证“返回了什么”，还要验证“依赖是怎么被调用的”。

## 五、TodoService 测试

### 1. 测试查询分支

`TodoService.getTodos` 根据参数选择不同的 Repository 方法：

```text
completed + keyword
completed
keyword
无筛选条件
```

测试中传入：

```java
todoService.getTodos(userId, false, "  java  ", pageable);
```

然后验证调用的是：

```java
findByUserIdAndCompletedAndTitleContainingIgnoreCaseAndDeletedFalse(
    userId, false, "java", pageable
)
```

这里同时验证了两件事：

1. 选择了正确的查询分支。
2. 关键词被去掉了前后空格。

### 2. 测试用户隔离

查询详情时，Repository 方法必须同时接收：

```text
todoId
userId
```

测试使用：

```java
when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 2L))
        .thenReturn(Optional.empty());
```

然后断言：

```java
assertThrows(TodoNotFoundException.class, () -> {
    todoService.getTodo(2L, 10L);
});
```

这样可以防止未来有人错误地改成只根据 Todo ID 查询，导致用户之间数据泄露。

### 3. 测试创建 Todo

创建流程是：

```text
查询当前用户
  -> 清理标题空格
  -> 默认 priority 为 MEDIUM
  -> 保存 Todo
  -> 发布 CREATED 事件
```

测试需要准备：

```java
when(userRepository.findById(1L)).thenReturn(Optional.of(user));
when(todoRepository.save(any(Todo.class))).thenReturn(savedTodo);
```

然后验证事件：

```java
verify(todoEventPublisher).publishActionLog(
        savedTodo,
        user,
        TodoAction.CREATED,
        "创建 Todo"
);
```

测试中没有真正写入数据库，但仍然验证了业务动作发生后必须发布操作日志事件。

### 4. 测试修改和完成状态

修改 Todo 时可能同时发生多个业务动作：

```text
修改标题 / 优先级
  -> UPDATED

completed: false -> true
  -> COMPLETED
```

使用 `ArgumentCaptor` 捕获事件参数：

```java
ArgumentCaptor<TodoAction> actionCaptor =
        ArgumentCaptor.forClass(TodoAction.class);

verify(todoEventPublisher, times(2)).publishActionLog(
        eq(todo),
        eq(user),
        actionCaptor.capture(),
        any(String.class)
);
```

之后可以断言动作顺序：

```java
assertEquals(
        List.of(TodoAction.UPDATED, TodoAction.COMPLETED),
        actionCaptor.getAllValues()
);
```

### 5. 测试软删除和恢复

软删除不是从数据库删除记录，而是修改状态：

```text
deleted = true
deletedAt = 当前时间
```

恢复则是：

```text
deleted = false
deletedAt = null
```

单元测试直接检查 Entity 状态，避免只通过 HTTP 响应间接判断。

## 六、AuthService 测试

文件：

```text
src/test/java/com/zading/todoapi/service/AuthServiceTest.java
```

AuthService 的依赖：

```text
UserRepository
PasswordEncoder
JwtService
```

### 注册成功

测试验证：

- 用户名前后空格被清理
- 密码经过 `PasswordEncoder` 编码
- 新用户默认角色为 `USER`
- 用户被保存

```java
when(passwordEncoder.encode("secret"))
        .thenReturn("encoded-secret");
```

### 重复用户名

```java
when(userRepository.existsByUsername("alice"))
        .thenReturn(true);
```

断言抛出 `DUPLICATE_USERNAME`，并验证密码编码器和保存方法没有被调用：

```java
verify(passwordEncoder, never()).encode(any(String.class));
verify(userRepository, never()).save(any(AppUser.class));
```

这类验证很重要，因为重复用户场景不应该继续执行后续业务。

### 登录成功

登录成功不仅要生成 Token，还要把当前角色传给 JWT：

```java
verify(jwtService).generateToken("alice", UserRole.ADMIN);
```

这样可以防止角色字段已经存在，但登录流程仍然生成没有权限信息的旧 Token。

## 七、AdminService 测试

文件：

```text
src/test/java/com/zading/todoapi/service/AdminServiceTest.java
```

测试重点：

- 用户 Entity 能映射为管理员用户 DTO
- 默认只调用 `findByDeletedFalse`
- `includeDeleted=true` 时调用 `findAll`
- 统计数据按 Repository 返回值正确组装

管理员 Service 测试不是测试 Spring Security 的 `hasRole`，因为角色匹配属于安全配置测试范围；这里测试的是管理员业务数据是否正确查询和转换。

## 八、TodoAttachmentService 测试

文件：

```text
src/test/java/com/zading/todoapi/service/TodoAttachmentServiceTest.java
```

### 为什么使用 `@TempDir`

附件 Service 需要真实验证文件创建、下载和删除，但不应该操作项目的 `uploads/` 目录。因此使用 JUnit 临时目录：

```java
@TempDir
private Path tempDir;
```

每个测试都会获得隔离目录，测试结束后由 JUnit 清理。

### 上传测试

使用 `MockMultipartFile` 模拟前端上传：

```java
MultipartFile file = new MockMultipartFile(
        "file",
        "note.txt",
        "text/plain",
        "hello".getBytes(StandardCharsets.UTF_8)
);
```

上传测试验证：

- 原始文件名保存正确
- Content-Type 保存正确
- 文件大小保存正确
- 系统生成了新的存储文件名
- 文件真实写入临时目录
- 附件元数据被保存

### 安全测试

附件文件名不能穿越存储根目录：

```text
../secret.txt
```

测试断言抛出 `BAD_REQUEST`，并且 Repository 没有保存元数据。

### 元数据和文件的一致性

删除附件时需要同时处理：

```text
删除数据库元数据
删除本地文件
```

测试同时验证：

```java
verify(todoAttachmentRepository).delete(attachment);
assertFalse(Files.exists(filePath));
```

## 九、单元测试和集成测试如何配合

两种测试关注点不同：

| 测试类型 | 主要验证内容 |
|---|---|
| Service 单元测试 | 业务分支、异常、依赖调用、数据转换 |
| MockMvc 接口测试 | URL、参数、认证、响应状态、JSON 结构 |
| JPA / 数据库测试 | 查询方法、迁移脚本、真实持久化行为 |
| 完整系统测试 | 多个模块协作和真实运行流程 |

不要用单元测试完全替代接口测试，也不要所有逻辑都只通过接口测试验证。合理的测试结构应该是：

```text
Service 层：大量快速单元测试
Controller 层：少量关键接口测试
数据库层：关键查询和迁移测试
```

## 十、本周测试文件

新增测试：

```text
src/test/java/com/zading/todoapi/service/TodoServiceTest.java
src/test/java/com/zading/todoapi/service/AuthServiceTest.java
src/test/java/com/zading/todoapi/service/AdminServiceTest.java
src/test/java/com/zading/todoapi/service/TodoAttachmentServiceTest.java
```

运行 Service 单元测试：

```bash
mvn -q -Dtest=TodoServiceTest,AuthServiceTest,AdminServiceTest,TodoAttachmentServiceTest test
```

运行全部测试：

```bash
mvn test
```

## 十一、常见问题

### 1. 为什么 Mock Repository 还要写 `when`？

Mock 默认不会返回真实数据。`when(...).thenReturn(...)` 是在告诉测试：当 Service 调用这个依赖时，应该返回什么结果。

### 2. `assert` 和 `verify` 有什么区别？

`assert` 验证测试结果，例如返回值、对象状态和异常；`verify` 验证依赖行为，例如 Repository 是否被调用、参数是否正确、某个方法是否没有被调用。

### 3. 为什么要测试 `never()`？

很多业务错误不只是“抛出异常”，还要求后续动作不能执行。例如用户名重复时不能编码密码，也不能保存用户。`never()` 可以验证流程确实停止。

### 4. 为什么不在每个单元测试中使用 `@SpringBootTest`？

`@SpringBootTest` 会启动完整应用，测试速度慢，并且会把 Controller、数据库、配置等因素带进来。Service 单元测试的目标是隔离当前类，所以使用 Mockito 更合适。

### 5. 为什么附件测试可以使用真实临时文件？

文件系统本身就是附件 Service 的核心依赖。使用 `@TempDir` 可以提供隔离、可清理的真实文件系统，同时不依赖项目正式上传目录。

## 十二、复盘问题与答案

### 1. 单元测试和集成测试的核心区别是什么？

单元测试隔离当前类，依赖用 Mock 替代；集成测试让多个真实组件一起运行，验证模块之间的协作。

### 2. 为什么 Service 层适合单元测试？

Service 层通常包含最多业务分支和异常规则，但不需要 HTTP 和数据库才能验证，因此可以快速、稳定地测试。

### 3. `@Mock` 和 `@InjectMocks` 分别做什么？

`@Mock` 创建假的依赖对象；`@InjectMocks` 创建被测对象并注入这些依赖。

### 4. 为什么要验证用户 ID？

用户 ID 是数据隔离条件。只验证返回值可能无法发现查询条件错误，使用 `verify` 可以确保每次查询都带上正确的用户 ID。

### 5. 什么情况下应该使用 `ArgumentCaptor`？

当需要检查传给依赖的对象内部字段，或者一次调用产生多个不同参数时，可以使用 `ArgumentCaptor` 捕获实际参数。

### 6. 为什么角色信息也要写 Service 测试？

角色属于登录业务的一部分。测试 `AuthService` 可以确保用户的当前角色被传递给 JWT，避免权限功能只在接口测试中被间接覆盖。

### 7. 单元测试越多越好吗？

不是。测试应该覆盖重要业务规则、异常和边界条件，而不是机械地覆盖每一行代码。测试还要保持可读、稳定，并且失败后容易定位。

## 十三、本周产出

完成本周后，项目的测试结构从“主要依赖接口集成测试”升级为：

```text
快速 Service 单元测试
        +
MockMvc 接口测试
        +
H2 / Flyway 集成验证
```

下一周进入第 23 周：查询优化、数据库索引和慢 SQL 排查思维。
