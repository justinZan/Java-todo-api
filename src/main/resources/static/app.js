const weeks = [
    { week: 1, stage: "Java 基础", title: "Java 程序结构与基础语法", focus: "理解类、main 方法、变量、类型、条件和循环怎样组成一个可运行程序。", files: ["docs/week-01-learning.md", "src/main/java/com/zading/todoapi/TodoApiApplication.java"], deliverable: "写出并运行一个包含输入、判断和方法调用的小程序。" },
    { week: 2, stage: "Java 基础", title: "面向对象、异常、集合与 Maven", focus: "把数据和行为放进对象，使用 List 管理多个对象，并为失败场景设计异常。", files: ["docs/week-02-learning.md", "src/main/java/com/zading/todoapi/model/Todo.java", "pom.xml"], deliverable: "能解释类、接口、集合、异常和 Maven 在项目中的职责。" },
    { week: 3, stage: "Java 基础", title: "Todo 项目分层", focus: "理解 controller、service、repository、model 的职责边界。", files: ["docs/week-03-learning.md", "src/main/java/com/zading/todoapi/controller/TodoController.java", "src/main/java/com/zading/todoapi/service/TodoService.java"], deliverable: "沿一条 Todo 请求说清楚每一层做了什么。" },
    { week: 4, stage: "Web API", title: "Spring Boot REST API", focus: "理解应用启动、路由映射、JSON 请求和 HTTP 响应。", files: ["docs/week-04-learning.md", "src/main/java/com/zading/todoapi/TodoApiApplication.java", "src/main/java/com/zading/todoapi/controller/TodoController.java"], deliverable: "新增一个可以通过浏览器或 curl 调用的 REST 接口。" },
    { week: 5, stage: "Web API", title: "JPA 与数据库", focus: "理解 Entity、Repository、主键和 Spring Data 方法名查询。", files: ["docs/week-05-learning.md", "src/main/java/com/zading/todoapi/model/Todo.java", "src/main/java/com/zading/todoapi/repository/TodoRepository.java"], deliverable: "写出一个 Repository 查询，并解释对应的 SQL 条件。" },
    { week: 6, stage: "Web API", title: "Profile、Flyway 与 DTO", focus: "把环境配置、数据库迁移和接口模型分开管理。", files: ["docs/week-06-learning.md", "src/main/resources/application.properties", "src/main/resources/db/migration", "src/main/java/com/zading/todoapi/dto/CreateTodoRequest.java"], deliverable: "新增一个迁移或 DTO 字段，并完成验证。" },
    { week: 7, stage: "Web API", title: "分页、排序与参数校验", focus: "控制列表接口的数据量、排序字段和非法参数。", files: ["docs/week-07-learning.md", "src/main/java/com/zading/todoapi/controller/TodoController.java", "src/main/java/com/zading/todoapi/controller/TodoSortParser.java"], deliverable: "完成一个带分页、排序和边界校验的查询。" },
    { week: 8, stage: "Web API", title: "注册、登录、JWT 与用户隔离", focus: "理解 Token 如何进入 SecurityContext，以及查询为什么必须带 userId。", files: ["docs/week-08-learning.md", "src/main/java/com/zading/todoapi/security/JwtAuthenticationFilter.java", "src/main/java/com/zading/todoapi/security/SecurityConfig.java"], deliverable: "证明一个用户无法读取另一个用户的 Todo。" },
    { week: 9, stage: "工程基础", title: "OpenAPI、请求日志与 requestId", focus: "让接口可发现，让一次请求可以通过 requestId 在日志中追踪。", files: ["docs/week-09-learning.md", "src/main/java/com/zading/todoapi/config/OpenApiConfig.java", "src/main/java/com/zading/todoapi/logging/RequestLoggingFilter.java"], deliverable: "从一次接口调用中找到对应 requestId 和日志记录。" },
    { week: 10, stage: "工程基础", title: "自动化测试与持续验证", focus: "区分单元测试和接口测试，让修改获得快速反馈。", files: ["docs/week-10-learning.md", "src/test/java/com/zading/todoapi/TodoApiTests.java", "pom.xml"], deliverable: "补充一条成功测试和一条失败测试。" },
    { week: 11, stage: "工程基础", title: "软删除与数据生命周期", focus: "区分删除、恢复和物理清理，并让普通查询过滤已删除数据。", files: ["docs/week-11-learning.md", "src/main/java/com/zading/todoapi/service/TodoService.java", "src/main/resources/db/migration/V4__add_todo_lifecycle_fields.sql"], deliverable: "完成删除与恢复测试，确认列表不会泄露已删除数据。" },
    { week: 12, stage: "工程基础", title: "统一响应、错误码与全局异常", focus: "把不同异常转换成前端可以稳定处理的 HTTP 状态和错误结构。", files: ["docs/week-12-learning.md", "src/main/java/com/zading/todoapi/exception/GlobalExceptionHandler.java", "src/main/java/com/zading/todoapi/exception/ErrorCode.java"], deliverable: "新增一个业务错误码，并覆盖接口失败测试。" },
    { week: 13, stage: "数据一致性", title: "事务、操作日志与回滚", focus: "让 Todo 变化和操作日志一起提交，失败时一起回滚。", files: ["docs/week-13-learning.md", "src/main/java/com/zading/todoapi/service/TodoService.java", "src/main/java/com/zading/todoapi/service/TodoActionLogService.java"], deliverable: "人为制造异常，并验证事务没有留下半成品数据。" },
    { week: 14, stage: "性能", title: "缓存与查询性能", focus: "识别重复查询，理解缓存命中、缓存 key 和数据失效。", files: ["docs/week-14-learning.md", "src/main/java/com/zading/todoapi/config/CacheNames.java", "src/main/java/com/zading/todoapi/service/TodoService.java"], deliverable: "观察缓存前后 SQL 日志，并说明更新时为什么要清缓存。" },
    { week: 15, stage: "性能", title: "Redis 缓存", focus: "把单 JVM 缓存切换成可共享的 Redis 缓存，并理解 TTL。", files: ["docs/week-15-learning.md", "src/main/java/com/zading/todoapi/config/RedisCacheConfig.java", "src/main/resources/application-redis.properties"], deliverable: "设计一个包含 userId 的缓存 key 和明确的 TTL。" },
    { week: 16, stage: "异步", title: "Spring 事件、异步与线程池", focus: "把附属工作移出主请求，并分析线程池耗尽和异常处理。", files: ["docs/week-16-learning.md", "src/main/java/com/zading/todoapi/config/AsyncConfig.java", "src/main/java/com/zading/todoapi/event/TodoActionLogEventListener.java"], deliverable: "完成一次 afterCommit 异步处理，并记录执行线程。" },
    { week: 17, stage: "后台任务", title: "定时任务与批处理", focus: "使用 cron、时区和分页批量处理过期 Todo。", files: ["docs/week-17-learning.md", "src/main/java/com/zading/todoapi/job/TodoOverdueJob.java", "src/main/java/com/zading/todoapi/service/TodoOverdueService.java"], deliverable: "手动运行任务，并验证重复执行不会产生错误结果。" },
    { week: 18, stage: "可观测性", title: "Actuator 与任务状态", focus: "通过 health、info、metrics 和任务状态判断服务是否正常。", files: ["docs/week-18-learning.md", "src/main/resources/application.properties", "src/main/java/com/zading/todoapi/service/TodoOverdueJobStatusService.java"], deliverable: "调用健康端点并解释每个状态代表什么。" },
    { week: 19, stage: "生产配置", title: "生产配置、启动与日志", focus: "区分开发配置和生产配置，理解环境变量和启动失败日志。", files: ["docs/week-19-learning.md", "src/main/resources/application-prod.properties", "src/main/resources/application-dev.properties"], deliverable: "使用指定 Profile 启动，并确认敏感配置来自环境变量。" },
    { week: 20, stage: "业务边界", title: "文件上传与附件管理", focus: "处理文件内容、数据库元数据、访问权限和失败补偿。", files: ["docs/week-20-learning.md", "src/main/java/com/zading/todoapi/controller/TodoAttachmentController.java", "src/main/java/com/zading/todoapi/service/TodoAttachmentService.java"], deliverable: "完成上传、下载和非法文件测试。" },
    { week: 21, stage: "业务边界", title: "RBAC 角色权限", focus: "区分认证、角色权限和资源归属，保护管理员接口。", files: ["docs/week-21-learning.md", "src/main/java/com/zading/todoapi/controller/AdminController.java", "src/main/java/com/zading/todoapi/security/SecurityConfig.java"], deliverable: "覆盖 USER 和 ADMIN 的访问矩阵。" },
    { week: 22, stage: "测试", title: "Service 单元测试与 Mock", focus: "隔离 Repository 等依赖，直接验证业务规则和异常。", files: ["docs/week-22-learning.md", "src/test/java/com/zading/todoapi/service/TodoServiceTest.java", "src/test/java/com/zading/todoapi/service/AuthServiceTest.java"], deliverable: "为一个 Service 方法补齐成功、异常和边界测试。" },
    { week: 23, stage: "数据库性能", title: "查询优化、索引与慢 SQL", focus: "从接口耗时追到 SQL 和执行计划，再用索引或查询改写验证。", files: ["docs/week-23-learning.md", "src/main/java/com/zading/todoapi/repository/TodoRepository.java", "src/main/resources/db/migration/V8__add_todo_query_indexes.sql"], deliverable: "记录一条查询优化前后的 SQL 和执行计划差异。" },
    { week: 24, stage: "运行环境", title: "Docker 与项目容器化", focus: "理解镜像、容器、端口、环境变量和数据卷。", files: ["docs/week-24-learning.md", "Dockerfile", "docker-compose.yml"], deliverable: "解释 Dockerfile 每一层作用；有 Docker 时完成一次容器启动。" },
    { week: 25, stage: "数据库设计", title: "PostgreSQL 与生产数据设计", focus: "使用约束、索引和迁移保护数据质量。", files: ["docs/week-25-learning.md", "docs/database-design.md", "src/main/resources/application-postgres.properties"], deliverable: "画出核心表关系并说明每个约束解决的问题。" },
    { week: 26, stage: "Redis 深入", title: "分布式锁、限流与幂等", focus: "使用共享状态解决多实例下的重复执行和并发保护。", files: ["docs/week-26-learning.md", "src/main/java/com/zading/todoapi/redis", "src/main/java/com/zading/todoapi/config/properties/RedisProtectionProperties.java"], deliverable: "完成一个幂等或限流场景，并验证 TTL 和重复请求。" },
    { week: 27, stage: "消息系统", title: "Kafka 与 RabbitMQ", focus: "理解消息发布、消费确认、重试、死信和消费幂等。", files: ["docs/week-27-learning.md", "src/main/java/com/zading/todoapi/messaging/kafka", "src/main/java/com/zading/todoapi/messaging/rabbitmq"], deliverable: "画出两种消息链路，并解释失败后消息去哪里。" },
    { week: 28, stage: "综合重构", title: "项目复盘、重构与交付", focus: "从请求入口追到数据、异常、测试和运行状态，小步改善结构。", files: ["docs/week-28-learning.md", "README.md", "src/main/java/com/zading/todoapi", "src/test/java/com/zading/todoapi"], deliverable: "完成一次行为不变的重构，并留下测试和设计说明。" },
];

