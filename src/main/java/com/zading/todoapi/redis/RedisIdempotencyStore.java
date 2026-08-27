package com.zading.todoapi.redis;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis 幂等 Key 实现。
 *
 * <p>Key 的值分为 PROCESSING 和 COMPLETED 两种状态。完成和释放都必须校验 owner token，
 * 防止处理超时后旧请求覆盖新请求的状态。</p>
 */
@Component
@Profile("redis")
public class RedisIdempotencyStore implements IdempotencyStore {
    private static final String PROCESSING_PREFIX = "PROCESSING:";
    private static final String COMPLETED_PREFIX = "COMPLETED:";

    private static final DefaultRedisScript<Long> COMPLETE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                redis.call('set', KEYS[1], ARGV[2], 'EX', ARGV[3])
                return 1
            end
            return 0
            """,
            Long.class
    );

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotencyStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public IdempotencyClaim tryClaim(String key, Duration ttl) {
        validate(key, ttl);

        String ownerToken = UUID.randomUUID().toString();
        Boolean claimed = redisTemplate.opsForValue().setIfAbsent(
                key,
                PROCESSING_PREFIX + ownerToken,
                ttl
        );

        if (Boolean.TRUE.equals(claimed)) {
            return IdempotencyClaim.claimed(ownerToken);
        }

        String currentValue = redisTemplate.opsForValue().get(key);

        if (currentValue != null && currentValue.startsWith(COMPLETED_PREFIX)) {
            return IdempotencyClaim.completed(currentValue.substring(COMPLETED_PREFIX.length()));
        }

        return IdempotencyClaim.processing();
    }

    @Override
    public boolean complete(String key, String ownerToken, String result, Duration ttl) {
        validate(key, ttl);
        if (ownerToken == null || ownerToken.isBlank()) {
            return false;
        }

        Long updated = redisTemplate.execute(
                COMPLETE_SCRIPT,
                List.of(key),
                PROCESSING_PREFIX + ownerToken,
                COMPLETED_PREFIX + result,
                String.valueOf(Math.max(1, ttl.toSeconds()))
        );

        return Long.valueOf(1L).equals(updated);
    }

    @Override
    public boolean release(String key, String ownerToken) {
        if (key == null || key.isBlank() || ownerToken == null || ownerToken.isBlank()) {
            return false;
        }

        Long deleted = redisTemplate.execute(
                RELEASE_SCRIPT,
                List.of(key),
                PROCESSING_PREFIX + ownerToken
        );

        return Optional.ofNullable(deleted).orElse(0L) > 0;
    }

    private void validate(String key, Duration ttl) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("幂等 Key 不能为空");
        }

        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("幂等 Key TTL 必须大于 0");
        }
    }
}
