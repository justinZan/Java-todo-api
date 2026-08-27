package com.zading.todoapi.redis;

import java.time.Duration;
import java.util.Optional;

/**
 * 分布式锁抽象。
 *
 * <p>业务代码只依赖这个接口，因此默认环境可以使用内存实现，redis profile 可以切换为 Redis 实现。</p>
 */
public interface DistributedLock {
    Optional<LockHandle> tryLock(String key, Duration leaseTime);

    void unlock(LockHandle handle);
}
