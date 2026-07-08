# 第一周学习说明：Java 基础和开发环境

第一周的目标是把 Java 开发环境搭起来，并能写出最基础的 Java 程序。

这一周不追求写复杂项目，重点是建立三个感觉：

```text
1. Java 程序如何编译和运行
2. Java 基础语法如何组织代码
3. 面向对象为什么是 Java 的核心思维
```

## 本周目标

完成后你应该能够：

- 安装并确认 JDK 21
- 理解 `java` 和 `javac` 的区别
- 使用 VS Code 编写 Java 代码
- 写出简单的 Java 类和方法
- 理解变量、条件、循环、数组、集合
- 初步理解类、对象、字段、方法、构造函数

## 第一步：安装并确认 JDK 21

做了什么：

- 安装 JDK 21
- 配置 `JAVA_HOME`
- 确认 `java -version`
- 确认 `javac -version`

为什么这么做：

- JDK 是 Java 开发工具包，没有 JDK 就无法编译 Java 代码。
- `java` 用来运行程序。
- `javac` 用来编译 `.java` 文件。
- `JAVA_HOME` 让 Maven、VS Code 等工具知道 JDK 在哪里。

常用检查命令：

```bash
java -version
javac -version
echo $JAVA_HOME
```

你要理解：

```text
.java 文件
  -> javac 编译
  -> .class 字节码
  -> java 运行
```

## 第二步：写第一个 Java 程序

做了什么：

- 新建 `HelloWorld.java`
- 编写 `main` 方法
- 编译并运行

示例：

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello Java");
    }
}
```

为什么这么做：

- `main` 方法是普通 Java 程序的入口。
- JVM 从 `main` 方法开始执行代码。
- `System.out.println` 是最基础的控制台输出方式。

你要先记住这个固定入口：

```java
public static void main(String[] args)
```

不用一开始就完全拆解每个关键字，先知道它是程序入口。

## 第三步：理解变量和类型

做了什么：

- 学习基本类型
- 学习字符串
- 学习变量声明和赋值

常见类型：

```java
int count = 1;
long id = 100L;
boolean completed = false;
double price = 12.5;
String title = "学习 Java";
```

为什么这么做：

- Java 是静态类型语言。
- 每个变量在声明时都需要明确类型。
- 类型决定了变量能保存什么数据，也决定了能做什么操作。

前端类比：

```ts
let title: string = '学习 Java'
let completed: boolean = false
```

Java 比 JavaScript 更严格，更接近 TypeScript 的类型思路。

## 第四步：理解条件和循环

做了什么：

- 学习 `if`
- 学习 `switch`
- 学习 `for`
- 学习 `while`

示例：

```java
if (completed) {
    System.out.println("已完成");
} else {
    System.out.println("未完成");
}
```

为什么这么做：

- 条件判断用来表达分支。
- 循环用来处理重复任务。
- 后面的 Todo 项目会大量用到这些基础控制结构。

比如：

```java
for (Todo todo : todos) {
    System.out.println(todo.getTitle());
}
```

这是后面遍历 Todo 列表的基础。

## 第五步：理解方法

做了什么：

- 学习定义方法
- 学习参数
- 学习返回值

示例：

```java
public static String normalizeTitle(String title) {
    return title.trim();
}
```

为什么这么做：

- 方法用来封装一段可复用逻辑。
- 参数是输入。
- 返回值是输出。
- 后面的 Service 层本质上就是很多业务方法的集合。

你可以这样理解：

```text
方法 = 给一段逻辑起名字
```

## 第六步：理解类和对象

做了什么：

- 学习 `class`
- 学习字段
- 学习构造函数
- 学习 getter / setter

示例：

```java
public class Todo {
    private Long id;
    private String title;
    private boolean completed;

