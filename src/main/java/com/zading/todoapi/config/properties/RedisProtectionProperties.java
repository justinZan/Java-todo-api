package com.zading.todoapi.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 第 26 周 Redis 能力的统一配置。
 *
 * <p>默认 profile 使用内存实现，redis profile 使用 Redis 实现；两者共享同一套业务参数，
 * 这样没有安装 Redis 时也可以运行和测试。</p>
 */
@Validated
@ConfigurationProperties(prefix = "app.redis")
public record RedisProtectionProperties(
        @NotNull Duration lockLease,
        @NotNull Duration idempotencyTtl,
        @NotNull Duration rateLimitWindow,
        @Min(1) int loginLimit,
        @Min(1) int todoCreateLimit
) {
}
