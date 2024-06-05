package com.jproject.zs.common.redis.migration.redisson.spi;

import com.jproject.zs.common.redis.migration.redis.broadcast.RedisOperationsBroadcast;
import com.jproject.zs.common.redis.migration.redisson.serialize.KryoSerializer;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonMap;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/14
 */
@Slf4j
@RequiredArgsConstructor
public class MapOperations {

    private final RedisOperationsBroadcast redisOperationsBroadcast;

    private final KryoSerializer kryoSerializer;


    public <K, V> V remove(RedissonMap<K, V> redissonMap, K field, boolean broadcast) {
        V v = redissonMap.remove(field);
        if (broadcast) {
            redisOperationsBroadcast.hdel(
                    redissonMap.getRawName().getBytes(),
                    kryoSerializer.serialize(field)
            );
        }
        return v;
    }

    public <K, V> V put(RedissonMap<K, V> redissonMap, K field, V value, boolean broadcast) {
        V v = redissonMap.put(field, value);
        if (broadcast) {
            redisOperationsBroadcast.hset(
                    redissonMap.getRawName().getBytes(),
                    kryoSerializer.serialize(field),
                    kryoSerializer.serialize(value),
                    -1
            );
        }
        return v;
    }


    public <K, V> void expire(RedissonMap<K, V> redissonMap, long ttl, TimeUnit timeUnit, boolean broadcast) {
        redissonMap.expire(ttl, timeUnit);
        if (broadcast) {
            redisOperationsBroadcast.expire(
                    redissonMap.getRawName().getBytes(),
                    timeUnit.toSeconds(ttl)
            );
        }
    }

    public <K, V> boolean delete(RedissonMap<K, V> redissonMap, boolean broadcast) {
        boolean val = redissonMap.delete();
        if (broadcast) {
            redisOperationsBroadcast.del(redissonMap.getRawName().getBytes());
        }
        return val;
    }


}
