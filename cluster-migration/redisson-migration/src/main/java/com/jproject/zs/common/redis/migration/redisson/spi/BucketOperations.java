package com.jproject.zs.common.redis.migration.redisson.spi;

import com.jproject.zs.common.redis.migration.redis.broadcast.RedisOperationsBroadcast;
import com.jproject.zs.common.redis.migration.redisson.serialize.KryoSerializer;
import io.vavr.control.Try;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonBucket;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/12
 */
@Slf4j
@RequiredArgsConstructor
public class BucketOperations {


    private final RedisOperationsBroadcast redisOptBroadcast;

    private final KryoSerializer kryoSerializer;


    public <V> void delete(RedissonBucket<V> bucket, boolean broadcast) {
        bucket.delete();
        if (broadcast) {
            Try.run(() -> {
                redisOptBroadcast.del(bucket.getRawName().getBytes(StandardCharsets.UTF_8));
            }).onFailure(t -> {
                log.error("Failed to broadcast delete operation", t);
            });
        }
    }


    public <V> void set(RedissonBucket<V> bucket, V value, long ttl, TimeUnit ttlUnit, boolean broadcast) {
        bucket.set(value, ttl, ttlUnit);
        if (broadcast) {
            Try.run(() -> {
                redisOptBroadcast.set(
                        bucket.getRawName().getBytes(StandardCharsets.UTF_8),
                        kryoSerializer.serialize(value),
                        ttlUnit.toSeconds(ttl));
            }).onFailure(t -> {
                log.error("Failed to broadcast set operation", t);
            });
        }
    }

    public <V> void set(RedissonBucket<V> bucket, V value, boolean broadcast) {
        bucket.set(value);
        if (broadcast) {
            Try.run(() -> {
                redisOptBroadcast.set(
                        bucket.getRawName().getBytes(StandardCharsets.UTF_8),
                        kryoSerializer.serialize(value), -1);
            }).onFailure(t -> {
                log.error("Failed to broadcast set operation", t);
            });
        }
    }

    public <V> void expire(RedissonBucket<V> bucket, long ttl, TimeUnit timeUnit, boolean broadcast) {
        bucket.expire(ttl, timeUnit);
        if (broadcast) {
            redisOptBroadcast.expire(bucket.getRawName().getBytes(StandardCharsets.UTF_8),
                    timeUnit.toSeconds(ttl));
        }
    }

}