    public Todo(Long id, String title, boolean completed) {
        this.id = id;
        this.title = title;
        this.completed = completed;
    }
}
```

为什么这么做：

- Java 是面向对象语言。
- 类是对象的模板。
- 对象是具体的数据。
- Todo 项目里的每一条任务，最终都会变成一个 `Todo` 对象。

前端类比：

```ts
type Todo = {
  id: number
  title: string
  completed: boolean
}
```

但 Java 的类除了描述数据，还可以包含构造函数和方法。

## 第七步：理解集合

做了什么：

- 学习 `List`
- 学习 `ArrayList`
- 学习 `Map`

示例：

```java
List<String> titles = new ArrayList<>();
titles.add("学习 Java");
titles.add("写 Todo 项目");
```

为什么这么做：

- 真实业务很少只处理一条数据。
- Todo 列表需要用集合保存。
- 后面的内存版 Repository 会用 `Map<Long, Todo>` 保存 Todo。

常见集合：

```text
List  有序列表
Map   key-value 映射
Set   不重复集合
```

## 本周复盘问题

1. JDK 和 JRE 有什么区别？
2. `java` 和 `javac` 分别做什么？
3. 为什么 Java 变量需要声明类型？
4. 类和对象有什么区别？
5. `List<Todo>` 表示什么？

如果这些问题能说清楚，第一周就算打好了 Java 入门地基。

## 本周复盘问题参考答案

### 1. JDK 和 JRE 有什么区别？

JDK 是 Java Development Kit，包含开发 Java 程序需要的工具，比如 `javac` 编译器、`java` 运行命令、标准类库等。

JRE 是 Java Runtime Environment，只负责运行 Java 程序，不包含完整开发工具。

简单理解：

```text
写 Java 程序需要 JDK
只运行 Java 程序可以只需要 JRE
```

现在学习和开发 Java，直接安装 JDK 21 即可。

### 2. `java` 和 `javac` 分别做什么？

`javac` 负责编译：

```text
HelloWorld.java -> HelloWorld.class
```

`java` 负责运行：

```text
java HelloWorld
```

也就是：

```text
javac 把源码变成字节码
java 把字节码交给 JVM 执行
```

### 3. 为什么 Java 变量需要声明类型？

因为 Java 是静态类型语言。变量声明时就要明确它能保存什么类型的数据。

例如：

```java
String title = "学习 Java";
boolean completed = false;
```

好处是：

- 编译阶段就能发现类型错误。
- IDE 可以给出更准确的提示。
- 代码结构更稳定，适合大型工程。

你可以把它类比成 TypeScript，而不是 JavaScript。

### 4. 类和对象有什么区别？

类是模板，对象是根据模板创建出来的具体实例。

例如：

```java
public class Todo {
    private String title;
}
```

`Todo` 是类。

```java
Todo todo = new Todo();
```

`todo` 是对象。

类描述“有什么字段、有什么方法”，对象保存“具体的数据”。

### 5. `List<Todo>` 表示什么？

`List<Todo>` 表示一个 Todo 列表，里面的每个元素都是 `Todo` 对象。

例如：

```java
List<Todo> todos = new ArrayList<>();
```

含义是：

```text
todos 是一个列表
列表里只能放 Todo 类型的数据
```

它会在后面的 Todo 列表查询、遍历、返回 JSON 时反复出现。

## 更多复盘问题

1. `String` 和 `int` 这类类型有什么区别？
2. `if` 和 `for` 分别适合解决什么问题？
3. 方法为什么要有参数和返回值？
4. 构造函数的作用是什么？
5. 为什么 Java 代码通常要放在类里面？
6. `ArrayList` 和数组有什么区别？
7. 为什么学习 Java 时要先理解对象？

## 更多复盘问题参考答案

### 1. `String` 和 `int` 这类类型有什么区别？

`int` 是基本类型，用来保存整数。

`String` 是引用类型，用来保存字符串对象。

示例：

```java
int count = 10;
String title = "学习 Java";
```

简单理解：

```text
int 保存简单数值
String 保存文本对象
```

后面你会看到，`Todo` 也是引用类型，因为它是一个对象。

### 2. `if` 和 `for` 分别适合解决什么问题？

`if` 用来处理条件分支。

比如：

```java
if (todo.isCompleted()) {
    System.out.println("已完成");
}
```

`for` 用来处理重复操作。

比如：

```java
for (Todo todo : todos) {
    System.out.println(todo.getTitle());
}
```

一句话：

```text
if 管判断
for 管重复
```

### 3. 方法为什么要有参数和返回值？

参数是方法的输入，返回值是方法的输出。

例如：

```java
public String normalizeTitle(String title) {
    return title.trim();
}
```

这里 `title` 是输入，处理后的字符串是输出。

方法有参数和返回值后，就可以把一段逻辑封装成可复用的小功能。

### 4. 构造函数的作用是什么？

构造函数用来创建对象时初始化数据。

例如：

```java
public Todo(Long id, String title, boolean completed) {
    this.id = id;
    this.title = title;
    this.completed = completed;
}
```

创建对象时：

```java
Todo todo = new Todo(1L, "学习 Java", false);
```

对象一创建就有了初始状态。

### 5. 为什么 Java 代码通常要放在类里面？

Java 是面向对象语言，大多数代码都属于某个类。

类可以包含：

- 字段
- 构造函数
- 方法

所以 Java 程序不是随便写几行脚本，而是通过类来组织代码。

这也是 Java 更适合大型工程的原因之一。

### 6. `ArrayList` 和数组有什么区别？

数组长度固定：

```java
String[] names = new String[3];
```

`ArrayList` 长度可以动态变化：

```java
List<String> names = new ArrayList<>();
names.add("Java");
```

在业务开发里，`List` 和 `ArrayList` 更常用，因为数据数量通常不是固定的。

### 7. 为什么学习 Java 时要先理解对象？

因为 Java 项目里的核心数据通常都会建模成对象。

比如：

```text
Todo
User
Order
Product
```

后面的 Controller、Service、Repository 也都是类和对象。

如果对象理解清楚，后面的 Spring Boot 项目会顺很多。
