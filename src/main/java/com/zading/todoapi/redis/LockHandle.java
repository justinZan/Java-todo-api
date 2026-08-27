package com.zading.todoapi.redis;

/**
 * 一次成功获取的锁。token 用来证明释放锁的请求就是持有锁的请求。
 */
public record LockHandle(String key, String token) {
}
