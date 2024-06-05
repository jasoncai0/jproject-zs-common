package com.jproject.zs.common.redis.migration.redisson.spi;

import com.jproject.zs.common.redis.migration.redis.broadcast.RedisOperationsBroadcast;
import com.jproject.zs.common.redis.migration.redisson.serialize.KryoSerializer;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.RedissonAtomicLong;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/14
 */
@RequiredArgsConstructor
public class AtomicLongOperations {

    private final RedisOperationsBroadcast redisOptBroadcast;

    private final KryoSerializer kryoSerializer;


    public void set(RedissonAtomicLong atomicLong, long value, boolean broadcast) {
        atomicLong.set(value);

        if (broadcast) {
            redisOptBroadcast.numberSet(
                    atomicLong.getRawName().getBytes(),
                    kryoSerializer.serialize(value),
                    -1L
            );
        }
    }

    public void expire(RedissonAtomicLong atomicLong, long ttl, TimeUnit ttlUnit, boolean broadcast) {
        atomicLong.expire(ttl, ttlUnit);

        if (broadcast) {
            redisOptBroadcast.expire(atomicLong.getRawName().getBytes(), ttlUnit.toSeconds(ttl));
        }
    }

    public long incrementAndGet(RedissonAtomicLong atomicLong, boolean broadcast) {
        long value = atomicLong.incrementAndGet();

        if (broadcast) {
            redisOptBroadcast.incr(atomicLong.getRawName().getBytes(), 1L);
        }

        return value;
    }

    public long addAndGet(RedissonAtomicLong atomicLong, long delta, boolean broadcast) {
        long value = atomicLong.addAndGet(delta);

        if (broadcast) {
            redisOptBroadcast.incr(atomicLong.getRawName().getBytes(), delta);
        }

        return value;
    }

    public boolean delete(RedissonAtomicLong atomicLong, boolean broadcast) {
        boolean val = atomicLong.delete();

        if (broadcast) {
            redisOptBroadcast.del(atomicLong.getRawName().getBytes());
        }
        return val;
    }


}
