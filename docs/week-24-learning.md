# 第 24 周：Docker 基础和项目容器化

本周目标是把已经可以通过 `java -jar` 运行的 Spring Boot 应用，进一步封装为可重复构建、可配置、可持久化的 Docker 容器。

本周没有安装或启动 Docker。我们先把工程文件准备好，并使用 Maven 打包和配置检查验证项目；以后本机安装 Docker 后，可以直接执行文档中的启动命令。

## 一、本周完成了什么

项目新增或修改了以下内容：

| 文件 | 作用 |
| --- | --- |
| `Dockerfile` | 使用多阶段构建 Java 21 应用镜像 |
| `.dockerignore` | 排除 Git、target、数据库文件和本地上传文件 |
| `docker-compose.yml` | 同时编排 Java 应用和 PostgreSQL |
| `.env.example` | 提供 Compose 所需环境变量示例 |
| `README.md` | 增加构建、启动、日志和数据卷说明 |

## 二、Docker 中的几个基本概念

### 1. 镜像 Image

镜像是应用运行所需要的文件系统模板，包含：

- 基础操作系统层；
- Java 运行时；
- 应用 JAR；
- 启动命令；
- 默认环境配置。

镜像本身是不可变的。启动镜像后，Docker 才会创建容器。

### 2. 容器 Container

容器是镜像的运行实例。一个镜像可以启动多个容器，每个容器有自己的：

- 进程；
- 网络地址；
- 环境变量；
- 可写临时文件层。

容器被删除后，容器可写层通常也会消失，所以数据库和上传文件不能只放在容器可写层中。

### 3. 数据卷 Volume

Volume 是由 Docker 管理的持久化存储。本项目使用：

```yaml
volumes:
  postgres_data:
  app_uploads:
```

PostgreSQL 数据保存在 `postgres_data`，Todo 附件保存在 `app_uploads`。删除容器不会自动删除这些 volume。

### 4. Compose

Docker Compose 用一个 YAML 文件描述多个服务、网络、端口、环境变量、依赖关系和数据卷。

本项目的关系是：

```text
浏览器
   |
   | localhost:8080
   v
app 容器
   |
   | jdbc:postgresql://postgres:5432/java_todo_api
   v
postgres 容器
```

## 三、第一处改变：Dockerfile 多阶段构建

文件：`Dockerfile`

完整结构：

```dockerfile
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser \
    && mkdir -p /app/uploads \
    && chown -R appuser:appuser /app
COPY --from=build --chown=appuser:appuser \
    /workspace/target/java-todo-api-1.0.0.jar \
    /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 1. 构建阶段

```dockerfile
FROM maven:3.9.9-eclipse-temurin-21 AS build
```

这一阶段包含 Maven 和 JDK 21，负责执行：

```bash
mvn package -DskipTests
```

为什么使用 JDK 而不是 JRE？

因为编译 Java 源代码需要 `javac` 等开发工具，而运行 JAR 通常只需要 JRE。

### 2. 设置工作目录

```dockerfile
WORKDIR /workspace
```

之后的相对路径都以 `/workspace` 为基准。这样不需要在每条命令中写很长的绝对路径。

### 3. 先复制 pom.xml

```dockerfile
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
```

Docker 会按照指令生成 layer。如果只修改 Java 源代码，`pom.xml` 没有变化，那么 Maven 依赖层可以被 Docker 复用，不必每次重新下载依赖。

`-B` 表示 batch mode，适合自动化构建；`-DskipTests` 表示构建镜像时先跳过测试。

注意：跳过测试只影响镜像构建速度，不能替代 CI 或发布前的测试。正式构建前仍然应该先执行：

```bash
mvn clean test
```

### 4. 复制源码并打包

```dockerfile
COPY src ./src
RUN mvn -B -DskipTests package
```

这里才复制 `src`。这样源代码发生变化时，只会重新执行源代码之后的 Docker layer。

最终生成：

```text
target/java-todo-api-1.0.0.jar
```

### 5. 运行阶段使用 JRE

```dockerfile
FROM eclipse-temurin:21-jre
```

运行阶段不再需要 Maven 和 `javac`，只保留 Java 运行时和最终 JAR。这样通常比直接使用 Maven JDK 镜像更小，也减少了运行环境中不必要的工具。

### 6. 使用非 root 用户

```dockerfile
RUN useradd --system --uid 10001 --create-home appuser \
    && mkdir -p /app/uploads \
    && chown -R appuser:appuser /app

USER appuser
```

容器中的应用不使用 root 用户运行。即使应用存在文件写入或依赖库风险，也能减少对容器系统的影响范围。

由于附件服务需要写入 `/app/uploads`，所以创建目录后必须把目录所有者改为 `appuser`。

### 7. 复制最终 JAR

```dockerfile
COPY --from=build --chown=appuser:appuser \
    /workspace/target/java-todo-api-1.0.0.jar \
    /app/app.jar
