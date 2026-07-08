# 第三周学习说明：控制台 Todo 项目和分层重构

第三周的目标是完成一个控制台版 Todo 项目。

这一周是从“Java 基础练习”走向“项目化学习”的关键阶段。你不再只写零散语法，而是开始组织一个真正的小项目。

## 本周目标

完成后你应该能够：

- 编写控制台交互程序
- 使用分层结构组织代码
- 用 Service 承载业务逻辑
- 用 Repository 管理数据存取
- 使用文件保存 Todo 数据
- 编写基础测试保护业务逻辑

## 项目目标

控制台 Todo 项目支持：

- 查看 Todo 列表
- 新增 Todo
- 修改 Todo
- 删除 Todo
- 切换完成状态
- 保存数据到文件
- 从文件读取数据

## 第一步：设计 Todo 模型

做了什么：

- 新增 `Todo` 类
- 定义 `id`
- 定义 `title`
- 定义 `completed`

示例：

```java
public class Todo {
    private Long id;
    private String title;
    private boolean completed;
}
```

为什么这么做：

- 所有功能都围绕 Todo 数据展开。
- 先定义模型，后面 Service、Repository、UI 都围绕模型工作。
- 这也是后面 Spring Boot API 中 Entity 的前身。

核心理解：

```text
模型是业务数据的中心。
```

## 第二步：编写控制台 UI

做了什么：

- 使用 `Scanner` 读取用户输入
- 显示菜单
- 根据菜单调用不同功能

示例菜单：

```text
1. 查看任务
2. 新增任务
3. 修改任务
4. 删除任务
5. 切换完成状态
0. 退出
```

为什么这么做：

- 控制台 UI 是用户入口。
- 它负责输入和输出，不应该写太多业务逻辑。
- 后面 Spring Boot 的 Controller 其实就是另一种“入口层”。

类比关系：

```text
控制台项目：TodoConsoleUI
Web 项目：TodoController
```

## 第三步：编写 Service 层

做了什么：

- 新增 `TodoService`
- 处理新增、修改、删除、切换完成状态
- 处理标题为空等业务规则

为什么这么做：

- UI 不应该直接操作数据。
- Service 负责业务规则。
- 以后把控制台 UI 换成 REST API，Service 思路仍然成立。

核心分层：

```text
UI
  -> Service
  -> Repository
```

## 第四步：设计 Repository 接口

做了什么：

- 新增 `TodoRepository`
- 定义数据访问方法

示例：

```java
public interface TodoRepository {
    List<Todo> findAll();

    Optional<Todo> findById(Long id);

    Todo save(Todo todo);

    boolean deleteById(Long id);
}
```

为什么这么做：

- Repository 表达“数据从哪里来、保存到哪里去”。
- Service 不需要知道底层是内存、文件还是数据库。
- 第五周切到 JPA 时，这个思想会继续延续。

这一点很重要：

```text
业务层不直接依赖具体存储细节。
```

## 第五步：实现文件存储

做了什么：

- 新增 `FileTodoRepository`
- 把 Todo 保存到文件
- 程序启动时从文件读取

为什么这么做：

- 文件存储让数据在程序退出后还能保留。
- 这是接触数据库前的轻量持久化练习。
- 你会开始理解“内存数据”和“持久化数据”的区别。

持久化含义：

```text
程序停止后，数据仍然存在。
```

## 第六步：处理存储异常

做了什么：

- 新增 `TodoStorageException`
- 文件读取或写入失败时抛出异常

为什么这么做：

- 文件操作可能失败。
- 比如文件不存在、权限不足、格式错误。
- 自定义异常可以让错误含义更明确。

这为后面 Spring Boot 的统一异常处理打基础。

## 第七步：编写测试

做了什么：

- 为 `TodoService` 编写测试
- 使用内存 Repository 做测试替身
- 验证新增、修改、删除、切换状态

为什么这么做：

- Service 是业务核心，值得测试。
- 测试不应该依赖真实文件。
- 使用内存实现可以让测试更快、更稳定。

这也是一个重要工程习惯：

```text
测试业务逻辑时，尽量隔离外部依赖。
```

## 第八步：从 CSV 演进到 JSON

做了什么：

- 早期可以用 CSV 保存简单数据
- 后续升级为 JSON 文件

为什么这么做：

- CSV 简单，但表达复杂结构不方便。
- JSON 更接近前后端接口数据格式。
- 这为第四周理解 REST API 的 JSON 请求/响应做铺垫。

## 第九步：第三周和后续项目的关系

第三周控制台项目里的结构：

```text
TodoConsoleUI
  -> TodoService
  -> TodoRepository
  -> FileTodoRepository
```

第四周 Spring Boot API 里的结构：

```text
TodoController
  -> TodoService
  -> TodoRepository
  -> InMemoryTodoRepository
```

第五周数据库版本里的结构：

```text
TodoController
  -> TodoService
  -> JpaRepository
  -> Database
```

你会发现，虽然入口和存储方式变了，但分层思想一直没变。

## 本周复盘问题

1. 控制台 UI 为什么不应该直接操作 Repository？
2. Service 层应该负责什么？
3. Repository 接口的价值是什么？
4. 文件存储和内存存储有什么区别？
5. 为什么测试 Service 时可以用内存 Repository？
6. 第三周的分层结构和第四周 Spring Boot API 有什么关系？

