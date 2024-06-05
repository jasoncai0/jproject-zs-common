package com.jproject.zs.common.redis.migration.redisson.factory;

import com.jproject.zs.common.redis.migration.redis.config.RedisAsyncConfig;
import com.jproject.zs.common.redis.migration.redisson.spi.AtomicLongOperations;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.redisson.RedissonAtomicLong;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/14
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, staticName = "wrap")
public class BroadcastAtomicLong {

    private String name;

    private RedissonAtomicLong atomicLong;

    private AtomicLongOperations atomicLongOperations;

    private RedisAsyncConfig redisAsyncConfig;

    public void set(long value) {
        set(value, redisAsyncConfig.isBroadcastEnabled());
    }

    public void expire(long ttl, TimeUnit ttlUnit) {
        expire(ttl, ttlUnit, redisAsyncConfig.isBroadcastEnabled());
    }

    public long incrementAndGet() {
        return incrementAndGet(redisAsyncConfig.isBroadcastEnabled());
    }

    public long addAndGet(long delta) {
        return addAndGet(delta, redisAsyncConfig.isBroadcastEnabled());
    }

    public void set(long value, boolean broadcast) {
        atomicLongOperations.set(atomicLong, value, broadcast);
    }

    public void expire(long ttl, TimeUnit ttlUnit, boolean broadcast) {
        atomicLongOperations.expire(atomicLong, ttl, ttlUnit, broadcast);
    }

    public long incrementAndGet(boolean broadcast) {
        return atomicLongOperations.incrementAndGet(atomicLong, broadcast);
    }

    public long addAndGet(long delta, boolean broadcast) {
        return atomicLongOperations.addAndGet(atomicLong, delta, broadcast);
    }

    public long get() {
        return atomicLong.get();
    }

    public boolean delete(boolean broadcast) {
        return atomicLongOperations.delete(atomicLong, broadcast);
    }

}
