package com.jproject.zs.common.redis.migration.redisson.factory;

import com.jproject.zs.common.redis.migration.redis.config.RedisAsyncConfig;
import com.jproject.zs.common.redis.migration.redisson.spi.LockOperations;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.redisson.RedissonLock;

/**
 * 该类的实现为可重入锁，目前的实现上看，value存储了节点和线程信息，无法直接unlock等效为delete，所以暂时不稳定，先禁用
 *
 * @author caizhensheng
 * @desc
 * @date 2023/4/17
 */
@Deprecated
@AllArgsConstructor(access = AccessLevel.PACKAGE, staticName = "wrap")
public class BroadcastLock {


    private String name;
    private RedissonLock lock;
    private LockOperations lockOperations;

    private RedisAsyncConfig redisAsyncConfig;


    public boolean tryLock(long ttl, TimeUnit ttlUnit) {
        return tryLock(ttl, ttlUnit, redisAsyncConfig.isBroadcastEnabled());
    }

    public void unlock() {
        unlock(redisAsyncConfig.isBroadcastEnabled());
    }


    public boolean tryLock(long ttl, TimeUnit ttlUnit, boolean broadcast) {
        return lockOperations.tryLock(lock, ttl, ttlUnit, broadcast);
    }

    public void unlock(boolean broadcast) {
        lockOperations.unlock(lock, broadcast);
    }

}
