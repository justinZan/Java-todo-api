package com.zading.todoapi.redis;

import java.time.Duration;

/**
 * 幂等 Key 存储抽象。
 */
public interface IdempotencyStore {
    IdempotencyClaim tryClaim(String key, Duration ttl);

    boolean complete(String key, String ownerToken, String result, Duration ttl);

    boolean release(String key, String ownerToken);
}
