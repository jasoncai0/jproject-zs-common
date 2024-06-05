package com.jproject.zs.common.redis.migration.redisson.factory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/17
 */
class BroadcastSetTest extends BaseBroadcastRedissonClientTest {

    @Test
    void remove() {

        BroadcastSet<TestPOJO<Long>> local = broadcastClient.getSet("testLocalSetRemoteGet.set");
        BroadcastSet<TestPOJO<Long>> remote = broadcastClient2.getSet("testLocalSetRemoteGet.set");

        local.add(new TestPOJO<>("zhensheng1", 1, 1L), true);
        local.add(new TestPOJO<>("zhensheng2", 2, 2L), true);
        local.add(new TestPOJO<>("zhensheng3", 3, 3L), true);

        System.out.println(remote.size());
        System.out.println(remote.values());

        Assertions.assertEquals(3, remote.size());

        local.remove(new TestPOJO<>("zhensheng1", 1, 1L), true);

        System.out.println(remote.size());
        System.out.println(remote.values());

        Assertions.assertEquals(2, remote.size());

        local.remove(new TestPOJO<>("zhensheng2", 2, 2L), true);

        System.out.println(remote.size());
        System.out.println(remote.values());

        Assertions.assertEquals(1, remote.size());

        local.remove(new TestPOJO<>("zhensheng3", 2, 2L), true);

        System.out.println(remote.size());
        System.out.println(remote.values());

        Assertions.assertEquals(1, remote.size());


    }

    @Test
    void add() {
    }

    @Test
    void values() {
    }

    @Test
    void size() {
    }
}