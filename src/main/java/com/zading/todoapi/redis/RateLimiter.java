package com.zading.todoapi.redis;

import java.time.Duration;

/**
 * 请求限流抽象。
 */
public interface RateLimiter {
    RateLimitDecision tryAcquire(String key, int limit, Duration window);
}
