package com.jproject.zs.common.redis.migration.redisson.factory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/17
 */
class BroadcastBucketTest extends BaseBroadcastRedissonClientTest {


    @Test
    public void testLocalSetRemoteGet() {

        BroadcastBucket<String> local = broadcastClient.<String>getBucket("testLocalSetRemoteGet");

        local.set("val", true);

        BroadcastBucket<Object> remote = broadcastClient2.getBucket("testLocalSetRemoteGet");

        System.out.println(remote.get());


    }

    @Test
    public void testLocalSetRemoteGet2() {

        BroadcastBucket<TestPOJO<Long>> local = broadcastClient.<TestPOJO<Long>>getBucket("testLocalSetRemoteGet");

        local.set(new TestPOJO<>("zhensheng1", 1, 1L), true);

        BroadcastBucket<TestPOJO<Long>> remote = broadcastClient2.getBucket("testLocalSetRemoteGet");

        System.out.println(remote.get());

        Assertions.assertTrue(remote.get().getValue() == 1L);

        remote.set(new TestPOJO<Long>("zhensheng2", 2, 2L), true);

        System.out.println(local.get());

        Assertions.assertTrue(remote.get().getValue() == 2L);

        local.delete(true);

        System.out.println(remote.get());

        Assertions.assertTrue(remote.get() == null);


    }

}