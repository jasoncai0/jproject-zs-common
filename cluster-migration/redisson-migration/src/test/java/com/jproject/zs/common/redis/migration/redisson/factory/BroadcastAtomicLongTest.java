package com.jproject.zs.common.redis.migration.redisson.factory;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/17
 */
class BroadcastAtomicLongTest extends BaseBroadcastRedissonClientTest {

    @Test
    void set() throws InterruptedException {

        BroadcastAtomicLong local = broadcastClient.getAtomicLong("testLocalSetRemoteGet.along");
        BroadcastAtomicLong remote = broadcastClient2.getAtomicLong("testLocalSetRemoteGet.along");

        local.delete(true);

        local.set(1L, true);

        System.out.println(remote.get());

        Assertions.assertEquals(1L, remote.get());

        local.incrementAndGet(true);

        System.out.println(remote.get());

        Assertions.assertEquals(2L, remote.get());

        local.addAndGet(100, true);

        System.out.println(remote.get());

        Assertions.assertEquals(102L, remote.get());


    }

    @Test
    void expire() throws InterruptedException {

        BroadcastAtomicLong local = broadcastClient.getAtomicLong("testLocalSetRemoteGet.along.1");
        BroadcastAtomicLong remote = broadcastClient2.getAtomicLong("testLocalSetRemoteGet.along.1");

        local.delete(true);

        local.set(1L, true);

        local.expire(1, TimeUnit.MILLISECONDS, true);

        Thread.sleep(2);

        System.out.println(remote.get());

        Assertions.assertEquals(0L, remote.get());

    }

    @Test
    void delete() {

        BroadcastAtomicLong local = broadcastClient.getAtomicLong("testLocalSetRemoteGet.along.2");
        BroadcastAtomicLong remote = broadcastClient2.getAtomicLong("testLocalSetRemoteGet.along.2");

        local.delete(true);

        local.set(1L, true);

        System.out.println(remote.get());

        Assertions.assertEquals(1L, remote.get());

        local.delete(true);

        Assertions.assertEquals(0L, remote.get());


    }

    @Test
    void incrementAndGet() {
    }

    @Test
    void addAndGet() {
    }

    @Test
    void get() {
    }
}