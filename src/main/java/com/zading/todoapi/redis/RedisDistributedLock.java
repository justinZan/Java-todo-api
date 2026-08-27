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
 * Redis 分布式锁实现。
 *
 * <p>SET NX EX 保证只有一个实例可以成功创建锁；Lua 释放脚本会先校验 token，
 * 防止持有旧锁的实例误删其他实例新获得的锁。</p>
 */
@Component
@Profile("redis")
public class RedisDistributedLock implements DistributedLock {
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisDistributedLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<LockHandle> tryLock(String key, Duration leaseTime) {
        validate(key, leaseTime);

        LockHandle handle = new LockHandle(key, UUID.randomUUID().toString());
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, handle.token(), leaseTime);

        return Boolean.TRUE.equals(acquired) ? Optional.of(handle) : Optional.empty();
    }

    @Override
    public void unlock(LockHandle handle) {
        if (handle == null) {
            return;
        }

        redisTemplate.execute(UNLOCK_SCRIPT, List.of(handle.key()), handle.token());
    }

    private void validate(String key, Duration leaseTime) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("锁 Key 不能为空");
        }

        if (leaseTime == null || leaseTime.isZero() || leaseTime.isNegative()) {
            throw new IllegalArgumentException("锁租期必须大于 0");
        }
    }
}