```

`--from=build` 表示从前面的构建阶段复制文件，而不是从宿主机复制。最终运行镜像不会包含 Maven 源码和构建缓存。

### 8. JVM 内存参数

```dockerfile
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
```

容器可能拥有和宿主机不同的内存限制。`MaxRAMPercentage` 让 JVM 根据容器可用内存计算堆内存上限，避免简单使用宿主机内存估算。

### 9. 暴露端口和启动命令

```dockerfile
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

`EXPOSE` 是镜像的端口声明，不会自动把端口发布到宿主机。真正的端口映射由 Compose 完成：

```yaml
ports:
  - "${APP_PORT:-8080}:8080"
```

左边是宿主机端口，右边是容器端口。

## 四、第二处改变：`.dockerignore`

文件：`.dockerignore`

主要忽略：

```text
.git
target/
data/
uploads/
.env
docs/
README.md
```

### 为什么忽略 target

Dockerfile 会在构建阶段重新执行 Maven 打包，所以不应该把宿主机的 `target` 复制进构建上下文。这样可以避免：

- 把旧 JAR 误复制到镜像；
- 增大 Docker build context；
- 让宿主机编译环境影响容器构建。

### 为什么忽略 data 和 uploads

这些目录属于本地运行数据，不应该被打包进镜像。数据库使用 PostgreSQL volume，附件使用 `app_uploads` volume。

### 为什么忽略 `.env`

`.env` 可能包含数据库密码和 JWT 密钥，不应该进入镜像构建上下文，也不应该提交到 Git。

## 五、第三处改变：Compose 同时运行 app 和 PostgreSQL

文件：`docker-compose.yml`

### 1. app 服务使用 Dockerfile 构建

```yaml
app:
  build:
    context: .
    dockerfile: Dockerfile
  image: java-todo-api:local
```

`context: .` 表示以项目根目录作为构建上下文。Docker 会读取根目录下的 `Dockerfile`。

### 2. app 依赖数据库健康状态

```yaml
depends_on:
  postgres:
    condition: service_healthy
```

只写 `depends_on: postgres` 只能保证启动顺序，不能保证数据库已经可以接受连接。当前 PostgreSQL 已经配置了 `pg_isready` healthcheck，所以应用会等待数据库健康后再启动。

### 3. 容器之间用服务名通信

```yaml
DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-java_todo_api}
```

这里的 `postgres` 是 Compose 服务名。Compose 会为服务创建内部网络，并提供服务名解析。

在应用容器内不能写：

```text
jdbc:postgresql://localhost:5432/java_todo_api
```

因为 `localhost` 指向当前的 app 容器，而 PostgreSQL 在另一个容器中。

### 4. 环境变量注入配置

```yaml
environment:
  SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
  DB_USERNAME: ${POSTGRES_USER:-postgres}
  DB_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
  JWT_SECRET: ${JWT_SECRET:-replace-with-a-long-random-secret-at-least-32-chars}
```

Compose 使用 `.env` 文件中的值替换 `${...}`。如果变量没有提供，就使用 `:-` 后面的默认值。

学习环境可以使用默认值，但真实环境必须修改：

- PostgreSQL 密码；
- JWT_SECRET；
- 对外暴露的端口；
- 文件存储和日志策略。

### 5. 附件使用 volume

```yaml
volumes:
  - app_uploads:/app/uploads
```

应用的 `FILE_STORAGE_ROOT` 默认是 `/app/uploads`。容器内写入该目录的文件会进入 `app_uploads` volume，而不是只留在容器临时层。

## 六、启动和停止流程

### 1. 准备环境文件

```bash
cp .env.example .env
```

打开 `.env`，至少修改：

```properties
POSTGRES_PASSWORD=your-strong-password
JWT_SECRET=your-long-random-secret-at-least-32-chars
```

### 2. 检查 Compose 配置

```bash
docker compose config
```

这个命令可以提前发现 YAML 格式、变量替换和服务配置问题。

### 3. 构建并启动

```bash
docker compose up -d --build
```

参数含义：

- `up`：创建并启动服务；
- `-d`：后台运行；
- `--build`：启动前重新构建应用镜像。

### 4. 查看状态

```bash
docker compose ps
```

预期可以看到：

```text
java-todo-api          running
java-todo-api-postgres healthy
```

### 5. 查看日志

```bash
docker compose logs -f app
docker compose logs -f postgres
```

`-f` 表示持续跟随日志。退出日志查看不会停止容器。

### 6. 调用健康检查

```bash
curl http://localhost:8080/actuator/health
```

也可以访问：

```text
http://localhost:8080/swagger-ui.html
```

### 7. 停止服务

```bash
docker compose down
```

`down` 会停止并删除容器，但默认保留 `postgres_data` 和 `app_uploads`。

### 8. 删除数据

```bash
docker compose down -v
```

`-v` 会删除 Compose volume，数据库和附件数据都会丢失。学习时可以使用，真实环境必须谨慎。

## 七、没有安装 Docker 时如何学习

当前电脑没有 Docker 时，仍然可以完成：

