package com.zading.todoapi.redis;

/**
 * 幂等 Key 的占用结果。
 */
public record IdempotencyClaim(
        Status status,
        String ownerToken,
        String result
) {
    public enum Status {
        /** 当前请求首次获得处理权。 */
        CLAIMED,
        /** 另一个请求正在处理同一个幂等 Key。 */
        PROCESSING,
        /** 之前的请求已经处理完成，result 保存业务结果标识。 */
        COMPLETED
    }

    public static IdempotencyClaim claimed(String ownerToken) {
        return new IdempotencyClaim(Status.CLAIMED, ownerToken, null);
    }

    public static IdempotencyClaim processing() {
        return new IdempotencyClaim(Status.PROCESSING, null, null);
    }

    public static IdempotencyClaim completed(String result) {
        return new IdempotencyClaim(Status.COMPLETED, null, result);
    }
}
