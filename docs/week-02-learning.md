# 第二周学习说明：面向对象、异常、集合和 Maven

第二周的目标是从“会写 Java 语法”升级到“能组织 Java 代码”。

这一周开始接触 Java 项目开发里更常见的能力：

```text
面向对象
集合
异常
包结构
Maven
单元测试
```

这些内容会直接服务于后面的 Todo 控制台项目和 Spring Boot API。

## 本周目标

完成后你应该能够：

- 理解封装、接口、实现类
- 使用 `List`、`Map` 管理数据
- 使用异常表达错误场景
- 理解 `package` 和目录结构
- 使用 Maven 管理 Java 项目
- 运行 JUnit 测试

## 第一步：理解包结构

做了什么：

- 使用 `package`
- 按功能拆分目录

示例：

```java
package com.zading.todo.service;
```

对应目录：

```text
src/main/java/com/zading/todo/service/
```

为什么这么做：

- 包结构用来组织代码。
- 不同职责的类应该放在不同包里。
- 后面的项目会形成 `model`、`service`、`repository`、`controller` 等目录。

你可以把包结构理解成前端项目里的目录模块：

```text
components/
services/
utils/
types/
```

## 第二步：理解封装

做了什么：

- 字段使用 `private`
- 对外提供 getter / setter

示例：

```java
public class Todo {
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
```

为什么这么做：

- 封装可以保护对象内部状态。
- 外部代码不能随意改字段。
- 后续可以在 setter 或业务方法里加入校验逻辑。

核心思路：

```text
对象内部数据不要完全裸露给外部。
```

## 第三步：理解接口和实现类

做了什么：

- 学习 `interface`
- 学习实现类 `implements`

示例：

```java
public interface TodoRepository {
    List<Todo> findAll();
}
```

```java
public class InMemoryTodoRepository implements TodoRepository {
    @Override
    public List<Todo> findAll() {
        return List.of();
    }
}
```

为什么这么做：

- 接口定义能力。
- 实现类提供具体做法。
- 以后可以替换实现，不影响调用方。

这条思路后面非常重要：

```text
Service 依赖 Repository 接口
Repository 可以从内存实现换成数据库实现
```

## 第四步：理解异常

做了什么：

- 使用 `throw`
- 使用 `try/catch`
- 自定义异常

示例：

```java
public class TodoStorageException extends RuntimeException {
    public TodoStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

为什么这么做：

- 异常用来表达程序无法正常完成的场景。
- 比如文件读取失败、Todo 不存在、参数非法。
- 自定义异常可以让错误含义更清晰。

不要把异常理解成“程序崩了”，更准确地说：

```text
异常是一种错误表达机制。
```

## 第五步：使用 Maven 管理项目

做了什么：

- 新建 `pom.xml`
- 使用标准 Maven 目录结构
- 用 Maven 编译、测试、打包

标准结构：

```text
src/main/java      业务代码
src/test/java      测试代码
pom.xml            项目配置
```

常用命令：

```bash
mvn test
mvn package
```

为什么这么做：

- Maven 负责依赖管理。
- Maven 负责统一构建流程。
- 后面的 Spring Boot 项目也是基于 Maven 管理。

前端类比：

```text
pom.xml ≈ package.json
mvn test ≈ npm test
mvn package ≈ npm run build
```

## 第六步：理解单元测试

做了什么：

- 引入 JUnit
- 编写测试类
- 使用断言验证结果

示例：

```java
@Test
void shouldAddTodo() {
    TodoService service = new TodoService(...);
    Todo todo = service.addTodo("学习 Java");

    assertEquals("学习 Java", todo.getTitle());
}
```

为什么这么做：

- 测试可以保护业务逻辑。
- 每次改代码后，可以快速确认没有破坏已有功能。
- 后面做重构、接数据库、加 API 时，测试会越来越重要。

测试的核心不是“为了测试而测试”，而是：

```text
用代码确认业务行为是对的。
```

## 第七步：理解 equals、toString 和调试输出

做了什么：

- 学习 `toString`
- 理解对象打印
- 初步了解对象比较

为什么这么做：

- Java 直接打印对象时默认不够友好。
- `toString` 可以让调试输出更清楚。
- 后面排查 Todo 数据时会更方便。

示例：

```java
@Override
public String toString() {
    return "Todo{id=" + id + ", title='" + title + "'}";
}
```

## 本周复盘问题

1. 为什么字段通常要设置为 `private`？
2. 接口和实现类分别负责什么？
3. 为什么 Service 依赖 Repository 接口会更灵活？
4. 异常适合表达什么场景？
5. Maven 的 `pom.xml` 主要负责什么？
6. 为什么要写单元测试？

如果这些问题能说清楚，第二周就开始有“工程化组织代码”的感觉了。

## 本周复盘问题参考答案

### 1. 为什么字段通常要设置为 `private`？

因为字段是对象的内部状态，不应该被外部代码随意修改。

使用 `private` 后，外部只能通过方法访问或修改字段：

```java
private String title;

