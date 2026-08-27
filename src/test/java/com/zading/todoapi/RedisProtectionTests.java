package com.zading.todoapi;

import com.zading.todoapi.redis.IdempotencyClaim;
import com.zading.todoapi.redis.InMemoryDistributedLock;
import com.zading.todoapi.redis.InMemoryIdempotencyStore;
import com.zading.todoapi.redis.InMemoryRateLimiter;
import com.zading.todoapi.redis.LockHandle;
import com.zading.todoapi.redis.RateLimitDecision;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisProtectionTests {
    @Test
    void shouldAllowOnlyOneInMemoryLockOwnerAndRequireOwnerTokenToUnlock() {
        InMemoryDistributedLock lock = new InMemoryDistributedLock();

        Optional<LockHandle> first = lock.tryLock("lock:test", Duration.ofMinutes(1));
        Optional<LockHandle> second = lock.tryLock("lock:test", Duration.ofMinutes(1));

        assertTrue(first.isPresent());
        assertTrue(second.isEmpty());

        lock.unlock(new LockHandle("lock:test", "another-owner"));
        assertTrue(lock.tryLock("lock:test", Duration.ofMinutes(1)).isEmpty());

        lock.unlock(first.get());
        assertTrue(lock.tryLock("lock:test", Duration.ofMinutes(1)).isPresent());
    }

    @Test
    void shouldRejectRequestsAfterRateLimitIsReached() {
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter();

        RateLimitDecision first = rateLimiter.tryAcquire("rate:test", 2, Duration.ofMinutes(1));
        RateLimitDecision second = rateLimiter.tryAcquire("rate:test", 2, Duration.ofMinutes(1));
        RateLimitDecision third = rateLimiter.tryAcquire("rate:test", 2, Duration.ofMinutes(1));

        assertTrue(first.allowed());
        assertTrue(second.allowed());
        assertFalse(third.allowed());
        assertEquals(3L, third.currentCount());
        assertTrue(third.retryAfterSeconds() >= 1);
    }

    @Test
    void shouldKeepCompletedIdempotencyResultUntilTtlExpires() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        Duration ttl = Duration.ofMinutes(1);

        IdempotencyClaim first = store.tryClaim("idempotency:test", ttl);
        IdempotencyClaim processing = store.tryClaim("idempotency:test", ttl);
        boolean completed = store.complete("idempotency:test", first.ownerToken(), "todo-10", ttl);
        IdempotencyClaim replay = store.tryClaim("idempotency:test", ttl);

        assertEquals(IdempotencyClaim.Status.CLAIMED, first.status());
        assertNotNull(first.ownerToken());
        assertEquals(IdempotencyClaim.Status.PROCESSING, processing.status());
        assertTrue(completed);
        assertEquals(IdempotencyClaim.Status.COMPLETED, replay.status());
        assertEquals("todo-10", replay.result());
    }
}
