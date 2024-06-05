package com.jproject.zs.common.redis.migration.redisson.factory;

import com.jproject.zs.common.redis.migration.redis.config.RedisAsyncConfig;
import com.jproject.zs.common.redis.migration.redisson.spi.AtomicLongOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.BucketOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.LockOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.MapOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.QueueOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.SetOperations;
import lombok.RequiredArgsConstructor;
import org.redisson.RedissonAtomicLong;
import org.redisson.RedissonBucket;
import org.redisson.RedissonLock;
import org.redisson.RedissonMap;
import org.redisson.RedissonQueue;
import org.redisson.RedissonSet;
import org.redisson.api.RedissonClient;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/14
 */
@RequiredArgsConstructor
public class BroadcastRedissonClient {

    private final RedissonClient redissonClient;

    private final RedisAsyncConfig redisAsyncConfig;

    private final MapOperations mapOperations;

    private final BucketOperations bucketOperations;

    private final QueueOperations queueOperations;

    private final SetOperations setOperations;

    private final AtomicLongOperations atomicLongOperations;

    private final LockOperations lockOperations;


    public <K, V> BroadcastMap<K, V> getMap(String name) {

        return BroadcastMap.wrap(name, ((RedissonMap<K, V>) redissonClient.getMap(name)), mapOperations,
                redisAsyncConfig);

    }


    public <V> BroadcastBucket<V> getBucket(String name) {

        return BroadcastBucket.wrap(name, ((RedissonBucket<V>) redissonClient.getBucket(name)), bucketOperations,
                redisAsyncConfig);

    }


    public <V> BroadcastQueue<V> getQueue(String name) {

        return BroadcastQueue.wrap(name, ((RedissonQueue<V>) redissonClient.getQueue(name)), queueOperations,
                redisAsyncConfig);

    }


    public <V> BroadcastSet<V> getSet(String name) {

        return BroadcastSet.wrap(name, ((RedissonSet<V>) redissonClient.getSet(name)), setOperations, redisAsyncConfig);
    }


    public BroadcastAtomicLong getAtomicLong(String name) {

        return BroadcastAtomicLong.wrap(name,
                (RedissonAtomicLong) redissonClient.getAtomicLong(name),
                atomicLongOperations, redisAsyncConfig);
    }


    public BroadcastLock getLock(String name) {
        return BroadcastLock.wrap(name, (RedissonLock) redissonClient.getLock(name), lockOperations, redisAsyncConfig);
    }

}
