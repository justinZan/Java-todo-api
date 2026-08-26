# syntax=docker/dockerfile:1

# 构建阶段：在 Maven + JDK 21 环境中编译 Spring Boot 可执行 JAR。
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# 先复制 pom.xml，利用 Docker layer cache 缓存 Maven 依赖。
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# 运行阶段：只保留 JRE 和最终 JAR，减小运行镜像体积。
FROM eclipse-temurin:21-jre

WORKDIR /app

# 应用不使用 root 用户运行；uploads 目录需要允许应用用户写入。
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