public String getTitle() {
    return title;
}
```

好处是：

- 保护对象内部状态。
- 后续可以在方法里加校验。
- 降低代码之间的耦合。

这就是封装。

### 2. 接口和实现类分别负责什么？

接口负责定义能力，实现类负责提供具体实现。

例如：

```java
public interface TodoRepository {
    List<Todo> findAll();
}
```

这表示“我需要一个能查询 Todo 列表的仓库”。

实现类：

```java
public class FileTodoRepository implements TodoRepository {
    // 具体从文件里读取 Todo
}
```

这表示“具体用文件实现这个能力”。

### 3. 为什么 Service 依赖 Repository 接口会更灵活？

因为 Service 只关心“能不能存取 Todo”，不关心底层到底是文件、内存还是数据库。

这样以后可以替换实现：

```text
FileTodoRepository
InMemoryTodoRepository
JpaRepository
```

而 Service 的核心业务逻辑可以少改甚至不改。

这就是“依赖抽象，而不是依赖具体实现”。

### 4. 异常适合表达什么场景？

异常适合表达程序无法按正常流程继续执行的情况。

例如：

- 文件读取失败
- 数据格式错误
- Todo 不存在
- 参数非法

异常不是单纯的“程序崩溃”，它是一种错误表达机制。

好的异常应该让错误原因更清楚。

### 5. Maven 的 `pom.xml` 主要负责什么？

`pom.xml` 是 Maven 项目的配置文件，主要负责：

- 项目信息
- Java 版本
- 依赖管理
- 测试配置
- 打包配置
- 插件配置

前端类比：

```text
pom.xml ≈ package.json
```

### 6. 为什么要写单元测试？

单元测试用来验证一小块业务逻辑是否正确。

好处是：

- 改代码后能快速确认有没有破坏已有功能。
- 重构时更有安全感。
- 业务规则可以用代码固定下来。

比如 Todo 标题不能为空，这种规则就适合用测试保护。

## 更多复盘问题

1. 为什么要按 `model`、`service`、`repository` 拆包？
2. `@Override` 的作用是什么？
3. `RuntimeException` 和普通返回错误值有什么区别？
4. 为什么测试代码要放在 `src/test/java`？
5. Maven 为什么要有标准目录结构？
6. 什么情况下适合写接口？
7. 为什么不要把所有代码都写在一个类里？

## 更多复盘问题参考答案

### 1. 为什么要按 `model`、`service`、`repository` 拆包？

因为不同代码职责不同。

常见分工：

```text
model       数据模型
service     业务逻辑
repository  数据访问
```

拆包后，代码更容易找，也更容易维护。

如果所有类都放在一个目录里，项目稍微变大就会乱。

### 2. `@Override` 的作用是什么？

`@Override` 表示当前方法是在重写父类或接口中的方法。

例如：

```java
@Override
public List<Todo> findAll() {
    return todos;
}
```

好处是编译器会帮你检查方法签名是否正确。

如果方法名写错了，编译器会报错。

这比运行时才发现问题安全很多。

### 3. `RuntimeException` 和普通返回错误值有什么区别？

普通返回错误值需要调用方每次都检查。

异常可以中断当前正常流程，把错误交给上层统一处理。

例如文件保存失败时，直接返回 `false` 信息太少，而抛出异常可以带上错误原因：

```java
throw new TodoStorageException("保存 Todo 失败", e);
```

异常更适合表达“当前流程无法继续”的情况。

### 4. 为什么测试代码要放在 `src/test/java`？

这是 Maven 标准目录结构。

```text
src/main/java  业务代码
src/test/java  测试代码
```

这样 Maven 能自动识别哪些代码用于正式打包，哪些代码只用于测试。

测试代码不会被当成应用业务代码发布出去。

### 5. Maven 为什么要有标准目录结构？

标准目录结构让工具和团队形成统一约定。

好处是：

- 新人容易看懂项目。
- Maven 不需要额外配置就知道哪里是源码、哪里是测试。
- IDE 能更准确识别项目。
- 后续 Spring Boot 项目也沿用同样结构。

约定比配置更省心。

### 6. 什么情况下适合写接口？

当你希望“定义能力”和“具体实现”分开时，适合写接口。

比如 Repository：

```text
能力：保存 Todo、查询 Todo
实现：内存、文件、数据库
```

如果未来可能替换实现，接口就很有价值。

如果只有一个非常简单、不会替换的类，也不一定非要写接口。

### 7. 为什么不要把所有代码都写在一个类里？

因为一个类承担太多职责会变得难读、难测、难改。

比如一个类同时负责：

```text
读取输入
处理业务
保存文件
打印结果
```

它很快会变成“万能类”，后续修改任何功能都可能影响其它功能。

拆分职责后，每个类只做好一件事，项目更稳定。