const weekDetails = [
    { concepts: ["JDK / JRE / JVM", "类与 main 方法", "基本类型与引用类型", "条件与循环", "方法、参数与返回值"], practice: "用 Todo 标题是否为空作为条件，拆出一个可复用的校验方法。", pitfall: "不要把 String 当成基本类型用 == 比较内容；应使用 equals，并先考虑 null。" },
    { concepts: ["封装与构造方法", "接口与多态", "List 与泛型", "受检/非受检异常", "Maven 生命周期"], practice: "建立一个 Todo 对象集合，通过接口方法新增、查询并处理不存在的情况。", pitfall: "不要为了复用几行代码就滥用继承，也不要用 catch (Exception) 吞掉所有失败原因。" },
    { concepts: ["分层职责", "依赖方向", "依赖注入", "领域模型", "DTO 与 Entity"], practice: "从 Controller 入口沿着 Service 追到 Repository，标注每层输入和输出。", pitfall: "Controller 不应堆积业务规则，Repository 也不应该决定 HTTP 状态码。" },
    { concepts: ["IoC 容器", "@SpringBootApplication", "@RestController", "HTTP 方法与状态码", "JSON 序列化"], practice: "新增一个小型 REST 接口，分别观察成功和参数错误时的响应。", pitfall: "不要让所有请求都返回 200；创建、参数错误和资源不存在应使用合适的 HTTP 状态。" },
    { concepts: ["Entity 生命周期", "JpaRepository", "方法名派生查询", "持久化上下文", "SQL 与对象映射"], practice: "为 Todo 增加一个查询条件，并从方法名推导实际 WHERE 子句。", pitfall: "派生方法名不是魔法 SQL；字段名写错会启动失败，关联查询还要警惕 N+1。" },
    { concepts: ["Spring Profile", "外部化配置", "Flyway 版本迁移", "DTO 边界", "Bean Validation"], practice: "增加一个 DTO 字段和对应迁移，验证旧数据与新请求都能正确处理。", pitfall: "数据库结构变更应交给迁移脚本，不要依赖 ddl-auto 在生产环境自动改表。" },
    { concepts: ["Page 与 Pageable", "Sort 白名单", "分页边界", "参数校验", "稳定排序"], practice: "调用分页接口并改变页码、大小和排序，观察响应元数据与 SQL。", pitfall: "不要允许无限 pageSize 或任意排序字段；生产查询必须有上限和稳定排序。" },
    { concepts: ["密码哈希", "JWT 声明与过期", "Security Filter Chain", "SecurityContext", "用户数据隔离"], practice: "注册两个用户，用各自 Token 创建数据，再验证交叉访问被拒绝。", pitfall: "资源归属不能信任请求中的 userId，必须从已认证身份中获取当前用户。" },
    { concepts: ["OpenAPI 契约", "请求过滤器", "requestId", "结构化日志", "MDC 上下文"], practice: "发起一次请求，从响应头拿到 requestId，再定位同一条服务端日志。", pitfall: "日志中不要输出密码、完整 Token 或敏感业务数据；可追踪不等于全部记录。" },
    { concepts: ["测试金字塔", "MockMvc", "测试夹具", "成功与失败路径", "持续集成"], practice: "为同一功能各写一个成功案例、一个业务失败案例和一个参数边界案例。", pitfall: "只验证 HTTP 200 或只测 happy path，会让测试在真正出错时失去保护作用。" },
    { concepts: ["软删除标记", "删除时间", "默认查询过滤", "恢复语义", "物理清理"], practice: "删除、查询、恢复同一条 Todo，逐步观察普通列表和回收站结果。", pitfall: "软删除后唯一约束仍可能生效；所有读取路径也必须统一过滤 deleted 数据。" },
    { concepts: ["@ExceptionHandler", "错误码", "HTTP 状态映射", "校验错误", "统一错误结构"], practice: "触发参数错误、资源不存在和业务冲突，比较三种错误响应。", pitfall: "不要把堆栈、SQL 或内部异常类名直接返回给前端，也不要用一个错误码代表所有失败。" },
    { concepts: ["事务边界", "回滚规则", "传播行为", "原子性", "afterCommit"], practice: "在写 Todo 和操作日志之间制造异常，确认两份数据一起回滚。", pitfall: "同类内部调用可能绕过 @Transactional 代理；捕获异常后不再抛出也可能阻止回滚。" },
    { concepts: ["Cache-Aside", "缓存 Key", "命中与未命中", "缓存失效", "一致性窗口"], practice: "连续查询同一数据观察 SQL 次数，再更新数据验证缓存是否及时清除。", pitfall: "缓存 Key 必须包含用户边界；只写缓存不设计失效会长期返回旧数据。" },
    { concepts: ["Redis 数据结构", "序列化", "TTL", "CacheManager", "降级策略"], practice: "设计 userId + todoId 的缓存 Key，并说明 TTL 到期和更新删除两种失效路径。", pitfall: "TTL 不是越长越好；没有版本和用户维度的 Key 很容易造成脏数据或数据串读。" },
    { concepts: ["Spring 事件", "@Async 代理", "线程池", "异常处理", "事务提交后事件"], practice: "记录发布线程与消费线程名称，确认附属操作不会阻塞主请求。", pitfall: "异步线程不会自动继承原事务和上下文；线程池无界队列也可能拖垮服务。" },
    { concepts: ["@Scheduled", "cron 与时区", "分页批处理", "幂等执行", "多实例并发"], practice: "手动触发一次过期任务，再重复执行并确认结果没有被重复修改。", pitfall: "单机定时任务部署到多实例后会同时运行，必须提前考虑锁、分片或幂等。" },
    { concepts: ["Health", "Info", "Metrics", "Readiness / Liveness", "自定义状态指标"], practice: "访问健康和指标端点，找到能判断数据库与定时任务状态的信号。", pitfall: "健康端点不是越详细越好；公开环境应避免泄露组件地址、异常和配置。" },
    { concepts: ["配置分层", "环境变量", "密钥管理", "日志级别", "启动失败诊断"], practice: "用不同 Profile 启动并比较配置，确认敏感值没有写死在仓库。", pitfall: "不要把生产密码写进 properties 或镜像；默认值也不能掩盖关键配置缺失。" },
    { concepts: ["Multipart 请求", "文件元数据", "路径穿越", "内容类型校验", "失败补偿"], practice: "上传合法和非法文件，再验证下载权限、文件大小与元数据是否一致。", pitfall: "绝不能把用户提供的原始文件名直接拼进磁盘路径，也不能只相信 Content-Type。" },
    { concepts: ["认证与授权", "Role 与 Permission", "方法级鉴权", "资源归属", "403 与 404"], practice: "用匿名、USER、ADMIN 三种身份调用接口，整理完整访问矩阵。", pitfall: "拥有某个角色不代表可以访问所有人的资源；角色校验之后仍要检查数据归属。" },
    { concepts: ["Arrange-Act-Assert", "Mockito Stub", "verify", "ArgumentCaptor", "行为测试"], practice: "隔离 Repository，为 Service 方法覆盖成功、异常与边界三类行为。", pitfall: "Mock 太多会把实现细节固定死；优先验证业务输出和关键协作，而不是每一次内部调用。" },
    { concepts: ["EXPLAIN ANALYZE", "索引选择性", "联合索引", "最左前缀", "深分页"], practice: "为真实查询记录执行计划，对比增加索引前后的扫描行数和耗时。", pitfall: "索引不是越多越好；低选择性字段、错误列顺序和函数计算都可能让索引失效。" },
    { concepts: ["镜像与容器", "分层构建", "端口映射", "环境变量", "数据卷与 Compose"], practice: "逐行解释 Dockerfile，从构建产物追踪到容器中的启动命令。", pitfall: "容器里的 localhost 指向容器自己；不要把密钥或开发数据库写进镜像层。" },
    { concepts: ["数据类型", "主外键约束", "检查约束", "事务与锁", "Schema 迁移"], practice: "检查核心表设计，为每个非空、唯一、外键和检查约束说明业务原因。", pitfall: "只在 Java 层校验无法阻止其他写入入口；关键数据规则应同时由数据库保护。" },
    { concepts: ["SET NX PX", "锁持有者令牌", "Lua 原子操作", "限流算法", "幂等键与 TTL"], practice: "对同一幂等键连续请求，观察首次执行、重复响应和过期后的行为。", pitfall: "释放锁时不能直接 DEL，必须确认持有者；多条 Redis 命令也不天然原子。" },
    { concepts: ["Topic / Partition", "Consumer Group / Offset", "Exchange / Queue / Binding", "Ack 与重试", "死信与消费幂等"], practice: "分别画出 Kafka 和 RabbitMQ 的发布、路由、确认、重试与死信链路。", pitfall: "消息系统通常是至少一次投递；消费者必须能安全处理重复消息，不能假设天然 exactly-once。" },
    { concepts: ["架构全景", "代码坏味道", "小步重构", "回归测试", "交付文档"], practice: "选一个真实坏味道，小步重构并用测试证明外部行为没有变化。", pitfall: "不要把重构变成大爆炸重写；每一步都应可编译、可验证、可回退。" },
];

