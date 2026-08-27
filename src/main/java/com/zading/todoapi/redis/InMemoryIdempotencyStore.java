package com.zading.todoapi.redis;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

/**
 * 没有 Redis 时使用的单 JVM 幂等 Key 存储。
 */
@Component
@Profile("!redis")
public class InMemoryIdempotencyStore implements IdempotencyStore {
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public IdempotencyClaim tryClaim(String key, Duration ttl) {
        validate(key, ttl);

        long now = System.currentTimeMillis();
        String ownerToken = UUID.randomUUID().toString();
        AtomicReference<IdempotencyClaim> claim = new AtomicReference<>();

        entries.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAtMillis() <= now) {
                claim.set(IdempotencyClaim.claimed(ownerToken));
                return Entry.processing(ownerToken, now + ttl.toMillis());
            }

            if (current.status() == EntryStatus.COMPLETED) {
                claim.set(IdempotencyClaim.completed(current.result()));
            } else {
                claim.set(IdempotencyClaim.processing());
            }

            return current;
        });

        return claim.get();
    }

    @Override
    public boolean complete(String key, String ownerToken, String result, Duration ttl) {
        validate(key, ttl);
        AtomicBoolean completed = new AtomicBoolean(false);
        long expiresAtMillis = System.currentTimeMillis() + ttl.toMillis();

        entries.computeIfPresent(key, (ignored, current) -> {
            if (current.status() == EntryStatus.PROCESSING
                    && current.ownerToken().equals(ownerToken)) {
                completed.set(true);
                return Entry.completed(result, expiresAtMillis);
            }

            return current;
        });

        return completed.get();
    }

    @Override
    public boolean release(String key, String ownerToken) {
        AtomicBoolean released = new AtomicBoolean(false);

        entries.computeIfPresent(key, (ignored, current) -> {
            if (current.status() == EntryStatus.PROCESSING
                    && current.ownerToken().equals(ownerToken)) {
                released.set(true);
                return null;
            }

            return current;
        });

        return released.get();
    }

    private void validate(String key, Duration ttl) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("幂等 Key 不能为空");
        }

        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("幂等 Key TTL 必须大于 0");
        }
    }

    private enum EntryStatus {
        PROCESSING,
        COMPLETED
    }

    private record Entry(
            EntryStatus status,
            String ownerToken,
            String result,
            long expiresAtMillis
    ) {
        private static Entry processing(String ownerToken, long expiresAtMillis) {
            return new Entry(EntryStatus.PROCESSING, ownerToken, null, expiresAtMillis);
        }

        private static Entry completed(String result, long expiresAtMillis) {
            return new Entry(EntryStatus.COMPLETED, null, result, expiresAtMillis);
        }
    }
}
