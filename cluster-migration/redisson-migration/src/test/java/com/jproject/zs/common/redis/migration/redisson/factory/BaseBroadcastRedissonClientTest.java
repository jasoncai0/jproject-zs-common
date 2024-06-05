package com.jproject.zs.common.redis.migration.redisson.factory;

import com.jproject.zs.common.redis.migration.redis.config.RedisAsyncConfig;
import com.jproject.zs.common.redis.migration.redisson.serialize.KryoSerializer;
import com.jproject.zs.common.redis.migration.redisson.spi.AtomicLongOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.BucketOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.LockOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.MapOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.QueueOperations;
import com.jproject.zs.common.redis.migration.redisson.spi.RedisOperationRedissonCallback;
import com.jproject.zs.common.redis.migration.redisson.spi.SetOperations;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.BeforeAll;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/17
 */
class BaseBroadcastRedissonClientTest {

    static RedissonClient client;
    static RedissonClient client2;

    static BroadcastRedissonClient broadcastClient;
    static BroadcastRedissonClient broadcastClient2;

    static RedisAsyncConfig redisAsyncConfig = new RedisAsyncConfig();

    static RedisOperationRedissonCallback redisOperationRedissonCallback1To2;

    static RedisOperationsBroadcastMockImpl redisOperationsBroadcast1To2 = new RedisOperationsBroadcastMockImpl();


    static RedisOperationRedissonCallback redisOperationRedissonCallback2To1;

    static RedisOperationsBroadcastMockImpl redisOperationsBroadcast2To1 = new RedisOperationsBroadcastMockImpl();

    static KryoSerializer kryoSerializer = new KryoSerializer();

    private static MapOperations mapOperations = new MapOperations(redisOperationsBroadcast1To2, kryoSerializer);

    private static BucketOperations bucketOperations = new BucketOperations(redisOperationsBroadcast1To2,
            kryoSerializer);

    private static QueueOperations queueOperations = new QueueOperations(redisOperationsBroadcast1To2, kryoSerializer);

    private static SetOperations setOperations = new SetOperations(redisOperationsBroadcast1To2, kryoSerializer);

    private static AtomicLongOperations atomicLongOperations = new AtomicLongOperations(redisOperationsBroadcast1To2,
            kryoSerializer);

    private static LockOperations lockOperations = new LockOperations(redisOperationsBroadcast1To2, kryoSerializer);

    private static MapOperations mapOperations2 = new MapOperations(redisOperationsBroadcast2To1, kryoSerializer);

    private static BucketOperations bucketOperations2 = new BucketOperations(redisOperationsBroadcast2To1,
            kryoSerializer);

    private static QueueOperations queueOperations2 = new QueueOperations(redisOperationsBroadcast2To1, kryoSerializer);

    private static SetOperations setOperations2 = new SetOperations(redisOperationsBroadcast2To1, kryoSerializer);

    private static AtomicLongOperations atomicLongOperations2 = new AtomicLongOperations(redisOperationsBroadcast2To1,
            kryoSerializer);

    private static LockOperations lockOperations2 = new LockOperations(redisOperationsBroadcast2To1, kryoSerializer);


    /**
     * broadcastClient1                        broadcastClient2 |                                   ^ |
     * | V -----------broadcastMock1To2--------->|
     */
    @BeforeAll
    public static void init() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://172.16.38.181:6379")
                .setConnectTimeout(2000)
                .setPassword("123456")
                .setConnectionMinimumIdleSize(5)
                .setTimeout(2000);
//        config.setSingleServerConfig(single);

        redisAsyncConfig.setBroadcastAsync(true);

        client = Redisson.create(config);

        Config config2 = new Config();
        config2.useSingleServer()
                .setAddress("redis://172.16.38.181:6379")
                .setConnectTimeout(2000)
                .setPassword("123456")
                .setDatabase(1)
                .setConnectionMinimumIdleSize(5)
                .setTimeout(2000);
//        config.setSingleServerConfig(single);

        client2 = Redisson.create(config2);

        redisOperationRedissonCallback1To2 = new RedisOperationRedissonCallback()
                .setRedissonClient(client2)
                .setKryoSerializer(kryoSerializer)
                .setBucketOperations(bucketOperations)
                .setQueueOperations(queueOperations)
                .setSetOperations(setOperations)
                .setMapOperations(mapOperations)
                .setAtomicLongOperations(atomicLongOperations)
                .setLockOperations(lockOperations);

        redisOperationsBroadcast1To2.setRedisOperationRedissonCallback(redisOperationRedissonCallback1To2);

        redisOperationRedissonCallback2To1 = new RedisOperationRedissonCallback()
                .setRedissonClient(client)
                .setKryoSerializer(kryoSerializer)
                .setBucketOperations(bucketOperations2)
                .setQueueOperations(queueOperations2)
                .setSetOperations(setOperations2)
                .setMapOperations(mapOperations2)
                .setAtomicLongOperations(atomicLongOperations2)
                .setLockOperations(lockOperations2);

        redisOperationsBroadcast2To1.setRedisOperationRedissonCallback(redisOperationRedissonCallback2To1);

        broadcastClient = new BroadcastRedissonClient(client,
                redisAsyncConfig,
                mapOperations,
                bucketOperations,
                queueOperations,
                setOperations,
                atomicLongOperations,
                lockOperations
        );

        broadcastClient2 = new BroadcastRedissonClient(client2,
                redisAsyncConfig,
                mapOperations2,
                bucketOperations2,
                queueOperations2,
                setOperations2,
                atomicLongOperations2,
                lockOperations2
        );


    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class TestPOJO<V> implements Serializable {

        private String name;

        private Integer age;

        private V value;

    }

}