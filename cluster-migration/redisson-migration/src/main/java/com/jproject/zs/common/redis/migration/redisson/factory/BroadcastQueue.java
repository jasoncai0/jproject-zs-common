package com.jproject.zs.common.redis.migration.redisson.factory;

import com.jproject.zs.common.redis.migration.redis.config.RedisAsyncConfig;
import com.jproject.zs.common.redis.migration.redisson.spi.QueueOperations;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.redisson.RedissonQueue;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/14
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, staticName = "wrap")
public class BroadcastQueue<V> {

    private String name;

    private RedissonQueue<V> queue;

    private QueueOperations queueOperations;

    private RedisAsyncConfig redisAsyncConfig;


    public void clearAndOfferAll(
            List<V> values,
            long ttl, TimeUnit ttlUnit) {
        clearAndOfferAll(values, ttl, ttlUnit, redisAsyncConfig.isBroadcastEnabled());
    }


    public void clear() {
        clear(redisAsyncConfig.isBroadcastEnabled());
    }


    public void offer(V value) {
        offer(value, redisAsyncConfig.isBroadcastEnabled());
    }

    public V poll() {
        return poll(redisAsyncConfig.isBroadcastEnabled());
    }


    public void clearAndOfferAll(
            List<V> values,
            long ttl, TimeUnit ttlUnit,
            boolean broadcast) {
        queueOperations.clearAndOfferAll(queue, values, ttl, ttlUnit, broadcast);
    }


    public void clear(boolean broadcast) {
        queueOperations.clear(queue, broadcast);
    }


    public void offer(V value, boolean broadcast) {
        queueOperations.offer(queue, value, broadcast);
    }

    public V poll(boolean broadcast) {
        return queueOperations.poll(queue, broadcast);
    }


}
