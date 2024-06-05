package com.jproject.zs.common.redis.migration.redisson.spi;

import com.jproject.zs.common.redis.migration.redis.broadcast.RedisOperationsBroadcast;
import com.jproject.zs.common.redis.migration.redisson.serialize.KryoSerializer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.RedissonQueue;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/13
 */
@RequiredArgsConstructor
public class QueueOperations {

    private final RedisOperationsBroadcast redisOperationsBroadcast;

    private final KryoSerializer kryoSerializer;

    public <V> void clearAndOfferAll(
            RedissonQueue<V> queue, List<V> values,
            long ttl, TimeUnit ttlUnit,
            boolean broadcast) {

        queue.clear();
        values.forEach(queue::offer);

        if (ttl > 0) {
            queue.expire(ttl, ttlUnit);
        }

        if (broadcast) {
            redisOperationsBroadcast.rpushAll(
                    queue.getRawName().getBytes(),

                    kryoSerializer.serializeList(values),
                    ttlUnit.toSeconds(ttl));
        }
    }

    public <V> void clear(RedissonQueue<V> queue, boolean broadcast) {
        queue.clear();

        if (broadcast) {
            redisOperationsBroadcast.del(queue.getRawName().getBytes());
        }
    }

    public <V> void offer(RedissonQueue<V> queue, V value, boolean broadcast) {
        queue.offer(value);

        if (broadcast) {
            redisOperationsBroadcast.lpush(
                    queue.getRawName().getBytes(),
                    new byte[][]{kryoSerializer.serialize(value)}
            );
        }
    }

    public <V> V poll(RedissonQueue<V> queue, boolean broadcast) {
        V value = queue.poll();

        if (broadcast) {
            redisOperationsBroadcast.rpop(
                    queue.getRawName().getBytes()
            );
        }
        return value;
    }


}
