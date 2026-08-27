package com.zading.todoapi.redis;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 没有 Redis 时使用的单 JVM 锁。
 *
 * <p>它只用于本地学习和测试，多个应用实例之间不会共享这张 Map，不能替代真正的 Redis 分布式锁。</p>
 */
@Component
@Profile("!redis")
public class InMemoryDistributedLock implements DistributedLock {
    private final ConcurrentHashMap<String, LockEntry> locks = new ConcurrentHashMap<>();

    @Override
    public Optional<LockHandle> tryLock(String key, Duration leaseTime) {
        validate(key, leaseTime);

        long now = System.currentTimeMillis();
        LockHandle handle = new LockHandle(key, UUID.randomUUID().toString());
        LockEntry newEntry = new LockEntry(handle.token(), now + leaseTime.toMillis());
        AtomicReference<LockHandle> acquired = new AtomicReference<>();

        locks.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAtMillis() <= now) {
                acquired.set(handle);
                return newEntry;
            }

            return current;
        });

        return Optional.ofNullable(acquired.get());
    }

    @Override
    public void unlock(LockHandle handle) {
        if (handle == null) {
            return;
        }

        locks.computeIfPresent(handle.key(), (key, current) ->
                current.token().equals(handle.token()) ? null : current
        );
    }

    private void validate(String key, Duration leaseTime) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("锁 Key 不能为空");
        }

        if (leaseTime == null || leaseTime.isZero() || leaseTime.isNegative()) {
            throw new IllegalArgumentException("锁租期必须大于 0");
        }
    }

    private record LockEntry(String token, long expiresAtMillis) {
    }
}
