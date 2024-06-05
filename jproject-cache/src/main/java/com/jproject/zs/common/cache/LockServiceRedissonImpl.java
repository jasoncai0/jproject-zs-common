package com.jproject.zs.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/5/20
 */
@Slf4j
@RequiredArgsConstructor
public class LockServiceRedissonImpl extends AbstractLockServiceImpl {

    private final RedissonClient redissonClient;

    private final String redisLockKey;

    private final Map<String, RLock> lockMap = new ConcurrentHashMap<>();


    @Override
    public boolean lock(String key, long timeout, long leaseTime, TimeUnit unit) throws InterruptedException {

        RLock lock = lockMap.computeIfAbsent(String.format(redisLockKey, key),
                redissonClient::getLock);
        if (leaseTime == -1L) {
            return lock.tryLock(timeout, unit);
        } else {
            return lock.tryLock(timeout, leaseTime, unit);
        }
    }

    @Override
    public void unlock(String key) {
        RLock lock = lockMap.get(String.format(redisLockKey, key));
        if (lock == null) {
            log.warn("Unexpected empty lock of key={}", key);
            return;
        }
        lock.unlock();
    }
}
