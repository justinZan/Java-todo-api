package com.zading.todoapi.redis;

/**
 * 一次限流判断的结果。
 *
 * @param allowed 是否允许本次请求
 * @param currentCount 当前时间窗口内的请求数
 * @param retryAfterSeconds 被拒绝时建议等待的秒数
 */
public record RateLimitDecision(
        boolean allowed,
        long currentCount,
        long retryAfterSeconds
) {
}
