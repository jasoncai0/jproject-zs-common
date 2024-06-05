package com.jproject.zs.common.redis.migration.redisson.spi;

import com.jproject.zs.common.redis.migration.redis.broadcast.RedisOperationsBroadcast;
import com.jproject.zs.common.redis.migration.redisson.serialize.KryoSerializer;
import lombok.RequiredArgsConstructor;
import org.redisson.RedissonSet;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/14
 */
@RequiredArgsConstructor
public class SetOperations {

    private final RedisOperationsBroadcast redisOperationsBroadcast;

    private final KryoSerializer kryoSerializer;


    public <V> boolean remove(RedissonSet<V> redissonSet, V value, boolean broadcast) {
        boolean r = redissonSet.remove(value);

        if (broadcast) {
            redisOperationsBroadcast.srem(
                    redissonSet.getRawName().getBytes(),
                    new byte[][]{kryoSerializer.serialize(value)}
            );
        }
        return r;
    }

    public <V> boolean add(RedissonSet<V> redissonSet, V value, boolean broadcast) {
        boolean r = redissonSet.add(value);

        if (broadcast) {
            redisOperationsBroadcast.sadd(
                    redissonSet.getRawName().getBytes(),
                    new byte[][]{kryoSerializer.serialize(value)}, -1
            );
        }
        return r;
    }

}
