package com.jproject.zs.common.redis.migration.redisson.spi;

import com.jproject.zs.common.redis.migration.redis.spi.RedisOperations;
import com.jproject.zs.common.redis.migration.redisson.serialize.KryoSerializer;
import io.vavr.Tuple2;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
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
 * @date 2023/4/12
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Accessors(chain = true)
public class RedisOperationRedissonCallback implements RedisOperations {

    private RedissonClient redissonClient;
    private KryoSerializer kryoSerializer;
    private BucketOperations bucketOperations;
    private QueueOperations queueOperations;
    private SetOperations setOperations;
    private MapOperations mapOperations;

    private AtomicLongOperations atomicLongOperations;

    private LockOperations lockOperations;


    @Override
    public void del(byte[] keyBytes) {
        bucketOperations.delete(bucket(keyBytes), false);
    }

    @Override
    public void set(byte[] keyBytes, byte[] valueBytes, long expireSeconds) {
        if (expireSeconds > 0) {
            bucketOperations.set(
                    bucket(keyBytes),
                    kryoSerializer.deserialize(valueBytes),
                    expireSeconds, TimeUnit.SECONDS, false
            );

        } else {
            bucketOperations.set(
                    bucket(keyBytes),
                    kryoSerializer.deserialize(valueBytes),
                    false
            );
        }
    }


    @Override
    public void mset(Map<byte[], byte[]> dataMap) {
        dataMap.forEach((k, v) -> {
            set(k, v, -1);
        });
    }


    @Override
    public void setNx(byte[] keyBytes, byte[] valueBytes, long expireSeconds) {
        lockOperations.tryLock(lock(keyBytes), expireSeconds, TimeUnit.SECONDS, false);
    }

    @Override
    public void incr(byte[] keyBytes, long delta) {
        atomicLongOperations.addAndGet(atomicLong(keyBytes), delta, false);
    }

    @Override
    public void sadd(byte[] keyBytes, byte[][] data, long expireSeconds) {

        Arrays.stream(data).forEach(bytes -> {
            setOperations.add(set(keyBytes), kryoSerializer.deserialize(bytes), false);
        });

        if (expireSeconds > 0) {
            expire(keyBytes, expireSeconds);
        }
    }

    @Override
    public void srem(byte[] keyBytes, byte[][] data) {
        Arrays.stream(data).forEach(bytes -> {
            setOperations.remove(set(keyBytes), kryoSerializer.deserialize(bytes), false);
        });
    }

    @Override
    public void hset(byte[] keyBytes, byte[] hashKeyBytes, byte[] dataBytes, long expireSeconds) {
        mapOperations.put(map(keyBytes),
                kryoSerializer.deserialize(hashKeyBytes),
                kryoSerializer.deserialize(dataBytes),
                false
        );

        if (expireSeconds > 0) {
            expire(keyBytes, expireSeconds);
        }
    }

    @Override
    public void hdel(byte[] keyBytes, byte[] hashKeyBytes) {
        mapOperations.remove(map(keyBytes),
                kryoSerializer.deserialize(hashKeyBytes),
                false
        );
    }

    @Override
    public void hmset(byte[] keyBytes, Map<byte[], byte[]> dataMap, long expireSeconds) {
        // 当前没有hash的批量设置的需求
    }

    @Override
    public void zadd(byte[] keyBytes, List<Tuple2<byte[], Double>> values, long expireSeconds) {
        // 当前没有zset的需求
    }

    @Override
    public void hincr(byte[] keyBytes, byte[] hashKeyBytes, long delta) {
        // 当前没有hash的incr需求

    }


    @Override
    public void rpush(byte[] keyBytes, byte[] data) {
        // 当前没有必要实现，只有队列是lpush rpop；

        // TODO 后续有栈的需求的时候再看
    }

    @Override
    public void rpushAll(byte[] keyBytes, byte[][] data, long expireSeconds) {

        queueOperations.clearAndOfferAll(
                queue(keyBytes),
                kryoSerializer.deserializeList(data),
                expireSeconds, TimeUnit.SECONDS,
                false
        );
    }

    @Override
    public void lpush(byte[] keyBytes, byte[][] data) {

        RedissonQueue<Object> queue = queue(keyBytes);
        kryoSerializer.deserializeList(data)
                .forEach(v -> queueOperations.offer(queue, v, false));

    }

    @Override
    public void rpop(byte[] keyBytes) {

        queueOperations.poll(
                queue(keyBytes),
                false
        );

    }

    @Override
    public void lpop(byte[] keyBytes) {
        //  当前没有必要实现，只有队列是lpush rpop；
    }

    @Override
    public void lpushTrim(byte[] keyBytes, byte[] data, int limit) {
        // 当前没有这个定制的需求
    }


    /**
     * 这里可以不关心key结构的大前提是测试下来，用何种结构体都能有效expire的测试结果
     */
    @Override
    public void expire(byte[] keyBytes, long expireSeconds) {
        bucketOperations.expire(bucket(keyBytes), expireSeconds, TimeUnit.SECONDS, false);
    }

    @Override
    public void mexpire(List<byte[]> keys, long expireSeconds) {
        // TODO 优化成批量的
        keys.forEach(key -> {
            expire(key, expireSeconds);
        });
    }

    @Override
    public void numberSet(byte[] keyBytes, byte[] valueBytes, long expireSeconds) {

        atomicLongOperations.set(
                atomicLong(keyBytes),
                (long) kryoSerializer.deserialize(valueBytes),
                false
        );
    }


    private RedissonBucket<Object> bucket(byte[] keyBytes) {
        return (RedissonBucket<Object>) redissonClient.getBucket(new String(keyBytes, StandardCharsets.UTF_8));
    }

    private RedissonQueue<Object> queue(byte[] keyBytes) {
        return (RedissonQueue<Object>) redissonClient.getQueue(new String(keyBytes, StandardCharsets.UTF_8));
    }

    private RedissonSet<Object> set(byte[] keyBytes) {
        return (RedissonSet<Object>) redissonClient.getSet(new String(keyBytes, StandardCharsets.UTF_8));
    }

    private RedissonMap<Object, Object> map(byte[] keyBytes) {
        return (RedissonMap<Object, Object>) redissonClient.getMap(new String(keyBytes, StandardCharsets.UTF_8));
    }

    private RedissonLock lock(byte[] keyBytes) {
        return (RedissonLock) redissonClient.getLock(new String(keyBytes, StandardCharsets.UTF_8));
    }

    private RedissonAtomicLong atomicLong(byte[] keyBytes) {
        return (RedissonAtomicLong) redissonClient.getAtomicLong(new String(keyBytes, StandardCharsets.UTF_8));
    }
}
