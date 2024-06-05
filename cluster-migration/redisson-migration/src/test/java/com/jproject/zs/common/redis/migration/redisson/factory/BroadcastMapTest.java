package com.jproject.zs.common.redis.migration.redisson.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/17
 */
class BroadcastMapTest extends BaseBroadcastRedissonClientTest {

    @Test
    void put() throws InterruptedException {

        BroadcastMap<String, TestPOJO<Long>> local = broadcastClient.<String, TestPOJO<Long>>getMap(
                "broadcast.test.map");

        BroadcastMap<String, TestPOJO<Long>> remote = broadcastClient2.<String, TestPOJO<Long>>getMap(
                "broadcast.test.map");

        local.put("1", new TestPOJO<Long>("zhengsheng1", 1, 1L), true);

        System.out.println(remote.get("1"));
        System.out.println(remote.get("2"));

        assertEquals(1L, (long) remote.get("1").getValue());
        assertNull(remote.get("2"));

        local.put("2", new TestPOJO<Long>("zhengsheng2", 2, 2L), true);

        System.out.println("--------");

        System.out.println(remote.get("1"));
        System.out.println(remote.get("2"));

        assertEquals(1L, (long) remote.get("1").getValue());
        assertEquals(2L, (long) remote.get("2").getValue());

        local.expire(1, TimeUnit.MICROSECONDS, true);

        Thread.sleep(2);

        System.out.println("--------");

        System.out.println(remote.get("1"));
        System.out.println(remote.get("2"));

        assertNull(remote.get("1"));
        assertNull(remote.get("2"));

        local.put("3", new TestPOJO<Long>("zhengsheng3", 3, 3L), true);

        local.put("4", new TestPOJO<Long>("zhengsheng4", 4, 4L), true);

        System.out.println("-------");

        System.out.println(remote.values());

        assertEquals(2, remote.size());

        System.out.println(remote.size());

        System.out.println(remote.containsKey("1"));
        System.out.println(remote.containsKey("2"));
        System.out.println(remote.containsKey("3"));
        System.out.println(remote.containsKey("4"));

        Assertions.assertFalse(remote.containsKey("1"));
        Assertions.assertFalse(remote.containsKey("2"));
        Assertions.assertTrue(remote.containsKey("3"));
        Assertions.assertTrue(remote.containsKey("4"));


    }

    @Test
    void expire() {
    }

    @Test
    void get() {
    }

    @Test
    void values() {
    }

    @Test
    void containsKey() {
    }

    @Test
    void size() {
    }
}