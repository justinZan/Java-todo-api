package com.zading.todoapi.redis;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 没有 Redis 时使用的单 JVM 固定窗口限流器。
 */
@Component
@Profile("!redis")
public class InMemoryRateLimiter implements RateLimiter {
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    @Override
    public RateLimitDecision tryAcquire(String key, int limit, Duration window) {
        validate(key, limit, window);

        long now = System.currentTimeMillis();
        long expiresAt = now + window.toMillis();
        AtomicReference<Counter> selected = new AtomicReference<>();

        counters.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAtMillis() <= now) {
                Counter fresh = new Counter(expiresAt, 1);
                selected.set(fresh);
                return fresh;
            }

            Counter next = new Counter(current.expiresAtMillis(), current.count() + 1);
            selected.set(next);
            return next;
        });

        Counter counter = selected.get();
        boolean allowed = counter.count() <= limit;
        long retryAfterSeconds = allowed ? 0 : secondsUntil(counter.expiresAtMillis(), now);
        return new RateLimitDecision(allowed, counter.count(), retryAfterSeconds);
    }

    private long secondsUntil(long expiresAtMillis, long now) {
        long remainingMillis = Math.max(0, expiresAtMillis - now);
        return Math.max(1, (remainingMillis + 999) / 1000);
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

    private record Counter(long expiresAtMillis, long count) {
    }
}
