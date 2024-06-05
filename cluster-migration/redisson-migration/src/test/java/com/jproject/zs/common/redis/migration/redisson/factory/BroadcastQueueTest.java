package com.jproject.zs.common.redis.migration.redisson.factory;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/17
 */
class BroadcastQueueTest extends BaseBroadcastRedissonClientTest {

    @Test
    void clearAndOfferAll() {

        BroadcastQueue<TestPOJO<Long>> local = broadcastClient.getQueue("testLocalSetRemoteGet.queue");
        BroadcastQueue<TestPOJO<Long>> remote = broadcastClient2.getQueue("testLocalSetRemoteGet.queue");

        local.clearAndOfferAll(
                Lists.newArrayList(
                        new TestPOJO<>("zhensheng1", 1, 1L),
                        new TestPOJO<>("zhensheng2", 2, 2L),
                        new TestPOJO<>("zhensheng3", 3, 3L)),
                30, TimeUnit.MINUTES,
                true);

        List<TestPOJO<Long>> values = new ArrayList<>();
        TestPOJO<Long> val = null;
        do {
            val = remote.poll(true);
            System.out.println(val);

            if (val != null) {
                values.add(val);
            }
            // 1 , 2 ,3
        } while (val != null);

//        System.out.println(local.poll(true));

        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(1L, (long) values.get(0).getValue());
        Assertions.assertEquals(2L, (long) values.get(1).getValue());
        Assertions.assertEquals(3L, (long) values.get(2).getValue());

        Assertions.assertNull(local.poll(true));

    }

    @Test
    void offer() {

        BroadcastQueue<TestPOJO<Long>> local = broadcastClient.getQueue("testLocalSetRemoteGet.queue.offer");
        BroadcastQueue<TestPOJO<Long>> remote = broadcastClient2.getQueue("testLocalSetRemoteGet.queue.offer");

        local.clearAndOfferAll(
                new ArrayList<>(),
                30, TimeUnit.MINUTES,
                true);

        local.offer(new TestPOJO<>("zhensheng1", 1, 1L), true);
        local.offer(new TestPOJO<>("zhensheng2", 2, 2L), true);
        local.offer(new TestPOJO<>("zhensheng3", 3, 3L), true);

        List<TestPOJO<Long>> values = new ArrayList<>();
        TestPOJO<Long> val = null;
        do {
            val = remote.poll(true);
            System.out.println(val);

            if (val != null) {
                values.add(val);
            }
            // 1 , 2 ,3
        } while (val != null);

//        System.out.println(local.poll(true));

        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(1L, (long) values.get(0).getValue());
        Assertions.assertEquals(2L, (long) values.get(1).getValue());
        Assertions.assertEquals(3L, (long) values.get(2).getValue());

        Assertions.assertNull(local.poll(true));

    }


    @Test
    public void testClear() {
        BroadcastQueue<TestPOJO<Long>> local = broadcastClient.getQueue("testLocalSetRemoteGet.queue.clear");
        BroadcastQueue<TestPOJO<Long>> remote = broadcastClient2.getQueue("testLocalSetRemoteGet.queue.clear");

        local.clear(true);

        local.offer(new TestPOJO<>("zhensheng1", 1, 1L), true);
        local.offer(new TestPOJO<>("zhensheng2", 2, 2L), true);
        local.offer(new TestPOJO<>("zhensheng3", 3, 3L), true);

        List<TestPOJO<Long>> values = new ArrayList<>();
        TestPOJO<Long> val = null;
        do {
            val = remote.poll(true);
            System.out.println(val);

            if (val != null) {
                values.add(val);
            }
            // 1 , 2 ,3
        } while (val != null);

//        System.out.println(local.poll(true));

        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(1L, (long) values.get(0).getValue());
        Assertions.assertEquals(2L, (long) values.get(1).getValue());
        Assertions.assertEquals(3L, (long) values.get(2).getValue());

        Assertions.assertNull(local.poll(true));


    }

    @Test
    void poll() {
    }
}