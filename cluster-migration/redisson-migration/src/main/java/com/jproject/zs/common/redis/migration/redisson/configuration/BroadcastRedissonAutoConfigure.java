package com.jproject.zs.common.redis.migration.redisson.configuration;

import com.jproject.zs.common.redis.migration.redis.broadcast.RedisOperationsBroadcast;
import com.jproject.zs.common.redis.migration.redis.config.RedisAsyncConfig;
import com.jproject.zs.common.redis.migration.redisson.factory.BroadcastRedissonClient;
import com.jproject.zs.common.redis.migration.redisson.mq.RedissonMsgHandler;
import com.jproject.zs.common.redis.migration.redisson.serialize.KryoSerializer;
import com.jproject.zs.common.redis.migration.redisson.spi.AtomicLongOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.BucketOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.LockOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.MapOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.QueueOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.RedisOperationRedissonCallback;
import com.jproject.zs.common.redis.migration.redisson.spi.SetOperations;
import org.redisson.api.RedissonClient;
import devmodule.component.databus.sub.DatabusSub;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/17
 */
public class BroadcastRedissonAutoConfigure {

    public BroadcastRedissonClient broadcastRedissonClient(
            RedissonClient redissonClient,
            RedisAsyncConfig redisAsyncConfig,
            RedisOperationsBroadcast redisOperationsBroadcast) {

        KryoSerializer kryo = kryoSerializer();
        MapOperations mapOperations = mapOperations(redisOperationsBroadcast, kryo);
        BucketOperations bucketOperations = bucketOperations(redisOperationsBroadcast, kryo);
        QueueOperations queueOperations = queueOperations(redisOperationsBroadcast, kryo);
        SetOperations setOperations = setOperations(redisOperationsBroadcast, kryo);
        AtomicLongOperations atomicLongOperations = atomicLongOperations(redisOperationsBroadcast, kryo);
        LockOperations lockOperations = lockOperations(redisOperationsBroadcast, kryo);

        return new BroadcastRedissonClient(redissonClient,
                redisAsyncConfig,
                mapOperations, bucketOperations, queueOperations,
                setOperations, atomicLongOperations, lockOperations);
    }

    public RedissonMsgHandler redissonMsgHandler(
            RedisAsyncConfig redisAsyncConfig,
            DatabusSub redisAsyncConsumer,
            RedissonClient redissonClient,
            RedisOperationsBroadcast redisOperationsBroadcast) {

        KryoSerializer kryoSerializer = kryoSerializer();

        RedisOperationRedissonCallback remoteMsgCallback = new RedisOperationRedissonCallback()
                .setRedissonClient(redissonClient)
                .setKryoSerializer(kryoSerializer)
                .setMapOperations(mapOperations(redisOperationsBroadcast, kryoSerializer))
                .setBucketOperations(bucketOperations(redisOperationsBroadcast, kryoSerializer))
                .setQueueOperations(queueOperations(redisOperationsBroadcast, kryoSerializer))
                .setSetOperations(setOperations(redisOperationsBroadcast, kryoSerializer))
                .setAtomicLongOperations(atomicLongOperations(redisOperationsBroadcast, kryoSerializer))
                .setLockOperations(lockOperations(redisOperationsBroadcast, kryoSerializer));
        return new RedissonMsgHandler(remoteMsgCallback, redisAsyncConfig, redisAsyncConsumer);
    }

    public KryoSerializer kryoSerializer() {
        return new KryoSerializer();
    }

    public MapOperations mapOperations(
            RedisOperationsBroadcast redisOperationsBroadcast,
            KryoSerializer kryoSerializer) {
        return new MapOperations(redisOperationsBroadcast, kryoSerializer);
    }

    public BucketOperations bucketOperations(
            RedisOperationsBroadcast redisOperationsBroadcast,
            KryoSerializer kryoSerializer) {
        return new BucketOperations(redisOperationsBroadcast, kryoSerializer);
    }


    public QueueOperations queueOperations(
            RedisOperationsBroadcast redisOperationsBroadcast,
            KryoSerializer kryoSerializer) {
        return new QueueOperations(redisOperationsBroadcast, kryoSerializer);
    }


    public SetOperations setOperations(
            RedisOperationsBroadcast redisOperationsBroadcast,
            KryoSerializer kryoSerializer) {
        return new SetOperations(redisOperationsBroadcast, kryoSerializer);
    }


    public AtomicLongOperations atomicLongOperations(
            RedisOperationsBroadcast redisOperationsBroadcast,
            KryoSerializer kryoSerializer) {
        return new AtomicLongOperations(redisOperationsBroadcast, kryoSerializer);
    }


    public LockOperations lockOperations(
            RedisOperationsBroadcast redisOperationsBroadcast,
            KryoSerializer kryoSerializer) {
        return new LockOperations(redisOperationsBroadcast, kryoSerializer);
    }


}
