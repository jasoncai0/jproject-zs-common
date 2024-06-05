package com.jproject.zs.common.redis.migration.redisson.spi;

import com.jproject.zs.common.redis.migration.redis.broadcast.RedisOperationsBroadcast;
import com.jproject.zs.common.redis.migration.redisson.serialize.KryoSerializer;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.RedissonLock;

/**
 * 注意在云游戏lock感觉没必要广播；
 *
 * @author caizhensheng
 * @desc
 * @date 2023/4/17
 */
@RequiredArgsConstructor
public class LockOperations {

    private final RedisOperationsBroadcast redisOptBroadcast;

    private final KryoSerializer kryoSerializer;


    public boolean tryLock(RedissonLock lock, long ttl, TimeUnit ttlUnit, boolean broadcast) {
        boolean val = lock.tryLock();
        if (broadcast && val) {
            redisOptBroadcast.setNx(
                    lock.getRawName().getBytes(),
                    kryoSerializer.serialize(true),
                    ttlUnit.toSeconds(ttl));
        }

        return val;
    }

    public void unlock(RedissonLock lock, boolean broadcast) {
        lock.unlock();
        if (broadcast) {
            redisOptBroadcast.del(lock.getRawName().getBytes());
        }
    }


}
