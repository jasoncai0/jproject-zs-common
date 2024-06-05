package com.jproject.zs.common.redis.migration.redisson.factory;

import com.jproject.zs.common.redis.migration.redis.config.RedisAsyncConfig;
import com.jproject.zs.common.redis.migration.redisson.spi.BucketOperations;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.redisson.RedissonBucket;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/14
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, staticName = "wrap")
public class BroadcastBucket<V> {

    private String name;
    private RedissonBucket<V> bucket;
    private BucketOperations bucketOperations;
    private RedisAsyncConfig redisAsyncConfig;


    public void delete() {
        delete(redisAsyncConfig.isBroadcastEnabled());
    }

    public void set(V value, long ttl, TimeUnit ttlUnit) {
        set(value, ttl, ttlUnit, redisAsyncConfig.isBroadcastEnabled());
    }

    public void set(V value) {
        set(value, redisAsyncConfig.isBroadcastEnabled());
    }

    public void expire(long ttl, TimeUnit timeUnit) {
        expire(ttl, timeUnit, redisAsyncConfig.isBroadcastEnabled());
    }

    public void delete(boolean broadcast) {
        bucketOperations.delete(bucket, broadcast);
    }

    public void set(V value, long ttl, TimeUnit ttlUnit, boolean broadcast) {
        bucketOperations.set(bucket, value, ttl, ttlUnit, broadcast);
    }

    public void set(V value, boolean broadcast) {
        bucketOperations.set(bucket, value, broadcast);
    }

    public void expire(long ttl, TimeUnit timeUnit, boolean broadcast) {
        bucketOperations.expire(bucket, ttl, timeUnit, broadcast);
    }

    public V get() {
        return bucket.get();
    }


}