const conceptExplanations = [
    [
        "JDK 是开发工具集合，包含 javac、java 等命令；JRE 提供运行库；JVM 负责加载并执行编译后的 .class 字节码。JDK 21 编译的是字节码，不是直接生成当前机器的可执行文件。",
        "JVM 以 public static void main(String[] args) 作为普通 Java 应用入口。static 表示无需先 new 对象，String[] args 用于接收命令行参数。",
        "基本类型变量直接保存数值或布尔值；引用类型变量保存对象引用，可以是 null。方法传参始终是值传递，对象参数传递的是引用值的副本。",
        "if / switch 用于分支选择，for / while 用于重复执行。循环必须明确结束条件，业务循环还要避免一次处理无限量数据。",
        "方法签名通过名称和参数区分调用；参数是输入，return 是输出。把重复判断提取成方法，可以让 main 只负责组织流程。",
    ],
    [
        "封装用 private 字段保护对象状态，并通过构造方法或行为方法保证对象从创建开始就合法，而不是让任意调用者随意修改字段。",
        "接口描述调用方依赖的能力，实现类提供具体做法。多态让 Service 依赖接口，在不改变调用代码的情况下替换内存、数据库等实现。",
        "List<T> 表示有顺序、可重复的同类型集合。泛型 T 在编译期约束元素类型，避免取值后到处进行强制转换。",
        "受检异常要求调用者捕获或继续声明；RuntimeException 通常表示参数或业务状态错误。异常应携带失败语义，不应被空 catch 吞掉。",
        "Maven 按 validate、compile、test、package 等阶段构建项目，并通过 groupId、artifactId、version 坐标解析依赖。mvn test 会先完成编译。",
    ],
    [
        "Controller 处理 HTTP 参数和响应，Service 执行业务规则，Repository 负责持久化，Model 表达数据和状态。分层的目标是隔离变化，不是机械增加类。",
        "典型依赖方向是 Controller → Service → Repository。下层不应反向依赖 Web 层，否则数据库代码会被 HTTP 细节绑死。",
        "构造器注入把依赖明确写进类的创建条件，也方便测试传入 Mock。Spring 容器负责找到 Bean 并完成对象装配。",
        "领域模型不仅是字段集合，也承载状态约束，例如 Todo 完成时同步记录 completedAt，避免不同调用方各写一套规则。",
        "DTO 表达接口输入输出，Entity 表达数据库映射。分开后，修改表结构不会自动改变公共 API，也能限制客户端可写字段。",
    ],
    [
        "IoC 表示对象创建与依赖组装交给 Spring 容器。被容器管理的对象称为 Bean，代理、事务和异步等能力都依赖这一生命周期。",
        "@SpringBootApplication 组合了配置、自动配置和组件扫描。SpringApplication.run 会创建 ApplicationContext 并启动内嵌 Web 服务器。",
        "@RestController 让方法返回值直接写入响应体；@GetMapping、@PostMapping 等注解把 HTTP 方法和路径映射到 Java 方法。",
        "GET 应只读，POST 通常创建资源，PUT 强调整体替换且应幂等，DELETE 重复调用后结果应稳定。状态码属于接口契约的一部分。",
        "Jackson 根据字段或访问器在 Java 对象与 JSON 之间转换。请求 JSON 缺字段、类型不匹配或日期格式错误时，会在进入业务方法前失败。",
    ],
    [
        "Entity 在 JPA 中经历 transient、managed、detached、removed 等状态。只有受持久化上下文管理的对象才能自动执行脏检查。",
        "JpaRepository<T, ID> 已提供 save、findById、existsById、deleteById 等通用操作；ID 泛型必须与实体主键类型一致。",
        "findByTitleContaining 等方法名会被 Spring Data 解析为查询条件。属性名必须真实存在，And、Or、OrderBy 会改变生成的查询。",
        "持久化上下文保证同一事务内同一主键通常对应同一对象。事务提交时，Hibernate 比较快照并生成必要的 UPDATE。",
        "ORM 最终仍执行 SQL。关联懒加载可能对每行再发一次查询形成 N+1，必须结合 SQL 日志和访问方式判断。",
    ],
    [
        "Profile 用于选择一组环境配置或 Bean，例如 dev 使用 H2、postgres 使用 PostgreSQL。激活 Profile 不应改变核心业务语义。",
        "Spring 配置可来自 properties、环境变量和命令行参数，并有明确优先级。生产密钥应从外部注入，而不是提交到仓库。",
        "Flyway 按 V1、V2 等版本顺序执行迁移，并在历史表记录校验和。已经执行的版本脚本不应直接修改，应新增下一个版本。",
        "请求 DTO 只暴露客户端允许提交的字段；响应 DTO 只输出接口承诺的字段，防止 Entity 关联和敏感信息意外序列化。",
        "Bean Validation 通过 @NotBlank、@Size 等声明约束；Controller 参数还需要 @Valid 才会触发校验并交给统一异常处理。",
    ],
    [
        "Page<T> 同时包含当前页数据、总条数和总页数。为了得到总数，JPA 分页通常还会额外执行 count 查询。",
        "Pageable 的页码默认从 0 开始，offset 等于 page × size。size 必须设置上限，避免一次把整张表读进内存。",
        "排序字段应使用白名单映射到实体属性，不能把客户端字符串直接拼进 SQL。未知字段应返回明确的 400。",
        "参数校验应覆盖 page≥0、size 范围和排序方向。校验越靠近入口，业务层收到的数据越可信。",
        "只按可能重复的字段排序会导致翻页时数据漂移；追加 id 作为次级排序，可以获得稳定且可重复的顺序。",
    ],
    [
        "密码应使用 BCrypt 等单向哈希保存。随机盐让相同密码产生不同结果，登录时调用 matches，而不是解密数据库中的密码。",
        "JWT 是签名令牌而不是加密容器，payload 可以被读取。它应只放必要声明，并校验签名、过期时间和签发方。",
        "认证过滤器从 Authorization 读取 Bearer Token，验证后构造 Authentication。过滤器顺序决定它是否在授权判断前执行。",
        "SecurityContext 保存当前请求的认证身份，Controller 或 Service 应从这里取得当前用户，而不是相信请求传来的 userId。",
        "用户隔离要落实到每条 Repository 查询，例如同时按 id 和 userId 查询。只在返回结果后再判断会增加数据泄露风险。",
    ],
    [
        "OpenAPI 用机器可读结构描述路径、参数、状态码和模型，Swagger UI 只是对这份契约的可视化与调试界面。",
        "OncePerRequestFilter 保证在一次请求调度中执行一次，适合生成 requestId、记录耗时并在 finally 中清理上下文。",
        "requestId 应优先复用合法的客户端请求头，否则生成新值；同一个值要写入响应头和日志，才能端到端追踪。",
        "结构化日志用稳定字段记录 method、path、status、duration 等信息，比拼接自然语言更容易检索和聚合。",
        "MDC 把 requestId 绑定到当前线程；线程复用前必须 clear。异步任务切换线程时，MDC 不会天然自动传递。",
    ],
    [
        "测试金字塔建议大量快速单元测试、适量集成测试和少量端到端测试。层级越高越真实，但运行更慢、定位更困难。",
        "MockMvc 在不监听真实端口的情况下执行 Spring MVC 与 Security 过滤链，适合验证状态码、响应 JSON 和权限规则。",
        "测试夹具应通过工厂或 Builder 创建合法默认对象，只在测试中覆盖相关字段，减少每个测试重复准备大量数据。",
        "失败路径包括非法参数、资源不存在、权限不足和业务冲突。测试不仅要断言失败，还要检查数据没有被错误修改。",
        "CI 应从干净环境执行同一套构建命令。依赖时间、随机值或共享数据库状态的测试会产生不稳定结果。",
    ],
    [
        "软删除通常用 deleted 标记资源是否可见，而不是立刻 DELETE。这样可以恢复、审计，并保留其他表的引用关系。",
        "deletedAt 记录删除发生时间，可用于回收站排序和保留期清理。删除和时间戳应在同一事务中一起更新。",
        "所有普通查询都必须排除 deleted=true，包括按 ID、列表、搜索和统计；遗漏一个入口就会让已删除数据重新出现。",
        "恢复操作要重新检查唯一约束、所有者和当前状态。已经恢复的资源再次恢复时，应定义幂等或冲突语义。",
        "物理清理是独立生命周期步骤，应按保留期分批执行并记录结果，避免一次删除大量行造成锁和日志压力。",
    ],
    [
        "@ExceptionHandler 把指定异常转换为 HTTP 响应；@RestControllerAdvice 可以让处理逻辑跨 Controller 统一生效。",
        "错误码是前后端稳定契约，例如 TODO_NOT_FOUND。异常类名和自然语言消息可以变化，错误码不应随意改变。",
        "400 表示请求不合法，401 表示未认证，403 表示身份无权访问，404 表示资源不可见，409 常用于状态冲突。",
        "参数校验失败通常包含多个字段错误，应整理成字段、规则和提示列表，而不是只返回第一条或完整堆栈。",
        "统一错误结构至少应包含 code、message、timestamp、path 和 requestId，便于前端处理，也便于服务端定位同一请求。",
    ],
    [
        "@Transactional 通常通过 Spring 代理在方法前开启事务、返回时提交、抛错时回滚；只有经过代理的调用才能触发。",
        "默认情况下 RuntimeException 和 Error 会回滚，受检异常默认不回滚。需要不同规则时应显式配置 rollbackFor。",
        "REQUIRED 会加入现有事务或创建新事务；REQUIRES_NEW 会暂停外层事务。传播行为会改变提交与回滚边界。",
        "原子性要求 Todo 更新与操作日志要么一起成功，要么一起失败。事务边界应围绕完整业务动作，而非单条 SQL。",
        "AFTER_COMMIT 事件只在事务成功提交后处理外部副作用，避免数据库已回滚但通知或消息已经发送。",
    ],
    [
        "Cache-Aside 由业务代码先查缓存，未命中再查数据库并回填；写操作通常先更新数据库，再删除对应缓存。",
        "缓存 Key 必须唯一表达查询条件和数据边界，例如 userId:todoId。遗漏用户维度可能让不同用户读取到同一份数据。",
        "缓存命中直接返回值，未命中才访问数据库。需要通过 SQL 日志或指标确认缓存确实减少了查询，而非凭感觉判断。",
        "新增、更新、删除和恢复都可能让缓存失效。只处理其中一个写入口会产生难以复现的旧数据。",
        "数据库提交与缓存删除之间存在短暂一致性窗口。缓存适合容忍短期旧值的读取场景，不应掩盖强一致业务要求。",
    ],
    [
        "Redis 是独立进程中的内存数据服务，多实例应用可以共享同一份缓存；它与单 JVM 的 ConcurrentMap 缓存边界不同。",
        "序列化决定 Java 对象如何存进 Redis。类字段变化和类型信息会影响兼容性，不能把序列化格式当作永远不变。",
        "TTL 是 Key 的剩余生存时间，到期后 Redis 自动删除。TTL 应根据数据变化频率和可接受旧值时间设置。",
        "RedisCacheManager 负责缓存名、序列化和默认 TTL；不同缓存可配置不同过期时间，避免一刀切。",
        "Redis 不可用时应明确选择失败、绕过缓存查数据库或限流保护。无限重试会放大延迟并拖垮请求线程。",
    ],
    [
        "Spring 事件让发布者只表达“发生了什么”，监听器决定后续动作，从而降低业务主流程对日志、通知等功能的直接依赖。",
        "@Async 也是代理能力，同类内部调用不会异步。异步方法返回 void 时，异常不能通过 Future 交还调用方。",
        "线程池的 core、max、queueCapacity 和拒绝策略共同决定高峰行为。无界队列可能把内存耗尽并制造长时间延迟。",
        "异步异常应由 AsyncUncaughtExceptionHandler、Future 或任务内部日志捕获，不能假设请求线程会看到失败。",
        "@TransactionalEventListener(AFTER_COMMIT) 在提交后触发监听器；此时原事务已经结束，新的数据库写入需要新事务。",
    ],
    [
        "@Scheduled 由 Spring 调度线程按 fixedDelay、fixedRate 或 cron 触发。任务方法不应依赖 HTTP 请求上下文。",
        "cron 必须明确时区。部署环境默认时区改变会让任务在错误时间运行，尤其要注意夏令时和跨地区部署。",
        "批处理应分页或按主键游标读取，限制每批数量。边处理边修改排序字段时，offset 分页可能跳过数据。",
        "幂等任务重复执行不会产生额外副作用，例如只更新仍处于未处理状态的数据，并记录确定的处理条件。",
        "多实例会各自触发同一定时任务。可使用数据库锁、Redis 锁或专用调度平台保证同一批任务只有一个执行者。",
    ],
    [
        "Health 表示组件是否可用，并可聚合数据库等依赖状态。DOWN 不只是字符串，还会映射到监控和 HTTP 状态。",
        "Info 用于暴露版本、构建等非敏感元数据，不应把密码、连接串或完整环境变量放进响应。",
        "Metrics 用 Counter、Gauge、Timer 等记录次数、当前值和耗时分布。平均值无法反映 P95/P99 长尾延迟。",
        "Liveness 判断进程是否需要重启，Readiness 判断是否能接收流量。外部依赖短暂失败通常不应让进程反复重启。",
        "自定义 HealthIndicator 应快速、可超时且副作用最小；耗时很长的深度检查会反过来拖慢监控和服务。",
    ],
    [
        "Profile 配置通常由 application.properties 加 application-prod.properties 覆盖。公共默认值放基础文件，环境差异放 Profile。",
        "环境变量适合容器和部署平台注入配置，可通过占位符映射。变量缺失时，关键生产配置应快速启动失败。",
        "密钥不应进入 Git、日志或镜像层。生产环境应使用 Secret 管理服务，并限制读取权限与轮换周期。",
        "日志级别应按包控制；生产环境通常避免全局 DEBUG，因为它会增加成本并可能输出敏感参数。",
        "启动失败应从最底层 Caused by 开始定位，区分端口占用、数据库不可达、配置绑定失败和迁移失败。",
    ],
    [
        "multipart/form-data 用 boundary 把文本字段与二进制文件分段。Spring 将文件映射为 MultipartFile，但内容仍需业务校验。",
        "数据库保存原文件名、大小、类型、存储位置和所有者等元数据；文件内容可存磁盘或对象存储，不应混淆两者事务。",
        "路径穿越利用 ../ 等片段逃出目标目录。服务端应生成存储名并规范化路径，不能直接使用用户文件名。",
        "Content-Type 来自客户端，不能完全信任。还应限制大小、扩展名，并在高风险场景检查文件签名或扫描内容。",
        "数据库写入成功但文件保存失败会产生孤儿记录，反之会产生孤儿文件。需要设计顺序、补偿清理和重试策略。",
    ],
    [
        "认证回答“你是谁”，授权回答“你能做什么”。JWT 验证成功只完成认证，并不自动允许访问所有接口。",
        "Role 适合 USER、ADMIN 等粗粒度分类，Permission 适合 todo:read 等具体能力。角色可以映射为一组权限。",
        "@PreAuthorize 在方法调用前执行表达式，可结合角色、参数和自定义授权服务，但复杂规则应保持可测试。",
        "资源归属是对象级权限：普通用户即使有读取 Todo 的能力，也只能读取 userId 属于自己的记录。",
        "403 明确表示资源存在但无权访问；404 可以隐藏资源是否存在。项目应统一选择，避免不同接口泄露信息。",
    ],
    [
        "Arrange 准备输入与依赖，Act 只调用被测行为，Assert 验证结果和关键副作用。三个阶段清晰能让失败更容易定位。",
        "Mockito when(...).thenReturn(...) 用于控制依赖输出。只 Stub 当前案例需要的调用，过度 Stub 会隐藏无效测试。",
        "verify 用于确认重要协作是否发生，例如保存一次操作日志；不要验证每个 getter 等实现细节。",
        "ArgumentCaptor 捕获传给依赖的对象，适合断言保存前设置的 userId、completedAt 等字段，而非只验证调用发生。",
        "行为测试关注给定输入产生的返回、异常和状态变化。重构内部实现后，只要业务行为不变，测试就应继续通过。",
    ],
    [
        "EXPLAIN ANALYZE 会真实执行 SQL，并显示实际耗时、实际行数和扫描方式。对写语句或生产大表使用前必须评估副作用。",
        "选择性表示条件能排除多少数据。唯一或高区分度列通常更适合索引，布尔字段单独索引往往收益有限。",
        "联合索引按多个列共同排序。列顺序应匹配常见过滤与排序模式，而不是简单照实体字段顺序创建。",
        "最左前缀表示联合索引通常从最左列开始有效；跳过首列查询时，数据库可能无法高效利用后续列。",
        "深分页的 OFFSET 仍需扫描并丢弃前面大量行。大数据场景可使用基于最后一条排序键的 Keyset Pagination。",
    ],
    [
        "镜像是只读模板，容器是镜像的运行实例。删除容器不会删除镜像，但容器可写层中的数据也会一起消失。",
        "Dockerfile 每条关键指令形成可缓存层。先复制依赖描述再复制源码，可以在代码变化时复用依赖下载层。",
        "多阶段构建在前一阶段使用完整 JDK 编译，最终阶段只保留 JRE 和 jar，减少镜像体积与攻击面。",
        "端口映射 8080:8080 左侧是宿主机端口，右侧是容器端口。EXPOSE 只描述端口，并不会自动发布到宿主机。",
        "Volume 把持久化数据放到容器可写层之外；Compose 网络中服务应通过服务名访问，localhost 只代表当前容器。",
    ],
    [
        "数据库类型表达真实范围和语义，例如 timestamptz 保存带时区时间点，numeric 适合精确金额，boolean 不应拿字符串代替。",
        "主键唯一标识行，外键保证引用目标存在。外键删除策略必须根据业务选择 RESTRICT、CASCADE 或 SET NULL。",
        "CHECK 约束可以保证 priority 范围、标题非空等规则，即使数据不是通过 Java 应用写入也仍然生效。",
        "PostgreSQL 使用 MVCC 提供并发可见性；行锁和隔离级别影响阻塞、不可重复读和写冲突，需要控制事务时长。",
        "Schema 迁移应支持已有数据：先增加可空列并回填，再增加非空约束，通常比一步强制修改更安全。",
    ],
    [
        "SET key value NX PX ttl 在一个原子命令中完成“仅不存在时写入并设置过期”，可用作基础分布式锁。",
        "锁值必须包含唯一持有者令牌。释放时先比较令牌，防止一个已超时的客户端删除另一个客户端后来获得的锁。",
        "Lua 脚本让比较与删除在 Redis 内原子执行。把 GET 和 DEL 分成两个命令会在两者之间产生竞态窗口。",
        "令牌桶允许一定突发流量，滑动窗口更精确但状态和计算成本更高。限流维度应明确是用户、IP 还是接口。",
        "幂等键标识一次业务请求，并缓存处理中或完成结果。TTL 要覆盖客户端可能重试的时间窗口，同时避免永久占用。",
    ],
    [
        "Kafka Topic 被划分为 Partition；同一 Partition 内记录有顺序，不同 Partition 之间没有全局顺序保证。",
        "同一 Consumer Group 内一个 Partition 同时只由一个消费者处理；Offset 表示消费位置，提交时机影响重复或丢失风险。",
        "RabbitMQ Exchange 根据类型和 routing key 把消息路由到 Queue，Binding 定义 Exchange 与 Queue 的连接规则。",
        "手动 Ack 应在业务成功后发送；失败可以重试、拒绝或进入死信。无限 requeue 会形成高速失败循环。",
        "消息系统通常提供至少一次投递，消费者必须用业务唯一键去重。死信队列保存最终失败消息，仍需要告警和人工处理流程。",
    ],
    [
        "架构全景应画出 HTTP 入口、业务层、数据库、缓存、消息和后台任务，以及每条关键数据流与故障边界。",
        "代码坏味道包括重复逻辑、过长方法、职责混乱和错误依赖方向。坏味道是重构信号，不等于功能 Bug。",
        "小步重构一次只做提取方法、改名或移动职责等单一变化，每一步保持可编译并运行相关测试。",
        "回归测试证明对外行为未改变，应覆盖核心成功路径、失败路径、权限与事务边界，而不只测试新结构。",
        "交付文档应让另一位开发者能从零启动、配置、调用、测试和排错；命令必须真实执行验证，不能只描述理想流程。",
    ],
];

