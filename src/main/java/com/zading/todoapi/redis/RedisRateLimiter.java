package com.zading.todoapi.redis;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Redis 固定窗口限流实现。
 *
 * <p>计数和设置过期时间放在同一个 Lua 脚本中，避免 INCR 成功后程序异常导致 Key 永不过期。</p>
 */
@Component
@Profile("redis")
public class RedisRateLimiter implements RateLimiter {
    private static final DefaultRedisScript<List> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            local ttl = redis.call('TTL', KEYS[1])
            return {current, ttl}
            """,
            List.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RateLimitDecision tryAcquire(String key, int limit, Duration window) {
        validate(key, limit, window);

        List<?> result = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(Math.max(1, window.toSeconds()))
        );

        if (result == null || result.size() < 2) {
            throw new IllegalStateException("Redis 限流脚本没有返回计数结果");
        }

        long currentCount = numberValue(result.get(0));
        long retryAfterSeconds = numberValue(result.get(1));
        boolean allowed = currentCount <= limit;

        return new RateLimitDecision(
                allowed,
                currentCount,
                allowed ? 0 : Math.max(1, retryAfterSeconds)
        );
    }

    private long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(String.valueOf(value));
    }

    private void validate(String key, int limit, Duration window) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("限流 Key 不能为空");
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("限流次数必须大于 0");
        }

        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("限流时间窗口必须大于 0");
        }
    }
}