如果这些问题能说清楚，第三周就真正完成了从“写语法”到“写项目”的转变。

## 本周复盘问题参考答案

### 1. 控制台 UI 为什么不应该直接操作 Repository？

因为 UI 的职责是处理用户输入和输出，不应该承担数据存取和业务规则。

如果 UI 直接操作 Repository，代码会变成：

```text
输入处理 + 业务判断 + 数据存储 全混在一起
```

后续要改成 Web API 时，这些逻辑就很难复用。

更好的结构是：

```text
UI -> Service -> Repository
```

### 2. Service 层应该负责什么？

Service 层负责业务逻辑。

例如：

- 新增 Todo 时标题不能为空。
- 新增 Todo 默认未完成。
- 修改 Todo 前要判断是否存在。
- 切换完成状态时要取反 `completed`。

一句话：

```text
Service 负责“这件事应该怎么做”。
```

### 3. Repository 接口的价值是什么？

Repository 接口把“数据访问能力”抽象出来。

Service 只依赖接口：

```java
TodoRepository todoRepository;
```

不关心具体实现是：

```text
内存
文件
数据库
```

这样存储方式变化时，业务层不需要跟着大改。

### 4. 文件存储和内存存储有什么区别？

内存存储：

```text
程序运行时存在
程序停止后数据丢失
```

文件存储：

```text
数据写入磁盘
程序停止后数据仍然存在
```

文件存储是接触数据库前最轻量的持久化方式。

### 5. 为什么测试 Service 时可以用内存 Repository？

因为测试 Service 的重点是验证业务逻辑，不是验证文件读写。

使用内存 Repository 的好处：

- 更快
- 更稳定
- 不依赖真实文件
- 不会污染本地数据

这就是测试里的“隔离外部依赖”。

### 6. 第三周的分层结构和第四周 Spring Boot API 有什么关系？

第三周是：

```text
TodoConsoleUI -> TodoService -> TodoRepository
```

第四周变成：

```text
TodoController -> TodoService -> TodoRepository
```

变化的是入口层：

```text
控制台 UI 换成 HTTP Controller
```

但 Service 和 Repository 的分层思想保持一致。

## 更多复盘问题

1. 为什么 `TodoConsoleUI` 更像入口层，而不是业务层？
2. 为什么新增 Todo 的标题校验应该放在 Service？
3. `Optional<Todo>` 的作用是什么？
4. 为什么文件保存失败要抛自定义异常？
5. CSV 和 JSON 分别适合什么场景？
6. 为什么 Repository 的 `save` 方法既可以新增也可以修改？
7. 控制台项目重构成 Web API 时，哪些代码最应该被复用？

## 更多复盘问题参考答案

### 1. 为什么 `TodoConsoleUI` 更像入口层，而不是业务层？

`TodoConsoleUI` 负责和用户交互：

- 打印菜单
- 读取输入
- 展示结果

这些都是入口层职责。

业务层职责是判断“新增 Todo 应该怎么做”“标题是否合法”“切换状态怎么处理”。

所以业务逻辑应该放在 Service，而不是 UI。

### 2. 为什么新增 Todo 的标题校验应该放在 Service？

因为标题不能为空是业务规则，不是控制台专属规则。

如果将来入口从控制台换成 REST API，这条规则仍然应该生效。

放在 Service 后，不管入口是：

```text
控制台
HTTP API
测试代码
```

都会走同一套校验逻辑。

### 3. `Optional<Todo>` 的作用是什么？

`Optional<Todo>` 用来表达“可能有 Todo，也可能没有”。

例如：

```java
Optional<Todo> todo = repository.findById(id);
```

这比直接返回 `null` 更明确。

调用方必须处理不存在的情况，代码可读性更好，也能减少空指针问题。

### 4. 为什么文件保存失败要抛自定义异常？

文件保存失败不是正常业务结果，而是存储层发生了错误。

自定义异常可以表达更明确的语义：

```java
throw new TodoStorageException("保存 Todo 失败", e);
```

这样上层看到异常名就知道是存储问题，而不是普通业务错误。

### 5. CSV 和 JSON 分别适合什么场景？

CSV 适合非常简单的表格数据。

优点：

- 简单
- 易读
- 可以用表格工具打开

JSON 适合结构化数据。

优点：

- 能表达对象
- 能表达嵌套结构
- 和前后端接口格式接近

Todo 变复杂后，JSON 比 CSV 更合适。

### 6. 为什么 Repository 的 `save` 方法既可以新增也可以修改？

因为保存动作本身可以根据 id 判断。

常见规则：

```text
id 为空 -> 新增
id 已存在 -> 修改
```

这样 Service 不需要关心底层到底是插入还是覆盖，只调用 `save` 即可。

后面的 JPA `save` 也有类似思想。

### 7. 控制台项目重构成 Web API 时，哪些代码最应该被复用？

最应该复用的是业务逻辑和数据模型。

例如：

- `Todo`
- `TodoService`
- `TodoRepository` 的设计思想

不太需要复用的是控制台输入输出代码，因为 Web API 的入口会变成 Controller。

这就是分层的价值：入口可以换，业务逻辑尽量保留。