const advancedKnowledge = [
    [
        { title: "栈、堆与变量作用域", detail: "局部变量通常随方法调用进入栈帧，对象实例位于堆中；变量保存的是值或对象引用。离开作用域后引用消失，但对象何时回收由垃圾收集器判断。" },
        { title: "String 不可变与 equals", detail: "String 的内容创建后不能修改，拼接通常产生新对象。== 比较引用是否相同，equals 比较字符内容；常量写在 equals 左侧还能避免 null 导致异常。" },
        { title: "编译错误与运行时异常", detail: "类型不匹配、缺少符号会在 javac 编译阶段失败；数组越界、空指针等在程序运行到对应路径时才出现。排错时先判断失败发生在哪个阶段。" },
    ],
    [
        { title: "equals 与 hashCode 契约", detail: "两个对象 equals 为 true 时必须拥有相同 hashCode，否则 HashSet 或 HashMap 可能存入逻辑重复对象。作为集合键的字段在存入后不应再改变。" },
        { title: "泛型与类型擦除", detail: "List<T> 在编译期约束元素类型，运行时大部分泛型参数会被擦除。因此不能直接 new T()，也不能可靠地用 instanceof List<String> 判断元素类型。" },
        { title: "Maven 依赖范围与传递依赖", detail: "compile、runtime、test 决定依赖在哪些阶段进入类路径；传递依赖可能引入版本冲突，应使用 dependency:tree 找到真正来源，而不是盲目重复声明版本。" },
    ],
    [
        { title: "依赖倒置", detail: "高层业务规则应依赖抽象而不是具体数据库实现。Service 面向 Repository 接口协作后，存储技术可以替换，单元测试也能使用 Fake 或 Mock。" },
        { title: "构造器注入", detail: "构造器参数明确表达类的必需依赖，便于创建不可变字段和直接测试。字段注入会隐藏依赖，并让脱离 Spring 容器创建对象变得困难。" },
        { title: "DTO 与 Entity 映射边界", detail: "Entity 服务于持久化，DTO 服务于接口契约。直接暴露 Entity 会让数据库字段、懒加载关系和接口格式耦合，映射层负责控制两者之间的变化。" },
    ],
    [
        { title: "一次 HTTP 请求的处理链", detail: "请求通常依次经过 Filter、Spring Security、DispatcherServlet、Controller、Service，最后进入数据层；响应再沿相反方向返回。不同横切逻辑应放在合适的阶段。" },
        { title: "HTTP 幂等语义", detail: "GET、PUT、DELETE 按语义应可重复执行而不产生额外结果，POST 通常不保证幂等。支付或创建类 POST 可借助幂等键补充重复请求保护。" },
        { title: "Jackson 序列化边界", detail: "Spring MVC 默认使用 Jackson 在 Java 对象和 JSON 之间转换。字段命名、日期格式、未知字段和循环引用都可能改变接口行为，应通过 DTO 和配置明确控制。" },
    ],
    [
        { title: "主键生成策略", detail: "IDENTITY 依赖数据库自增，SEQUENCE 可提前批量获取编号，UUID 适合分布式生成但索引更大。策略会影响批量插入、数据库兼容性与索引局部性。" },
        { title: "懒加载与事务边界", detail: "LAZY 关联通常在真正访问属性时查询数据库；离开持久化上下文后再访问可能抛出 LazyInitializationException。应在业务边界内明确加载所需数据。" },
        { title: "N+1 查询", detail: "先查 N 条主记录，再逐条访问关联可能额外执行 N 次 SQL。可通过 fetch join、EntityGraph、DTO 投影或批量加载解决，但要结合分页限制选择方案。" },
    ],
    [
        { title: "配置优先级", detail: "命令行参数、环境变量、Profile 文件和默认配置存在覆盖顺序。同一键的最终值应通过启动日志或 Environment 验证，避免误以为修改的文件一定生效。" },
        { title: "迁移脚本不可变", detail: "已在共享环境执行过的 Flyway 版本不应修改，否则校验和会不一致。结构修正应新增更高版本迁移，并同时考虑旧数据如何转换。" },
        { title: "DTO 向后兼容", detail: "新增可选字段通常兼容旧客户端，删除或改变字段类型容易破坏契约。输入 DTO、输出 DTO 分离后，可以分别演进写入要求与返回格式。" },
    ],
    [
        { title: "Offset 与游标分页", detail: "Offset 分页实现简单，但页码越深扫描和丢弃的数据越多；游标分页以稳定排序键继续读取，性能更稳定，但不适合任意跳页。" },
        { title: "稳定排序", detail: "只按可能重复的 createdAt 排序会导致跨页重复或遗漏，应追加唯一 id 作为第二排序键，使相同数据在多次查询中保持确定顺序。" },
        { title: "校验的三个层次", detail: "DTO 校验负责格式，Service 校验业务规则，数据库约束守住最终一致性。三层职责不同，不能只依靠前端校验或用数据库异常代替所有业务提示。" },
    ],
    [
        { title: "认证与授权", detail: "认证回答“你是谁”，授权回答“你能做什么”。JWT 被验证后建立身份，但每个操作仍需检查角色、权限和资源归属。" },
        { title: "密码盐与自适应哈希", detail: "BCrypt 会为每个密码生成随机盐，并通过成本参数增加暴力破解代价。密码不能使用可逆加密，也不能直接保存普通 SHA-256 摘要。" },
        { title: "Token 吊销与刷新", detail: "无状态 JWT 在过期前通常持续有效。短访问令牌配合刷新令牌、版本号或吊销列表，可以在安全性、性能和立即退出需求之间折中。" },
    ],
    [
        { title: "MDC 生命周期", detail: "MDC 把 requestId 放入当前线程日志上下文。请求结束必须在 finally 中清除，否则线程池复用线程时，下一次请求可能继承错误的标识。" },
        { title: "结构化日志", detail: "稳定的字段名比拼接自然语言更容易检索和聚合。建议记录 event、requestId、userId、duration 和 result，同时避免密码、Token 与敏感正文。" },
        { title: "契约漂移", detail: "OpenAPI 文档与真实行为不一致称为契约漂移。应让注解、DTO、校验和测试共同约束状态码与 Schema，并在 CI 中生成或校验文档。" },
    ],
    [
        { title: "单元、切片与集成测试", detail: "单元测试隔离一个类，WebMvcTest 等切片测试加载部分 Spring 组件，集成测试验证完整协作。不同层次覆盖的风险不同，不应全部依赖重量级上下文。" },
        { title: "确定性测试", detail: "测试不应依赖真实时间、随机顺序或外部网络。通过注入 Clock、固定随机种子和本地替身，使同一输入每次得到相同结果。" },
        { title: "测试数据隔离", detail: "每个测试应独立创建所需数据并清理状态，不能依赖执行顺序。事务回滚、唯一数据库实例或明确清理策略可以避免相互污染。" },
    ],
    [
        { title: "软删除与唯一约束", detail: "软删除只是修改标记，原行仍参与普通唯一索引。可使用部分唯一索引、复合约束或业务规则，明确删除后同名数据是否允许重新创建。" },
        { title: "审计保留策略", detail: "删除时间、操作者和原因属于审计信息，应定义保留期限和访问权限。无限保留会增加存储、隐私与合规成本。" },
        { title: "物理清理与外键", detail: "后台清理软删除数据时必须考虑附件、日志等关联关系。数据库外键的 CASCADE、RESTRICT 与应用层清理顺序需要保持一致。" },
    ],
    [
        { title: "领域异常与技术异常", detail: "TodoNotFound、DuplicateTitle 表达可预期业务失败；数据库连接中断属于技术故障。两类异常的状态码、日志级别和对外信息不应相同。" },
        { title: "错误响应与追踪标识", detail: "错误响应可包含稳定 errorCode、可读 message、字段 errors 和 requestId。requestId 让用户反馈能够对应服务端日志，但不应返回内部堆栈。" },
        { title: "错误契约演进", detail: "前端通常依赖错误码做分支处理，因此错误码比文案更稳定。新增错误码通常安全，重命名或复用旧错误码会产生隐蔽兼容问题。" },
    ],
    [
        { title: "事务隔离级别", detail: "READ COMMITTED、REPEATABLE READ、SERIALIZABLE 对脏读、不可重复读和幻读提供不同保护。隔离越强并发代价通常越高，应按业务不变量选择。" },
        { title: "乐观锁", detail: "@Version 在更新时比较版本号，可以发现其他事务已经修改同一行。冲突后应返回明确错误或基于新数据重试，而不是静默覆盖。" },
        { title: "提交后副作用", detail: "邮件、消息等副作用若在事务提交前执行，数据库回滚后可能留下外部结果。afterCommit 适合提交后触发；要求可靠投递时应进一步使用 Outbox。" },
    ],
    [
        { title: "缓存穿透", detail: "大量查询不存在的 key 会持续打到数据库。可缓存短 TTL 的空结果、使用布隆过滤器，并限制恶意请求，但要避免把临时不存在缓存太久。" },
        { title: "缓存击穿与雪崩", detail: "热点 key 失效时并发回源称为击穿，大量 key 同时过期称为雪崩。互斥重建、随机 TTL 和提前刷新可以降低瞬时压力。" },
        { title: "驱逐与冷启动", detail: "容量不足时缓存会按策略驱逐数据；应用重启或缓存清空后进入冷启动。性能测试必须覆盖低命中率阶段，而不只观察稳定命中状态。" },
    ],
    [
        { title: "Key 命名空间", detail: "Redis Key 应包含应用、环境、业务、版本和实体标识，例如 todo-api:prod:todo:v1:userId:id，避免跨环境冲突并支持结构升级。" },
        { title: "序列化兼容", detail: "缓存中的 JSON 或二进制对象可能比应用版本存活更久。字段改名和类结构变化会造成反序列化失败，应加入版本、容错读取或主动换 Key。" },
        { title: "Redis 故障降级", detail: "缓存不可用时可回源数据库，但必须限制并发，防止数据库被瞬间压垮。缓存是性能组件时通常不应让一次 Redis 故障直接阻断核心读取。" },
    ],
    [
        { title: "线程上下文传播", detail: "异步线程不会自动拥有原线程的 SecurityContext、MDC 和事务。需要显式传递必要信息，并避免把完整可变请求对象交给后台线程。" },
        { title: "背压与拒绝策略", detail: "生产任务速度超过消费能力时，队列会持续增长。有界队列配合 Abort、CallerRuns 等拒绝策略，让过载变得可观察并限制资源占用。" },
        { title: "事件语义", detail: "事件描述已经发生的事实，应使用 TodoCreated 而不是 CreateTodo。监听器不应偷偷承担主业务成功所必需、却没有可靠性保障的步骤。" },
    ],
    [
        { title: "fixedRate 与 fixedDelay", detail: "fixedRate 按开始时间间隔调度，任务过慢可能积压；fixedDelay 从上一次结束后再等待。cron 适合日历时间，但必须明确时区。" },
        { title: "游标批处理", detail: "按 id > lastId 排序读取比深 offset 更稳定。每批完成后保存游标，失败可从最近检查点继续，并避免修改作为游标的字段。" },
        { title: "多实例调度", detail: "应用扩容后每个实例都会触发 @Scheduled。可使用数据库锁、ShedLock、分片或独立调度平台，且任务本身仍应保持幂等。" },
    ],
    [
        { title: "SLI、SLO 与告警", detail: "SLI 是延迟、错误率等实际指标，SLO 是目标值。告警应针对用户影响和持续时间，而不是任意一次瞬时波动。" },
        { title: "Readiness 与 Liveness", detail: "Liveness 判断进程是否需要重启，Readiness 判断是否可接收流量。把临时数据库故障写进 Liveness 可能造成所有实例反复重启。" },
        { title: "指标基数", detail: "把 userId、完整 URL 等高变化值作为指标标签会产生海量时间序列。指标标签应使用有限枚举，具体请求定位交给日志和 Trace。" },
    ],
    [
        { title: "十二要素配置", detail: "可部署程序应把环境差异放在外部配置中，同一构建产物可运行于开发、测试和生产。敏感值由密钥系统或环境注入。" },
        { title: "优雅停机", detail: "收到终止信号后先停止接收新请求，再等待进行中的请求和后台任务结束。超时后才强制退出，减少发布期间的半完成操作。" },
        { title: "日志级别与滚动", detail: "ERROR 表示需要处理的故障，WARN 表示异常但可继续，INFO 记录关键业务事件，DEBUG 用于诊断。生产日志还需按大小或时间滚动并设置保留期。" },
    ],
    [
        { title: "流式上传与内存", detail: "把整个文件读入 byte[] 会让大文件占满堆内存。流式复制应设置大小上限、超时和临时目录，并在失败时清理部分文件。" },
        { title: "Magic Bytes 校验", detail: "Content-Type 和扩展名都由客户端提供，不能证明真实格式。图片、PDF 等应检查文件头特征，并在需要时使用专门解析库二次验证。" },
        { title: "对象存储与签名 URL", detail: "生产附件通常存入 S3 类对象存储，数据库只保存元数据和对象 Key。短期签名 URL 可以避免应用服务器传输大文件，同时限制访问时间。" },
    ],
    [
        { title: "RBAC 与 ABAC", detail: "RBAC 根据角色授予权限，ABAC 还考虑资源、部门、时间等属性。简单系统优先 RBAC，复杂规则可在角色判断后增加属性条件。" },
        { title: "最小权限原则", detail: "用户和服务只获得完成工作所需的最少权限。管理员能力应拆分并审计，不能因为登录成功就默认拥有所有资源访问权。" },
        { title: "默认拒绝", detail: "安全配置应先拒绝，再显式放行公开端点。URL 规则适合粗粒度保护，@PreAuthorize 可结合方法参数和资源归属做细粒度判断。" },
    ],
    [
        { title: "Mock、Stub 与 Fake", detail: "Stub 提供预设返回，Mock 还验证交互，Fake 是可工作的简化实现。选择越接近真实行为，测试通常越稳定，但搭建成本也更高。" },
        { title: "状态验证与交互验证", detail: "状态验证关注返回值和数据变化，交互验证关注依赖是否被调用。只有发送消息、禁止写入等协作本身属于契约时，才重点 verify 调用。" },
        { title: "变异测试思维", detail: "把条件取反或删除关键语句后，测试应失败。即使不使用变异测试工具，也可短暂破坏实现来判断断言是否真正保护业务规则。" },
    ],
    [
        { title: "覆盖索引", detail: "查询需要的过滤列、排序列和返回列都能从索引获得时，可减少回表读取。覆盖索引更大，会增加写入成本，应服务于高价值查询。" },
        { title: "SARGable 条件", detail: "对索引列直接比较通常可使用索引；在列上执行函数、隐式类型转换或前置通配符 LIKE 可能使数据库无法有效定位范围。" },
        { title: "Keyset 深分页", detail: "使用 (created_at, id) < (?, ?) 继续下一页，数据库可以从索引位置向后扫描。它避免大 OFFSET，但客户端必须保存上一页游标。" },
    ],
    [
        { title: "镜像层缓存", detail: "Dockerfile 每条指令通常产生一层。先复制稳定的依赖描述并下载依赖，再复制源码构建，可以在源码变化时复用依赖层。" },
        { title: "PID 1 与信号", detail: "容器主进程作为 PID 1 接收停止信号。使用 exec 形式 ENTRYPOINT 能让 Java 正确收到 SIGTERM，从而执行 Spring Boot 优雅停机。" },
        { title: "多阶段构建与非 root", detail: "构建阶段包含 Maven/JDK，运行阶段只保留 JRE 和 jar，可减小镜像与攻击面。运行用户应是普通用户，并只授予必要目录权限。" },
    ],
    [
        { title: "规范化与反规范化", detail: "规范化减少重复和更新异常，反规范化用冗余换取读取效率。是否冗余应由真实查询与一致性策略决定，而不是提前猜测性能问题。" },
        { title: "MVCC", detail: "PostgreSQL 通过多版本并发控制让读取看到一致快照，更新产生新版本。长事务会阻碍旧版本清理，造成表膨胀和 Vacuum 压力。" },
        { title: "timestamp 与 timestamptz", detail: "timestamptz 保存时间点并按会话时区显示，timestamp 不包含时区语义。跨地区系统通常保存 UTC 时间点，并在展示层转换。" },
    ],
    [
        { title: "Fencing Token", detail: "分布式锁过期后，旧持有者可能继续执行。递增 fencing token 让下游拒绝较旧编号的写入，比单纯延长锁 TTL 更能防止过期持有者。" },
        { title: "令牌桶与滑动窗口", detail: "令牌桶允许一定突发流量，滑动窗口更精确统计最近时间段请求。算法选择取决于是否允许突发、精度需求和 Redis 操作成本。" },
        { title: "幂等记录状态机", detail: "幂等键不应只保存“见过”。可记录 PROCESSING、SUCCEEDED、FAILED 和响应摘要，区分处理中重试、已完成回放与允许重新执行。" },
    ],
    [
        { title: "Kafka 顺序与重平衡", detail: "Kafka 只保证单 Partition 顺序。消费者加入或离开会触发 Rebalance，处理中的 Partition 被转移前应正确提交或放弃 Offset，避免重复和长暂停。" },
        { title: "Exchange 类型与 Prefetch", detail: "RabbitMQ direct 按 routing key 精确匹配，topic 支持模式，fanout 广播。Prefetch 限制每个消费者未确认消息数，避免单个消费者囤积任务。" },
        { title: "消息 Schema 演进", detail: "生产者与消费者可能不同版本并存。新增可选字段通常向后兼容，删除或改变语义风险更高；应使用版本字段或 Schema Registry 管理契约。" },
    ],
    [
        { title: "架构决策记录 ADR", detail: "ADR 用背景、备选方案、决定和后果保存关键取舍。它解释为什么使用当前方案，避免后来者只看到代码却不知道约束。" },
        { title: "内聚与耦合", detail: "高内聚表示一个模块围绕单一目的，低耦合表示模块通过稳定边界协作。重构目标不是类越多越好，而是变化影响范围更可控。" },
        { title: "发布与回滚清单", detail: "交付前检查迁移兼容性、配置、健康检查、监控和回滚路径。数据库破坏性变更应采用扩展—迁移—收缩步骤，避免旧版本无法回滚。" },
    ],
];

