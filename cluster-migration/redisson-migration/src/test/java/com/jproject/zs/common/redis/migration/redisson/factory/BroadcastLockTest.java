package com.jproject.zs.common.redis.migration.redisson.factory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/17
 */
class BroadcastLockTest extends BaseBroadcastRedissonClientTest {


    @Test
    void tryLock() throws ExecutionException, InterruptedException {
        BroadcastLock local = broadcastClient.getLock("broadcast.test.lock");
        BroadcastLock remote = broadcastClient2.getLock("broadcast.test.lock");

        local.unlock(true);
        remote.unlock(true);

        System.out.println(local.tryLock(30, TimeUnit.MINUTES, true));

        System.out.println(CompletableFuture.supplyAsync(() -> {
            return remote.tryLock(30, TimeUnit.MINUTES, true);
        }).get());


    }

    @Test
    void unlock() {
    }
}