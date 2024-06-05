package com.jproject.zs.common.redis.migration.redisson.broadcast;

import com.jproject.zs.common.redis.migration.redisson.broadcast.BucketOperationsTest.TestPOJO;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/14
 */
public class ExpireAndDelOperationsTest {


    static RedissonClient client;




    @BeforeAll
    public static void init() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://172.16.38.181:6379")
                .setConnectTimeout(2000)
                .setPassword("123456")
                .setConnectionMinimumIdleSize(5)
                .setTimeout(2000);
//        config.setSingleServerConfig(single);

        client = Redisson.create(config);


    }

    @Test
    public void testBucket() {

        RBucket<TestPOJO> bucket = client.getBucket("common.util.test.1");

        bucket.set(new TestPOJO("zhensheng", 18));

        RMap<String, TestPOJO> map = client.getMap("common.util.test.1");

        map.delete();

        System.out.println(bucket.get());
    }


    @Test
    public void testBucket2() throws InterruptedException {

        RBucket<TestPOJO> bucket = client.getBucket("common.util.test.1");

        bucket.set(new TestPOJO("zhensheng", 18));
        bucket.expire(10, TimeUnit.MINUTES);
//        System.out.println(bucket.getExpireTime());

        RMap<String, TestPOJO> map = client.getMap("common.util.test.1");

//        map.delete();

        map.expire(10, TimeUnit.SECONDS);

        System.out.println(bucket.get());

        map.expire(1, TimeUnit.SECONDS);
        System.out.println(bucket.get());

        Thread.sleep(1000);

        System.out.println(bucket.get());

//        System.out.println(bucket.getExpireTime());

    }

    // 测试atomiclong 和bucket的set是否互通
    @Test
    public void testBucket3() throws InterruptedException {

        RBucket<Long> bucket = client.getBucket("common.util.test.long.1");

        bucket.set(100L);

        RAtomicLong atomicLong = client.getAtomicLong("common.util.test.long.1");

        System.out.println(atomicLong.get());

        atomicLong.set(200L);

        System.out.println(bucket.get());

    }
}