const sourceLessons = [
    {
        file: "src/main/java/com/zading/todoapi/TodoApiApplication.java",
        language: "JAVA",
        code: `@EnableCaching
@ConfigurationPropertiesScan
@SpringBootApplication
public class TodoApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TodoApiApplication.class, args);
    }
}`,
        notes: [
            "@SpringBootApplication 同时启用自动配置、组件扫描和配置类能力，扫描起点是当前包 com.zading.todoapi。",
            "@ConfigurationPropertiesScan 发现项目中的配置 record，并把 application.properties 的值绑定为类型安全对象。",
            "SpringApplication.run 创建 Spring 容器、启动内嵌 Tomcat，并完成 Bean 装配；main 方法只是应用入口。",
        ],
        configFile: "pom.xml",
        configs: [
            { key: "java.version", value: "21", detail: "Maven 编译与运行目标为 JDK 21，IDE 选择的 JDK 也要保持一致。" },
            { key: "spring-boot-starter-parent", value: "3.3.2", detail: "统一管理 Spring 依赖版本和常用 Maven 插件默认值。" },
            { key: "spring-boot-maven-plugin", value: "enabled", detail: "负责生成可以用 java -jar 直接启动的 Spring Boot JAR。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/model/Todo.java",
        language: "JAVA",
        code: `@Entity
@Table(name = "todos")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private TodoPriority priority = TodoPriority.MEDIUM;
}`,
        notes: [
            "Todo 是一个类，也是描述 Todo 状态的对象；字段把标题、完成状态和优先级封装在同一个模型中。",
            "Long 是引用类型，新增对象时 id 可以为 null；保存后再由数据库生成主键值。",
            "priority 在对象创建时拥有 MEDIUM 默认值，业务代码仍可通过 setter 显式覆盖它。",
        ],
        configFile: "pom.xml",
        configs: [
            { key: "spring-boot-starter-data-jpa", value: "compile", detail: "提供 Entity、Repository 和 Hibernate 对象关系映射能力。" },
            { key: "h2", value: "runtime", detail: "只在运行时提供默认本地数据库，业务源码不直接依赖 H2 类。" },
            { key: "spring-boot-starter-validation", value: "compile", detail: "提供 @NotBlank、@Size 等 Jakarta Validation 注解。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/controller/TodoController.java",
        language: "JAVA",
        code: `private final TodoService todoService;
private final TodoMapper todoMapper;
private final TodoActionLogMapper todoActionLogMapper;
private final TodoSortParser todoSortParser;
private final RateLimiter rateLimiter;
private final RedisProtectionProperties redisProperties;

public TodoController(
        TodoService todoService,
        TodoMapper todoMapper,
        TodoActionLogMapper todoActionLogMapper,
        TodoSortParser todoSortParser,
        RateLimiter rateLimiter,
        RedisProtectionProperties redisProperties
) {
    this.todoService = todoService;
    this.todoMapper = todoMapper;
    this.todoActionLogMapper = todoActionLogMapper;
    this.todoSortParser = todoSortParser;
    this.rateLimiter = rateLimiter;
    this.redisProperties = redisProperties;
}`,
        notes: [
            "Controller 依赖 Service 处理业务、依赖 Mapper 转换响应，不直接编写数据库查询。",
            "final 表示依赖在构造完成后不能被替换，避免对象处于依赖缺失的半初始化状态。",
            "Spring 找到唯一构造器后自动注入 Bean；测试时也可以直接传入 Mock，不必启动整个容器。",
        ],
        configFile: "src/main/resources/application.properties",
        configs: [
            { key: "component scan", value: "com.zading.todoapi.*", detail: "启动类位于根包，因此 Controller、Service、Repository 都处于默认扫描范围内。" },
            { key: "spring.jpa.open-in-view", value: "false", detail: "数据库访问集中在 Service 事务中，Controller 不应隐式触发懒加载。" },
            { key: "spring.jpa.hibernate.ddl-auto", value: "validate", detail: "应用只验证表结构，不让 Hibernate 绕过 Flyway 自动修改数据库。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/controller/TodoController.java",
        language: "JAVA",
        code: `@RestController
@RequestMapping("/api/todos")
public class TodoController {
    @PostMapping
    public ResponseEntity<ApiResponse<TodoResponse>> createTodo(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateTodoRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false)
            String idempotencyKey
    ) {
        Todo createdTodo = todoService.addTodo(
                currentUser.getId(),
                request.getTitle(),
                request.getPriority(),
                request.getDueDate(),
                idempotencyKey
        );
    }
}`,
        notes: [
            "@RequestMapping 定义资源根路径，@PostMapping 再把方法绑定到 POST /api/todos。",
            "@RequestBody 让 Jackson 把 JSON 转成 CreateTodoRequest，@Valid 随后执行字段校验。",
            "创建接口使用 ResponseEntity 返回 201 和 Location，而不是所有情况都固定返回 200。",
        ],
        configFile: "src/main/resources/application.properties",
        configs: [
            { key: "server.port", value: "8080", detail: "内嵌 Tomcat 监听端口，对应本地访问地址 http://localhost:8080。" },
            { key: "springdoc.api-docs.path", value: "/v3/api-docs", detail: "输出 OpenAPI JSON，前端工具可以读取它生成接口客户端。" },
            { key: "springdoc.swagger-ui.path", value: "/swagger-ui.html", detail: "提供可视化接口查看和调试页面。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/repository/TodoRepository.java",
        language: "JAVA",
        code: `public interface TodoRepository
        extends JpaRepository<Todo, Long> {

    Page<Todo> findByUserIdAndCompletedAndDeletedFalse(
            Long userId,
            boolean completed,
            Pageable pageable
    );
}`,
        notes: [
            "JpaRepository<Todo, Long> 声明实体类型和主键类型，并继承 save、findById、existsById 等通用方法。",
            "Spring Data 把方法名拆成 userId、completed、deleted=false 三个查询条件，不需要手写实现类。",
            "Pageable 会把页码、大小和排序追加到最终 SQL，返回的 Page 还包含总条数等分页元数据。",
        ],
        configFile: "src/main/resources/application.properties",
        configs: [
            { key: "spring.datasource.url", value: "jdbc:h2:file:./data/todo-db-v2;MODE=PostgreSQL", detail: "默认数据保存到项目 data 目录，并启用 PostgreSQL 兼容模式。" },
            { key: "spring.jpa.show-sql", value: "true", detail: "学习环境输出 Hibernate SQL，便于把 Repository 方法与真实查询对应起来。" },
            { key: "spring.jpa.hibernate.ddl-auto", value: "validate", detail: "Entity 与表不一致时启动失败，结构修改必须通过迁移脚本完成。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/dto/CreateTodoRequest.java",
        language: "JAVA",
        code: `public class CreateTodoRequest {
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 100, message = "任务标题最多 100 个字符")
    private String title;

    @FutureOrPresent(message = "截止日期不能早于今天")
    private LocalDate dueDate;
}`,
        notes: [
            "输入 DTO 只描述客户端允许提交的字段，不把 Entity 的 id、user、deleted 等内部字段暴露出去。",
            "@NotBlank 同时拒绝 null、空字符串和纯空格；@Size 约束接口长度，数据库仍有自己的最终约束。",
            "@FutureOrPresent 使用当前日期验证 dueDate；Controller 参数上的 @Valid 才会触发这些规则。",
        ],
        configFile: "src/main/resources/application.properties",
        configs: [
            { key: "spring.flyway.enabled", value: "true", detail: "应用启动时先校验并执行版本化数据库迁移。" },
            { key: "spring.flyway.locations", value: "classpath:db/migration", detail: "V1、V2 等 SQL 文件从该目录按版本顺序加载。" },
            { key: "spring.jpa.hibernate.ddl-auto", value: "validate", detail: "Flyway 负责改表，JPA 只检查 DTO 对应的 Entity 能否正确映射。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/controller/TodoSortParser.java",
        language: "JAVA",
        code: `if (!ALLOWED_SORT_FIELDS.contains(field)) {
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

return requestedSort;`,
        notes: [
            "排序白名单阻止客户端传入不存在、敏感或代价过高的属性名。",
            "非唯一字段排序时追加 id，避免相同 title 或 createdAt 的记录在翻页时顺序漂移。",
            "解析逻辑独立成组件后，Controller 只负责接收参数，排序规则可以单独测试。",
        ],
        configFile: "src/main/java/com/zading/todoapi/controller/TodoController.java",
        configs: [
            { key: "page", value: "default 0, min 0", detail: "Spring Data 页码从 0 开始，负数会被参数校验拒绝。" },
            { key: "size", value: "default 10, range 1..100", detail: "设置上限避免一次请求读取过多数据。" },
            { key: "sort", value: "id,asc", detail: "默认使用唯一主键稳定排序，也支持白名单中的业务字段。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/security/JwtAuthenticationFilter.java",
        language: "JAVA",
        code: `String authHeader = request.getHeader("Authorization");
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);
    return;
}

String token = authHeader.substring(7);
String username = jwtService.extractUsername(token);`,
        notes: [
            "过滤器只处理 Bearer Token；没有 Token 时继续过滤链，最终由权限规则决定是否允许访问。",
            "substring(7) 去掉固定的 Bearer 加空格前缀，得到真正 JWT。",
            "验证签名、过期时间和角色后才会把 AuthenticatedUser 放入 SecurityContext，Controller 不能信任请求里的 userId。",
        ],
        configFile: "src/main/resources/application.properties",
        configs: [
            { key: "app.jwt.secret", value: "${JWT_SECRET:学习默认值}", detail: "优先读取环境变量；生产环境必须提供足够长的随机密钥。" },
            { key: "app.jwt.expiration-minutes", value: "120", detail: "访问令牌默认两小时过期，缩短时间能降低泄露后的可用窗口。" },
            { key: "SessionCreationPolicy", value: "STATELESS", detail: "服务端不创建登录 Session，每次请求都独立验证 Token。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/logging/RequestLoggingFilter.java",
        language: "JAVA",
        code: `String requestId = resolveRequestId(request);
MDC.put("requestId", requestId);
response.setHeader(properties.requestIdHeader(), requestId);

try {
    filterChain.doFilter(request, response);
} finally {
    MDC.remove("requestId");
}`,
        notes: [
            "客户端可以传入 requestId；没有提供时服务端生成 UUID，并通过响应头返回。",
            "MDC 让同一线程中的日志自动关联 requestId，便于从一次响应追到服务端日志。",
            "清理必须放在 finally 中，因为 Web 线程会被线程池复用，不清理会污染下一次请求。",
        ],
        configFile: "src/main/resources/application.properties",
        configs: [
            { key: "app.request-logging.enabled", value: "true", detail: "控制请求日志过滤器是否参与记录，可在测试环境关闭减少噪声。" },
            { key: "app.request-logging.request-id-header", value: "X-Request-Id", detail: "统一请求与响应使用的追踪头名称。" },
            { key: "logging.level.com.zading.todoapi", value: "INFO", detail: "默认输出关键业务日志，dev profile 会覆盖成 DEBUG。" },
        ],
    },
    {
        file: "src/test/java/com/zading/todoapi/TodoApiTests.java",
        language: "JAVA",
        code: `mockMvc.perform(get("/api/todos")
        .header("Authorization", authClient.bearer(token)))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.success").value(true))
    .andExpect(jsonPath("$.data.items", hasSize(1)))
    .andExpect(jsonPath("$.data.page").value(0));`,
        notes: [
            "MockMvc 在测试 JVM 内模拟 HTTP 请求，仍会经过 Controller、过滤器、序列化和异常处理。",
            "断言不仅检查 200，还检查统一响应结构、列表数量和分页元数据。",
            "Authorization 头使用真实注册登录得到的 Token，因此同时覆盖认证和用户隔离链路。",
        ],
        configFile: "src/test/resources/application-test.properties",
        configs: [
            { key: "spring.datasource.url", value: "jdbc:h2:mem:todo_api_test", detail: "每次测试进程使用内存数据库，不污染本地 data 目录。" },
            { key: "app.todo.overdue-job.enabled", value: "false", detail: "测试时关闭后台调度，避免定时线程修改测试数据。" },
            { key: "app.request-logging.enabled", value: "false", detail: "减少测试控制台日志，单独的 Smoke Test 会覆盖开启场景。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/service/TodoService.java",
        language: "JAVA",
        code: `@Transactional
public void deleteTodo(Long userId, Long id) {
    Todo todo = getTodo(userId, id);
    AppUser user = todo.getUser();
    todo.setDeleted(true);
    todo.setDeletedAt(LocalDateTime.now());

    Todo savedTodo = todoRepository.save(todo);
    addActionLog(savedTodo, user, TodoAction.DELETED, "删除 Todo");
}`,
        notes: [
            "软删除不执行 DELETE，而是写入 deleted 和 deletedAt，保留恢复与审计能力。",
            "getTodo 同时带 userId 和 deleted=false，保证用户只能删除自己的可见数据。",
            "Todo 修改和操作日志处于同一事务；中途抛出运行时异常时数据库变化一起回滚。",
        ],
        configFile: "src/main/resources/db/migration/V4__add_todo_lifecycle_fields.sql",
        configs: [
            { key: "deleted", value: "BOOLEAN NOT NULL DEFAULT FALSE", detail: "旧数据迁移后自动成为未删除状态，普通查询必须过滤该字段。" },
            { key: "deleted_at", value: "TIMESTAMP NULL", detail: "只有删除后的记录才保存删除时间，恢复时重新设为 null。" },
            { key: "completed_at", value: "TIMESTAMP NULL", detail: "完成状态之外保留发生时间，方便审计和统计。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/exception/GlobalExceptionHandler.java",
        language: "JAVA",
        code: `@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleBusinessException(
        BusinessException exception,
        HttpServletRequest request
) {
    ErrorCode code = exception.getErrorCode();
    return ResponseEntity.status(code.getHttpStatus())
            .body(ApiResponse.error(code, exception.getMessage(), request.getRequestURI()));
}`,
        notes: [
            "@RestControllerAdvice 让该方法统一处理所有 Controller 抛出的 BusinessException。",
            "异常携带 ErrorCode，ErrorCode 再决定 HTTP 状态，避免每个 Controller 重复写错误响应。",
            "响应只暴露稳定错误码、消息和路径；意外异常的完整堆栈只写服务端日志。",
        ],
        configFile: "src/main/java/com/zading/todoapi/exception/ErrorCode.java",
        configs: [
            { key: "TODO_NOT_FOUND", value: "404 NOT_FOUND", detail: "资源不存在，调用方不应把它当作服务器故障重试。" },
            { key: "CONCURRENT_UPDATE_CONFLICT", value: "409 CONFLICT", detail: "乐观锁冲突，客户端应刷新数据后再决定是否重试。" },
            { key: "RATE_LIMIT_EXCEEDED", value: "429 TOO_MANY_REQUESTS", detail: "请求频率超过限制，并可结合 retryAfterSeconds 提示等待。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/service/TodoService.java",
        language: "JAVA",
        code: `@Transactional
public Todo addTodo(
        Long userId,
        String title,
        TodoPriority priority,
        LocalDate dueDate
) {
    return addTodo(userId, title, priority, dueDate, null);
}`,
        notes: [
            "外部调用 public addTodo 时，Spring 事务代理先开启事务，方法正常结束后才提交。",
            "重载方法继续执行 addTodoInternal，Todo 保存和操作日志事件发布都处于已经开启的事务中。",
            "同类内部调用不会再经过一次代理，但这里不需要第二个事务边界；真正入口已经由 public 方法建立事务。",
        ],
        configFile: "事务与事件边界",
        configs: [
            { key: "@Transactional", value: "public Service method", detail: "Spring 代理在方法进入前开启事务，运行时异常默认触发回滚。" },
            { key: "TransactionPhase", value: "AFTER_COMMIT", detail: "需要外部副作用的监听器在数据库提交成功后再执行。" },
            { key: "spring.jpa.open-in-view", value: "false", detail: "事务结束后不允许 Web 层继续偷偷访问数据库。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/service/TodoService.java",
        language: "JAVA",
        code: `@Transactional(readOnly = true)
@Cacheable(
    cacheNames = CacheNames.TODO_DETAIL,
    key = "#userId + ':' + #id"
)
public Todo getTodo(Long userId, Long id) {
    return todoRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
            .orElseThrow(() -> new TodoNotFoundException(id));
}`,
        notes: [
            "第一次调用执行 Repository 并写入缓存；相同 key 再次调用可以直接返回缓存值。",
            "key 同时包含 userId 和 id，防止不同用户的同号资源发生缓存串读。",
            "更新、删除和恢复方法使用 @CacheEvict 清除旧值，否则接口可能持续返回修改前数据。",
        ],
        configFile: "src/main/resources/application.properties",
        configs: [
            { key: "spring.cache.type", value: "simple", detail: "默认使用当前 JVM 的内存缓存，不需要安装 Redis。" },
            { key: "CacheNames.TODO_DETAIL", value: "todo-detail", detail: "集中定义缓存名称，避免注解中散落拼写不同的字符串。" },
            { key: "cache key", value: "userId:todoId", detail: "缓存维度必须与数据库查询的用户隔离条件一致。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/config/RedisCacheConfig.java",
        language: "JAVA",
        code: `private static final Duration TODO_DETAIL_TTL = Duration.ofMinutes(10);
private static final Duration TODO_LOGS_TTL = Duration.ofMinutes(5);

return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(ttl)
        .disableCachingNullValues()
        .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()
                )
        )
        .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                        valueSerializer
                )
        );`,
        notes: [
            "不同缓存可配置不同 TTL：详情变化相对少，操作日志使用更短时间减少陈旧窗口。",
            "不缓存 null，避免一次暂时未查到的数据长期阻止后续真实数据出现。",
            "Key 使用字符串序列化，Value 使用 JSON；修改类结构时要考虑旧缓存反序列化兼容性。",
        ],
        configFile: "src/main/resources/application-redis.properties",
        configs: [
            { key: "spring.cache.type", value: "redis", detail: "启用 redis profile 后把 Spring Cache 底层从本机内存切换为 Redis。" },
            { key: "spring.data.redis.host", value: "${REDIS_HOST:localhost}", detail: "默认连接本机；容器或远程环境通过 REDIS_HOST 覆盖。" },
            { key: "spring.data.redis.timeout", value: "2s", detail: "连接或命令不能无限等待，故障时应尽快失败并进入降级逻辑。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/event/TodoActionLogEventListener.java",
        language: "JAVA",
        code: `@Async(AsyncConfig.TODO_TASK_EXECUTOR)
@TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
)
public void handleTodoActionLog(TodoActionLogEvent event) {
    todoMessagePublisher.publish(
            TodoActionLogMessage.from(event)
    );
}`,
        notes: [
            "@TransactionalEventListener(AFTER_COMMIT) 保证只有数据库提交成功才继续发送消息。",
            "@Async 把监听逻辑交给 todoTaskExecutor，HTTP 请求线程不用等待消息处理完成。",
            "异步线程不继承原事务；发布失败要由异步异常日志、重试或消息 Outbox 进一步处理。",
        ],
        configFile: "src/main/java/com/zading/todoapi/config/AsyncConfig.java",
        configs: [
            { key: "corePoolSize", value: "2", detail: "常驻两个工作线程处理异步任务。" },
            { key: "maxPoolSize / queueCapacity", value: "4 / 100", detail: "队列满后最多扩展到四个线程，仍需考虑最终拒绝策略。" },
            { key: "awaitTerminationSeconds", value: "5", detail: "应用关闭时最多等待五秒，让已接收任务尽量完成。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/job/TodoOverdueJob.java",
        language: "JAVA",
        code: `@Scheduled(
    cron = "\${app.todo.overdue-job.cron}",
    zone = "\${app.todo.overdue-job.zone:Asia/Shanghai}"
)
public void scanOverdueTodos() {
    Optional<LockHandle> lock = distributedLock.tryLock(
            LOCK_KEY,
            redisProperties.lockLease()
    );
}`,
        notes: [
            "cron 和时区来自外部配置，改变执行时间不需要重新修改 Java 代码。",
            "任务开始先获取分布式锁，多个应用实例中只有一个实例继续扫描。",
            "锁必须在 finally 中释放；处理逻辑本身仍需幂等，防止锁过期或进程异常导致重复执行。",
        ],
        configFile: "src/main/resources/application.properties",
        configs: [
            { key: "app.todo.overdue-job.enabled", value: "true", detail: "通过 ConditionalOnProperty 决定是否创建任务 Bean。" },
            { key: "app.todo.overdue-job.cron / zone", value: "0 0 9 * * * / Asia/Shanghai", detail: "每天上海时区 09:00 执行，Spring cron 包含秒字段。" },
            { key: "app.todo.overdue-job.page-size", value: "50", detail: "每批最多处理 50 条，避免一次加载所有过期记录。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/service/TodoOverdueJobStatusService.java",
        language: "JAVA",
        code: `private final AtomicReference<TodoOverdueJobStatus> status =
        new AtomicReference<>(TodoOverdueJobStatus.neverRun());

public void recordSuccess(LocalDate runDate, int count, long durationMs) {
    status.set(new TodoOverdueJobStatus(
            "todo-overdue", runDate, LocalDateTime.now(),
            true, count, durationMs, null
    ));
}`,
        notes: [
            "AtomicReference 让调度线程写入状态、HTTP 线程读取状态时保持原子可见。",
            "状态同时记录成功与否、处理数量和耗时，比只打印一条“任务完成”日志更可观测。",
            "当前状态只保存在单 JVM 内存中，多实例生产环境应聚合到指标、日志或共享存储。",
        ],
        configFile: "src/main/resources/application.properties",
        configs: [
            { key: "management.endpoints.web.exposure.include", value: "health,info,metrics", detail: "只暴露需要的 Actuator 端点，避免开放所有运维能力。" },
            { key: "management.endpoint.health.show-details", value: "never", detail: "匿名健康响应不泄露数据库地址和内部异常。" },
            { key: "management.endpoint.health.probes.enabled", value: "true", detail: "启用 liveness 和 readiness，供容器平台判断重启或流量接入。" },
        ],
    },
    {
        file: "src/main/resources/application-prod.properties",
        language: "PROPERTIES",
        code: `spring.datasource.url=\${DB_URL}
spring.datasource.username=\${DB_USERNAME}
spring.datasource.password=\${DB_PASSWORD}

spring.jpa.show-sql=false
spring.h2.console.enabled=false
app.jwt.secret=\${JWT_SECRET}
logging.level.org.springframework.security=WARN`,
        notes: [
            "没有冒号默认值的 ${DB_URL} 表示生产启动时必须提供环境变量，缺失就应该尽早失败。",
            "生产环境关闭 SQL 输出和 H2 Console，减少日志噪声与开发工具暴露风险。",
            "敏感值只写变量名，不把数据库密码和 JWT 密钥提交到 Git 或打进镜像。",
        ],
        configFile: "Profile 启动方式",
        configs: [
            { key: "spring.profiles.active", value: "prod", detail: "先加载公共 application.properties，再由 prod 文件覆盖同名项。" },
            { key: "DB_URL", value: "required", detail: "生产连接串由部署环境注入；Compose 中主机名应是 postgres 而非 localhost。" },
            { key: "FILE_STORAGE_ROOT", value: "uploads", detail: "可覆盖附件目录，容器环境通常设置为挂载卷 /app/uploads。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/service/TodoAttachmentService.java",
        language: "JAVA",
        code: `private Path resolveStoragePath(Path root, String storagePath) {
    Path resolved = root.resolve(storagePath).normalize();

    if (!resolved.startsWith(root)) {
        throw new BusinessException(
                ErrorCode.BAD_REQUEST,
                "附件路径不安全"
        );
    }
    return resolved;
}`,
        notes: [
            "resolve 后调用 normalize 消除路径中的 . 和 ..，得到规范化绝对位置。",
            "startsWith(root) 确认最终文件仍在允许目录内，阻止 ../../etc/passwd 一类路径穿越。",
            "磁盘写入成功但数据库保存失败时还要删除文件，否则会产生没有元数据引用的孤儿文件。",
        ],
        configFile: "src/main/resources/application.properties",
        configs: [
            { key: "app.file-storage.root-location", value: "uploads", detail: "相对路径以应用工作目录为基准；生产环境可改成持久化挂载目录。" },
            { key: "app.file-storage.max-file-size-bytes", value: "5242880", detail: "业务层单文件上限为 5 MiB，上传前先检查避免占用过多资源。" },
            { key: "FILE_STORAGE_ROOT", value: "/app/uploads (Docker)", detail: "Compose 把该目录挂载到 volume，删除容器后附件仍然保留。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/controller/AdminController.java",
        language: "JAVA",
        code: `@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
}`,
        notes: [
            "类级 @PreAuthorize 会保护该 Controller 的所有方法，普通 USER 在进入方法前得到 403。",
            "hasRole('ADMIN') 会查找 ROLE_ADMIN authority，UserRole.authority() 负责生成相同格式。",
            "管理员角色只解决功能权限；普通资源接口仍需通过 userId 检查数据归属。",
        ],
        configFile: "src/main/java/com/zading/todoapi/security/SecurityConfig.java",
        configs: [
            { key: "/api/admin/**", value: "hasRole('ADMIN')", detail: "URL 层先做粗粒度拦截，方法注解形成第二层保护。" },
            { key: "anyRequest", value: "authenticated", detail: "除明确 permitAll 的路径外，其余请求默认要求认证。" },
            { key: "@EnableMethodSecurity", value: "enabled", detail: "没有它，@PreAuthorize 注解不会参与方法调用鉴权。" },
        ],
    },
    {
        file: "src/test/java/com/zading/todoapi/service/TodoServiceTest.java",
        language: "JAVA",
        code: `@ExtendWith(MockitoExtension.class)
class TodoServiceTest {
    @Mock
    private TodoRepository todoRepository;

    @Test
    void shouldReturnUserTodoWhenTodoExists() {
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(expected));
        assertSame(expected, todoService.getTodo(1L, 10L));
    }
}`,
        notes: [
            "MockitoExtension 初始化 @Mock，不加载 Spring 容器，因此 Service 测试启动更快、失败范围更小。",
            "when(...).thenReturn(...) 为 Repository 设置当前案例需要的行为，未设置的调用不会访问数据库。",
            "测试同时应 verify 关键协作，并覆盖 Optional.empty、边界输入和不应发生的写入。",
        ],
        configFile: "测试配置",
        configs: [
            { key: "spring-boot-starter-test", value: "scope test", detail: "JUnit、Mockito、AssertJ 和 Spring Test 只进入测试类路径。" },
            { key: "Mockito MockMaker", value: "mock-maker-subclass", detail: "使用子类方式创建 Mock，配置位于 test resources。" },
            { key: "test naming", value: "should...When...", detail: "方法名直接表达行为和场景，让失败报告可以作为业务说明阅读。" },
        ],
    },
    {
        file: "src/main/resources/db/migration/V8__add_todo_query_indexes.sql",
        language: "SQL",
        code: `CREATE INDEX idx_todos_user_deleted_completed_id
    ON todos (user_id, deleted, completed, id);

CREATE INDEX idx_todos_deleted_completed_due_id
    ON todos (deleted, completed, due_date, id);`,
        notes: [
            "联合索引列顺序从常用等值过滤 user_id、deleted、completed 开始，最后放排序或范围相关列。",
            "第一个索引服务用户按完成状态分页，尾部 id 还能提供稳定排序。",
            "第二个索引服务过期扫描：过滤未删除、未完成记录，再按 due_date 范围定位。",
        ],
        configFile: "查询诊断配置",
        configs: [
            { key: "spring.jpa.show-sql", value: "true (learning)", detail: "先确认 ORM 实际生成什么 SQL，再分析索引是否匹配查询条件。" },
            { key: "hibernate.format_sql", value: "true", detail: "格式化输出提升可读性，但不能替代 EXPLAIN ANALYZE。" },
            { key: "index migration", value: "Flyway V8", detail: "索引属于数据库结构，应版本化部署并能在所有环境重复验证。" },
        ],
    },
    {
        file: "Dockerfile",
        language: "DOCKERFILE",
        code: `FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
COPY --from=build --chown=appuser:appuser \
    /workspace/target/java-todo-api-1.0.0.jar \
    /app/app.jar
USER appuser
ENTRYPOINT ["java", "-jar", "/app/app.jar"]`,
        notes: [
            "第一阶段包含 Maven 和 JDK，只负责下载依赖与编译；第二阶段只保留运行所需 JRE 和 JAR。",
            "先复制 pom.xml 能利用镜像层缓存，源码变化时不必重新下载所有 Maven 依赖。",
            "最终进程使用 appuser 而不是 root，exec 形式 ENTRYPOINT 让 Java 正确接收容器停止信号。",
        ],
        configFile: "docker-compose.yml",
        configs: [
            { key: "SPRING_PROFILES_ACTIVE", value: "prod", detail: "容器默认使用生产配置，并通过环境变量连接 PostgreSQL。" },
            { key: "APP_PORT", value: "8080:8080", detail: "左侧是宿主机端口，右侧是容器内 Spring Boot 端口。" },
            { key: "JAVA_TOOL_OPTIONS", value: "-XX:MaxRAMPercentage=75.0", detail: "JVM 最大堆按容器可用内存比例计算，为非堆内存保留空间。" },
        ],
    },
    {
        file: "src/main/resources/db/migration/V9__add_data_constraints_and_todo_version.sql",
        language: "SQL",
        code: `ALTER TABLE todos
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE todos
    ADD CONSTRAINT ck_todos_title_not_blank
    CHECK (TRIM(title) <> '');

ALTER TABLE todos
    ADD CONSTRAINT ck_todos_priority
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));`,
        notes: [
            "version 对应 Entity 的 @Version，Hibernate 更新时把旧版本放进 WHERE，检测并发覆盖。",
            "标题非空和优先级枚举下沉到数据库，即使写入绕过 Java API，也不能破坏核心规则。",
            "Java 校验负责友好提示，数据库约束负责最终兜底，两者不是互相替代。",
        ],
        configFile: "src/main/resources/application-postgres.properties",
        configs: [
            { key: "spring.datasource.url", value: "${DB_URL:jdbc:postgresql://localhost:5432/java_todo_api}", detail: "本地默认连接 PostgreSQL；容器环境必须把 localhost 改为服务名。" },
            { key: "spring.jpa.database-platform", value: "PostgreSQLDialect", detail: "让 Hibernate 生成适配 PostgreSQL 的 SQL 方言。" },
            { key: "spring.flyway.enabled", value: "true", detail: "PostgreSQL 与 H2 使用同一套版本迁移，持续验证兼容性。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/redis/RedisDistributedLock.java",
        language: "JAVA + LUA",
        code: `Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(key, handle.token(), leaseTime);

if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
end
return 0`,
        notes: [
            "setIfAbsent 对应 SET NX，并同时写入 TTL；只有第一个竞争者获得锁。",
            "锁值是随机 owner token，释放时先比较 token，防止旧持有者删除别人后来获得的新锁。",
            "比较与删除放在 Lua 中一次执行，避免 GET 成功后锁刚好过期、再误删新锁的竞态。",
        ],
        configFile: "src/main/resources/application.properties",
        configs: [
            { key: "app.redis.lock-lease", value: "5m", detail: "锁持有进程崩溃后最多保留五分钟，实际值应覆盖最慢正常任务。" },
            { key: "app.redis.idempotency-ttl", value: "10m", detail: "完成结果在 TTL 内可用于重复请求回放，过期后请求会被视为新的操作。" },
            { key: "app.redis.rate-limit-window", value: "1m", detail: "固定窗口计数的时间范围，Lua 确保 INCR 与 EXPIRE 原子执行。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/messaging/kafka/KafkaTodoMessageConsumer.java",
        language: "JAVA",
        code: `@RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1_000L)
)
@KafkaListener(
        topics = "\${app.messaging.kafka.topic}",
        groupId = "\${app.messaging.kafka.consumer-group}"
)
public void consume(TodoActionLogMessage message) {
    messageHandler.handle(message);
}`,
        notes: [
            "同一 consumer group 内，一个 Partition 同时只分配给一个消费者实例处理。",
            "业务异常向外抛出后由 RetryableTopic 重试三次，持续失败的消息最终进入 DLT。",
            "Kafka 和 RabbitMQ 都可能至少投递一次，messageHandler 必须用 messageId 等业务键处理重复消息。",
        ],
        configFile: "application-kafka.properties / application-rabbitmq.properties",
        configs: [
            { key: "spring.kafka.bootstrap-servers", value: "${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}", detail: "kafka profile 才连接 Broker；默认 profile 继续使用内存消息实现。" },
            { key: "spring.kafka.listener.ack-mode", value: "record", detail: "每条记录成功处理后提交 Offset，失败时交给重试与 DLT。" },
            { key: "rabbit retry / DLQ", value: "3 attempts → todo-action-log.dlq", detail: "RabbitMQ 重试耗尽后拒绝且不重新入队，由死信配置转发。" },
        ],
    },
    {
        file: "src/main/java/com/zading/todoapi/mapper/TodoMapper.java",
        language: "JAVA",
        code: `@Component
public class TodoMapper {
    public TodoResponse toResponse(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.isCompleted(),
                todo.isDeleted(),
                todo.getPriority(),
                todo.getDueDate(),
                todo.getCompletedAt(),
                todo.getDeletedAt(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }
}`,
        notes: [
            "Mapper 把持久化 Entity 转成接口 DTO，Controller 不需要逐字段拼装响应。",
            "接口只选择客户端需要的字段，不会意外序列化懒加载 user、内部版本号等数据。",
            "重构 Mapper 时可用现有接口测试证明 JSON 行为不变，这就是小步、可回归的结构改善。",
        ],
        configFile: "交付与构建配置",
        configs: [
            { key: "java.version", value: "21", detail: "README、CI、Dockerfile 和本地 IDE 都应使用同一 Java 大版本。" },
            { key: "mvn test", value: "required before delivery", detail: "交付前运行完整回归，不能只验证本次修改涉及的单个接口。" },
            { key: "spring-boot-maven-plugin", value: "package executable JAR", detail: "最终产物通过相同命令启动，部署文档应记录环境变量和健康检查。" },
        ],
    },
];

const dayPlans = [
    {
        label: "概念日", verb: "建立概念模型", skill: "把抽象术语翻译成项目中的真实职责",
        action: "阅读本周文档，写下核心术语、输入、输出、生命周期和使用场景。",
        verify: "不看文档，用自己的话讲清概念之间的关系，并举出项目中的对应位置。",
        questions: ["这个技术解决了项目中的哪个具体问题？", "它与相邻概念最容易混淆的区别是什么？", "如果不使用它，代码会出现什么症状？"],
    },
    {
        label: "读码日", verb: "跟踪源码链路", skill: "从入口追踪到最终数据或外部副作用",
        action: "从推荐文件进入，使用 IDE 跳转追踪一次完整调用，记录参数如何变化。",
        verify: "画出入口、业务层、数据层或外部服务之间的调用链，并标注每一步返回值。",
        questions: ["一次请求最先进入哪个类和方法？", "关键数据在哪一步被校验或改变？", "代码为什么属于这一层而不是相邻层？"],
    },
    {
        label: "实现日", verb: "完成最小功能", skill: "把需求拆成一个职责清晰、可以独立验证的改动",
        action: "基于本周主题完成一个小改动，控制在一次测试能够覆盖的范围内。",
        verify: "项目能够编译，改动产生可观察结果，成功路径符合预期。",
        questions: ["这次改动最小的职责边界在哪里？", "输入、输出和失败方式分别是什么？", "为什么选择改这里，而不是在调用方绕过问题？"],
    },
    {
        label: "边界日", verb: "处理异常和边界", skill: "在错误输入和依赖失败时保护响应与数据一致性",
        action: "主动制造空值、错误 ID、重复请求、无权限或外部服务失败。",
        verify: "失败响应明确，日志足以定位原因，数据没有留下半完成状态。",
        questions: ["最坏情况下会留下什么不一致数据？", "这个错误应该由哪一层转换或处理？", "调用方如何区分可重试和不可重试错误？"],
    },
    {
        label: "测试日", verb: "补齐测试保护", skill: "用成功、失败和边界案例描述稳定的业务行为",
        action: "为本周功能增加成功测试和失败测试，并减少重复的测试准备代码。",
        verify: "指定测试与完整 mvn test 都通过，故意破坏实现时测试能够失败。",
        questions: ["测试保护的是实现细节还是用户依赖的行为？", "哪个边界最可能在未来回归？", "这个 Mock 是否让测试过度了解内部实现？"],
    },
    {
        label: "重构日", verb: "整理职责和命名", skill: "在行为不变的前提下减少重复与理解成本",
        action: "检查方法长度、重复逻辑、命名和依赖方向，只选择一个坏味道重构。",
        verify: "外部接口行为不变，已有测试保持通过，代码意图比重构前清楚。",
        questions: ["重构后少理解了哪一层复杂度？", "这次变化是否混入了新功能？", "哪个测试证明外部行为没有变化？"],
    },
    {
        label: "复盘日", verb: "完成一周复盘", skill: "独立演示功能并解释实现、限制和下一步",
        action: "从需求到测试讲解本周能力，更新学习文档中的答案、错误记录和未解决问题。",
        verify: "能够独立演示功能，说出关键调用链、设计取舍和生产环境缺口。",
        questions: ["本周最重要的能力能否独立重做？", "哪次错误最值得保留在笔记中？", "如果进入生产环境，最先补哪项保护？"],
    },
];

// file:// 页面在部分浏览器或隐私模式下可能禁止访问 localStorage。
// 读取或保存失败时退化为内存状态，不影响首页本身的展示和切换功能。
const progressStorage = {
    get(key) {
        try {
            return window.localStorage.getItem(key);
        } catch {
            return null;
        }
    },
    set(key, value) {
        try {
            window.localStorage.setItem(key, value);
        } catch {
            // 离线页面仍可使用，只是不保存刷新后的学习位置。
        }
    },
};

const state = {
    week: Number(progressStorage.get("java-todo-current-week")) || 1,
    day: Number(progressStorage.get("java-todo-current-day")) || 1,
};

const weekSelect = document.querySelector("#week-select");
const daySelect = document.querySelector("#day-select");

function getCommands(week) {
    const commands = ["mvn -DskipTests compile", "mvn test"];

    if (week <= 19 || week === 22 || week === 28) {
        commands.unshift("mvn spring-boot:run");
    }
    if (week === 24) {
        commands.push("docker compose up -d");
    }
    if (week === 25) {
        commands.push("mvn spring-boot:run -Dspring-boot.run.profiles=postgres");
    }
    if (week === 26) {
        commands.push("mvn spring-boot:run -Dspring-boot.run.profiles=redis");
    }
    if (week === 27) {
        commands.push("mvn spring-boot:run -Dspring-boot.run.profiles=kafka");
    }

    return commands.slice(0, 3);
}

function getProjectFiles(week) {
    return week.files.filter((file) => !file.startsWith("docs/week-"));
}

function buildSpecificStudy(week, weekDetail, explanations, dayPlan) {
    const sourceFiles = getProjectFiles(week);
    const primaryFile = sourceFiles[0] || week.files[0];
    const secondaryFile = sourceFiles[1] || primaryFile;
    const concepts = weekDetail.concepts;

    const contentByDay = {
        1: [
            { title: `理解 ${concepts[0]}`, detail: explanations[0] },
            { title: `区分 ${concepts[1]} 与 ${concepts[2]}`, detail: `${explanations[1]} ${explanations[2]}` },
            { title: "对应到项目", detail: `打开 ${primaryFile}，指出 ${concepts[0]} 在当前代码中的具体入口、配置或数据结构。` },
            { title: "建立最小验证", detail: `${weekDetail.practice} 修改一个输入，预测结果后再运行代码验证。` },
        ],
        2: [
            { title: "确认调用起点", detail: `从 ${primaryFile} 开始，找到对外入口、注解、构造器依赖和第一个被调用的方法。` },
            { title: "追踪下一层", detail: `继续跳转到 ${secondaryFile}，记录参数名称、类型、校验位置以及返回值如何变化。` },
            { title: "观察关键机制", detail: `${concepts[3]}：${explanations[3]}` },
            { title: "画出完整链路", detail: `用“入口 → 业务规则 → 数据或外部副作用 → 响应”画图，并给每个箭头标注传递的数据。` },
        ],
        3: [
            { title: "先定义可观察行为", detail: `目标结果是：${week.deliverable} 先写出一个具体输入和期望输出，再开始改代码。` },
            { title: "确定最小改动点", detail: `优先在 ${primaryFile} 或 ${secondaryFile} 中找到职责匹配的位置，不在 Controller 或调用方临时绕过规则。` },
            { title: "实现核心路径", detail: `${weekDetail.practice} 每完成一个小步骤就执行编译，避免一次修改过多文件。` },
            { title: "解释实现依据", detail: `说明这次代码怎样使用 ${concepts[0]} 和 ${concepts[1]}，以及为什么没有选择另一种实现位置。` },
        ],
        4: [
            { title: "非法输入", detail: `围绕 ${concepts[0]} 设计空值、格式错误、越界或不存在数据，并预测具体异常或 HTTP 状态。` },
            { title: "重复与并发", detail: `连续执行两次同一操作，检查第二次是幂等、冲突还是重复产生副作用，并记录数据库状态。` },
            { title: "故障与一致性", detail: `在数据写入或外部操作中途制造失败，确认是否出现半完成数据、旧缓存或重复消息。` },
            { title: "本周高风险点", detail: weekDetail.pitfall },
        ],
        5: [
            { title: "成功案例", detail: `给定合法输入，断言“${week.deliverable}”对应的返回值、状态变化和关键依赖调用。` },
            { title: "失败案例", detail: `把“${weekDetail.pitfall}”转换成一条可重复执行的测试，断言失败类型和数据未被错误修改。` },
            { title: "边界案例", detail: `针对 ${concepts[2]} 选择一个最小值、最大值、空集合或重复值，写清边界内外的不同结果。` },
            { title: "测试有效性", detail: `临时破坏关键判断，确认新测试会失败；恢复代码后运行完整 mvn test，防止只写出永远通过的测试。` },
        ],
        6: [
            { title: "检查职责", detail: `阅读 ${primaryFile}，圈出与类名不相符的 HTTP、业务、存储或基础设施逻辑，只选择一个问题处理。` },
            { title: "改善命名", detail: `检查与 ${concepts[0]} 相关的方法和变量，名称应表达业务意图，而不是 process、data、handle 等模糊词。` },
            { title: "调整依赖方向", detail: `确认 ${primaryFile} 不反向依赖更上层的 Web 细节；必要时提取接口、DTO 或独立方法。` },
            { title: "证明行为不变", detail: `重构前后使用相同输入运行测试和接口，比较状态码、JSON、数据库变化及日志副作用。` },
        ],
        7: [
            { title: "闭卷解释", detail: `不用文档解释 ${concepts.join("、")}，每个概念至少说出定义、项目位置和一个错误用法。` },
            { title: "完整演示", detail: `从项目启动开始，演示一条成功路径、一条失败路径，并用日志或数据库证明内部行为。` },
            { title: "复述调用链", detail: `从 ${primaryFile} 出发讲到最终数据或副作用，说明每一层为什么存在以及事务或权限边界在哪里。` },
            { title: "评估生产缺口", detail: `结合“${weekDetail.pitfall}”说明当前实现还缺少的监控、性能、安全或故障恢复能力。` },
        ],
    };

    return contentByDay[state.day].map((item, index) => ({
        ...item,
        label: `${dayPlan.label} · ${String(index + 1).padStart(2, "0")}`,
    }));
}

function describeFile(file) {
    if (file.endsWith(".java")) {
        return `打开 ${file}：先看类注解和构造器依赖，再看 public 方法的参数、返回值、异常和调用对象。`;
    }
    if (file.endsWith(".sql")) {
        return `打开 ${file}：逐句对应表、列、约束或索引，并写出它会改变的数据结构。`;
    }
    if (file.endsWith(".properties")) {
        return `打开 ${file}：找出配置键、默认值和启用条件，再定位读取这些配置的 Java 类。`;
    }
    if (file === "pom.xml") {
        return "打开 pom.xml：找到相关 dependency 和 plugin，说明它们在 compile、test 或 package 哪个阶段生效。";
    }
    if (file === "Dockerfile" || file.endsWith("compose.yml")) {
        return `打开 ${file}：按执行顺序解释构建阶段、运行命令、端口、环境变量与数据卷。`;
    }
    return `打开 ${file}：先列出目录或章节，再选择与今天主题最相关的一条链路继续追踪。`;
}

function buildCodePath(week) {
    const paths = [
        `先读 ${week.files[0]} 中与“${week.title}”对应的概念、代码说明和复盘问题，列出三个需要在源码中确认的结论。`,
        ...getProjectFiles(week).map(describeFile),
    ];

    if (paths.length < 4) {
        paths.push("使用 IDE 的“转到定义”和“查找引用”，确认调用方、实现类和测试分别在哪里。");
        paths.push("从一个真实请求或方法调用出发，记录输入值在每一层的变化，直到返回结果或产生副作用。");
    }
    return paths.slice(0, 4);
}

function buildLabSteps(week, weekDetail, dayPlan) {
    const primaryFile = getProjectFiles(week)[0] || week.files[0];
    const command = getCommands(week.week)[0];
    const commonLastStep = `运行 ${command} 和 mvn test，记录命令、关键输出以及与你预期不同的地方。`;

    const labsByDay = {
        1: [
            `在笔记中为 ${weekDetail.concepts.slice(0, 3).join("、")} 各写一句定义，并画出三者关系。`,
            `在 ${primaryFile} 中为每个概念找到一个真实代码位置，记下类名或方法名。`,
            weekDetail.practice,
            commonLastStep,
        ],
        2: [
            `选择 ${primaryFile} 中一个 public 方法作为起点，写下具体输入和预期返回。`,
            "用 IDE 逐次跳转，并为每一层记录：收到什么、检查什么、调用谁、返回什么。",
            "运行一次真实调用，对照日志、断点或 SQL，修正自己画出的调用链。",
            "把最终调用链画进本周文档，要求另一位开发者可以按图找到每个文件。",
        ],
        3: [
            `把“${week.deliverable}”拆成一个最小成功案例和明确的不做事项。`,
            `只在职责匹配的文件中实现：优先检查 ${primaryFile}，每次修改后执行编译。`,
            weekDetail.practice,
            commonLastStep,
        ],
        4: [
            `先正常执行一次“${week.deliverable}”相关流程，保存成功基线。`,
            `分别输入非法值、重复请求和不存在的 ID，记录响应、日志和数据库变化。`,
            `针对“${weekDetail.pitfall}”制造一次失败，确认没有留下半完成状态。`,
            "把每个失败场景整理为：触发条件 → 当前结果 → 期望结果 → 应由哪一层负责。",
        ],
        5: [
            `为“${week.deliverable}”补一条成功测试，断言业务输出而不只断言方法被调用。`,
            `围绕“${weekDetail.pitfall}”补一条失败测试，并断言数据或依赖没有错误副作用。`,
            `为 ${weekDetail.concepts[2]} 补一条边界测试，使用清楚的 given_when_then 命名。`,
            "故意破坏一处关键逻辑确认测试会失败，恢复后运行 mvn test。",
        ],
        6: [
            `在 ${primaryFile} 中只选择一个坏味道：重复、过长方法、错误命名或职责混乱。`,
            "先运行相关测试保存基线，再做一次提取方法、改名或移动职责。",
            "检查调用方和依赖方向，不借重构偷偷加入新功能。",
            "再次运行相同测试，并在文档中写出重构前后理解成本的变化。",
        ],
        7: [
            `闭卷回答本周复盘题，再对照 ${week.files[0]} 修正不准确的表述。`,
            `完整演示“${week.deliverable}”，同时展示一个失败场景。`,
            `从 ${primaryFile} 开始口述调用链，并解释 ${weekDetail.concepts[0]} 在哪里生效。`,
            "更新本周文档：保留错误记录、验证证据、生产缺口和下一周要复习的一个问题。",
        ],
    };

    return labsByDay[state.day];
}

function getExpectedResult(week, weekDetail, dayPlan) {
    const expectedByDay = {
        1: `能脱离文档准确解释 ${weekDetail.concepts.slice(0, 3).join("、")}，并在项目中指出各自位置。`,
        2: "得到一张包含入口、参数变化、业务规则、数据访问和返回结果的调用链图。",
        3: `${week.deliverable} 项目能够编译，真实调用与预先写下的输入输出一致。`,
        4: "至少记录三个可重复的失败场景；错误响应明确，日志可定位，数据没有半完成状态。",
        5: "新增成功、失败和边界测试；故意破坏逻辑时测试失败，恢复后完整测试通过。",
        6: "只改善一个代码坏味道；外部接口、数据库结果和测试行为与重构前一致。",
        7: `不依赖提示完成“${week.deliverable}”演示，并能回答本周概念、调用链和生产缺口。`,
    };
    return `${expectedByDay[state.day]} ${dayPlan.verify}`;
}

function renderSourceLesson(lesson) {
    document.querySelector("#source-file").textContent = lesson.file;
    document.querySelector("#source-language").textContent = lesson.language;
    document.querySelector("#config-file").textContent = lesson.configFile;

    const codeBlock = document.querySelector("#source-code");
    codeBlock.replaceChildren(...lesson.code.split("\n").map((line, index) => {
        const row = document.createElement("span");
        const lineNumber = document.createElement("span");
        const lineContent = document.createElement("span");

        row.className = "code-line";
        lineNumber.className = "line-number";
        lineContent.className = "line-content";
        lineNumber.textContent = String(index + 1).padStart(2, "0");
        lineContent.textContent = line || " ";
        row.append(lineNumber, lineContent);
        return row;
    }));

    renderOrderedList("#source-note-list", lesson.notes);

    const configList = document.querySelector("#config-list");
    configList.replaceChildren(...lesson.configs.map((config) => {
        const item = document.createElement("article");
        const key = document.createElement("code");
        const value = document.createElement("strong");
        const detail = document.createElement("p");

        item.className = "config-item";
        key.className = "config-key";
        value.className = "config-value";
        key.textContent = config.key;
        value.textContent = config.value;
        detail.textContent = config.detail;
        item.append(key, value, detail);
        return item;
    }));
}

function renderKnowledge(weekDetail, explanations, advancedItems) {
    const list = document.querySelector("#knowledge-list");
    const knowledgeItems = [
        ...weekDetail.concepts.map((title, index) => ({ title, detail: explanations[index], advanced: false })),
        ...advancedItems.map((item) => ({ ...item, advanced: true })),
    ];

    list.replaceChildren(...knowledgeItems.map((knowledge, index) => {
        const article = document.createElement("article");
        const number = document.createElement("span");
        const copy = document.createElement("div");
        const title = document.createElement("h5");
        const detail = document.createElement("p");

        if (knowledge.advanced) {
            article.classList.add("advanced");
        }
        number.textContent = String(index + 1).padStart(2, "0");
        title.textContent = knowledge.title;
        detail.textContent = knowledge.detail;
        copy.append(title, detail);
        article.append(number, copy);
        return article;
    }));
}

function renderSpecificStudy(items) {
    const list = document.querySelector("#specific-study-list");
    list.replaceChildren(...items.map((item) => {
        const article = document.createElement("article");
        const label = document.createElement("span");
        const copy = document.createElement("div");
        const title = document.createElement("h5");
        const detail = document.createElement("p");

        label.textContent = item.label;
        title.textContent = item.title;
        detail.textContent = item.detail;
        copy.append(title, detail);
        article.append(label, copy);
        return article;
    }));
}

function renderOrderedList(selector, items) {
    const list = document.querySelector(selector);
    list.replaceChildren(...items.map((text) => {
        const item = document.createElement("li");
        item.textContent = text;
        return item;
    }));
}

function renderObjectives(week, weekDetail, dayPlan) {
    const objectives = [
        `说清楚 ${weekDetail.concepts.slice(0, 3).join("、")} 之间的关系。`,
        `${dayPlan.skill}，并能在项目代码中指出对应位置。`,
        `留下可验证的产出：${week.deliverable}`,
    ];
    const list = document.querySelector("#objective-list");
    list.replaceChildren(...objectives.map((objective) => {
        const item = document.createElement("li");
        item.textContent = objective;
        return item;
    }));
}

function renderConcepts(weekDetail, advancedItems) {
    const list = document.querySelector("#concept-list");
    const concepts = [
        ...weekDetail.concepts.map((title) => ({ title, advanced: false })),
        ...advancedItems.map((item) => ({ title: item.title, advanced: true })),
    ];
    list.replaceChildren(...concepts.map((concept) => {
        const item = document.createElement("span");
        item.textContent = concept.title;
        if (concept.advanced) {
            item.classList.add("advanced");
        }
        return item;
    }));
}

function renderReviewQuestions(week, weekDetail, dayPlan) {
    const questions = [
        dayPlan.questions[0],
        `在“${week.title}”中，${weekDetail.concepts[0]} 与 ${weekDetail.concepts[1]} 是怎样协作的？`,
        dayPlan.questions[2],
    ];
    const list = document.querySelector("#review-list");
    list.replaceChildren(...questions.map((question) => {
        const item = document.createElement("li");
        item.textContent = question;
        return item;
    }));
}

function renderRoadmap() {
    const container = document.querySelector("#roadmap-list");
    container.replaceChildren();

    weeks.forEach((week) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `week-button${week.week === state.week ? " active" : ""}`;
        button.dataset.week = String(week.week);
        button.innerHTML = `<span>WEEK ${String(week.week).padStart(2, "0")} · ${week.stage}</span><strong>${week.title}</strong>`;
        button.addEventListener("click", () => {
            state.week = week.week;
            weekSelect.value = String(week.week);
            progressStorage.set("java-todo-current-week", String(state.week));
            renderPage();
            document.querySelector("#today").scrollIntoView({ behavior: "smooth" });
        });
        container.append(button);
    });
}

function renderPage() {
    const week = weeks[state.week - 1];
    const weekDetail = weekDetails[state.week - 1];
    const explanations = conceptExplanations[state.week - 1];
    const advancedItems = advancedKnowledge[state.week - 1];
    const sourceLesson = sourceLessons[state.week - 1];
    const dayPlan = dayPlans[state.day - 1];

    document.querySelector("#hero-week").textContent = `第 ${week.week} 周`;
    document.querySelector("#hero-topic").textContent = week.title;
    document.querySelector("#hero-day").textContent = `DAY ${state.day}`;
    document.querySelector("#hero-concept-count").textContent = `${weekDetail.concepts.length + advancedItems.length} 个技术点`;
    document.querySelector("#hero-day-focus").textContent = dayPlan.verb;
    document.querySelector("#stage-label").textContent = `${week.stage} · WEEK ${String(week.week).padStart(2, "0")}`;
    document.querySelector("#day-label").textContent = `${dayPlan.label} · DAY ${state.day}`;
    document.querySelector("#daily-title").textContent = `${dayPlan.verb}：${week.title}`;
    document.querySelector("#daily-focus").textContent = `${week.focus} ${dayPlan.action}`;
    document.querySelector("#daily-deliverable").textContent = week.deliverable;
    document.querySelector("#pitfall-text").textContent = weekDetail.pitfall;
    document.querySelector("#study-mode-label").textContent = dayPlan.label;

    renderObjectives(week, weekDetail, dayPlan);
    renderConcepts(weekDetail, advancedItems);
    renderKnowledge(weekDetail, explanations, advancedItems);
    renderSourceLesson(sourceLesson);
    renderSpecificStudy(buildSpecificStudy(week, weekDetail, explanations, dayPlan));
    renderOrderedList("#code-path-list", buildCodePath(week));
    renderOrderedList("#lab-step-list", buildLabSteps(week, weekDetail, dayPlan));
    document.querySelector("#expected-result").textContent = getExpectedResult(week, weekDetail, dayPlan);
    renderReviewQuestions(week, weekDetail, dayPlan);

    const fileList = document.querySelector("#file-list");
    fileList.replaceChildren(...week.files.map((file) => {
        const item = document.createElement("li");
        item.textContent = file;
        return item;
    }));

    const commandList = document.querySelector("#command-list");
    commandList.replaceChildren(...getCommands(week.week).map((command) => {
        const code = document.createElement("code");
        code.textContent = command;
        return code;
    }));

    renderRoadmap();
}

weeks.forEach((week) => {
    const option = document.createElement("option");
    option.value = String(week.week);
    option.textContent = `第 ${week.week} 周 · ${week.title}`;
    weekSelect.append(option);
});

weekSelect.value = String(state.week);
daySelect.value = String(state.day);

weekSelect.addEventListener("change", () => {
    state.week = Number(weekSelect.value);
    progressStorage.set("java-todo-current-week", String(state.week));
    renderPage();
});

daySelect.addEventListener("change", () => {
    state.day = Number(daySelect.value);
    progressStorage.set("java-todo-current-day", String(state.day));
    renderPage();
});

document.querySelector("#today-date").textContent = new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "long",
}).format(new Date());

renderPage();