1. 阅读 Dockerfile 的构建阶段和运行阶段；
2. 理解 `.dockerignore` 为什么排除本地文件；
3. 使用 Maven 生成可执行 JAR；
4. 使用 `docker compose config` 以外的静态方式检查 YAML 结构；
5. 理解容器服务名、端口映射、环境变量和 volume；
6. 等安装 Docker 后再执行真正的镜像构建。

可以先验证 JAR：

```bash
mvn clean package -DskipTests
```

可以验证配置文件内容：

```bash
sed -n '1,240p' Dockerfile
sed -n '1,260p' docker-compose.yml
```

不能在没有 Docker 的情况下真实验证：

- `docker build`；
- `docker compose up`；
- 容器网络；
- volume 持久化；
- 容器内 PostgreSQL 连接。

这些属于环境验证，不影响本周的代码和配置学习。

## 八、常见问题

### 1. 为什么要多阶段构建？

因为编译和运行需要的工具不同。构建阶段需要 Maven 和 JDK，运行阶段只需要 JRE。多阶段构建可以让最终镜像不包含 Maven、源码和构建缓存。

### 2. 为什么不直接把本地 target 目录复制进镜像？

这样会让镜像依赖本地构建结果，容易把旧文件或错误 profile 的 JAR 放进去。Dockerfile 内部重新执行 Maven，构建过程更可重复。

### 3. 为什么 app 使用 prod profile？

容器环境更接近部署环境：使用 PostgreSQL、关闭 H2 Console、关闭 SQL 输出，并从环境变量读取数据库密码和 JWT 密钥。

### 4. 为什么本地默认还是 H2？

因为 Docker 和 PostgreSQL 是可选工具。默认 H2 让你可以在没有 Docker 的电脑上继续编写和测试 Java 代码；Compose 则为以后接近生产的运行方式提供入口。

### 5. 为什么数据库要用 volume？

容器是可替换的。升级镜像、删除容器或重建服务都不应该导致数据库丢失，因此数据库文件必须放在独立的持久化存储中。

### 6. Docker volume 和 bind mount 有什么区别？

- volume 由 Docker 管理，适合数据库和应用持久化数据；
- bind mount 直接映射宿主机路径，适合本地开发时查看或编辑文件。

本项目先使用 named volume，避免把宿主机路径和具体操作系统绑定在一起。

### 7. `EXPOSE 8080` 会发布端口吗？

不会。它只是镜像元数据声明。真正的发布由 Compose 的 `ports` 完成：

```yaml
ports:
  - "8080:8080"
```

### 8. `docker compose down` 会删除数据库吗？

默认不会删除 named volume。只有执行 `docker compose down -v` 才会删除 Compose volume。

## 九、本周复盘题与参考答案

### 问题 1：镜像和容器有什么区别？

镜像是不可变的运行模板，容器是镜像启动后的运行实例。一个镜像可以创建多个容器。

### 问题 2：为什么 Dockerfile 先复制 pom.xml，再复制 src？

为了利用 Docker layer cache。依赖只在 `pom.xml` 变化时重新下载，普通源代码修改可以复用依赖层。

### 问题 3：为什么应用连接数据库使用 `postgres` 而不是 `localhost`？

因为 Compose 中 `postgres` 是数据库容器的服务名，容器之间通过 Compose 内部网络通信。`localhost` 只代表当前容器。

### 问题 4：为什么应用要使用非 root 用户？

减少应用进程获得过高系统权限后的影响范围，是容器安全的基础实践。

### 问题 5：为什么附件要使用 volume？

容器删除后临时写入层可能消失。volume 独立于容器生命周期，可以保存附件。

### 问题 6：为什么容器配置使用环境变量？

同一个镜像可以在开发、测试和生产中复用，只通过环境变量改变数据库地址、密码、JWT 密钥和端口等环境差异。

### 问题 7：Dockerfile 中 `-DskipTests` 是否代表不需要测试？

不是。它只表示镜像构建步骤不执行测试。发布前仍然需要先运行 `mvn clean test`，CI 也应该独立执行测试。

## 十、下一周

按照既定路线，下一周是第 25 周：PostgreSQL 深入和数据库设计。届时会学习 PostgreSQL 数据类型、约束、事务隔离、数据库设计和 H2 与 PostgreSQL 的差异。

## 代码精读补充

### 1. Dockerfile 多阶段构建

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre
COPY --from=build /workspace/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

第一阶段需要 Maven 和完整 JDK，只负责编译；第二阶段只保留 JRE 和最终 JAR。最终镜像不包含源码和 Maven 缓存，因此更小，也减少运行时内容。

### 2. Compose 中的数据库持久化

```yaml
volumes:
  postgres_data:
  app_uploads:
```

容器生命周期和数据生命周期应该分开。数据库文件放入 `postgres_data`，附件放入 `app_uploads`；删除应用容器时，业务数据和文件不会因为容器临时层消失。

### 3. 为什么镜像不写死环境差异

镜像只包含应用，数据库地址、用户名、密码和 JWT 密钥通过环境变量注入。这样同一个镜像可以在测试、预发布和生产环境复用。
