package com.jproject.zs.common.redis.migration.redisson.broadcast;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.jproject.zs.common.common.util.JsonUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.RedissonBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/13
 */
public class BucketOperationsTest {


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

        RedissonBucket<TestPOJO> bucket = (RedissonBucket<TestPOJO>) client.<TestPOJO>getBucket("common.util.test.1");

        bucket.set(new TestPOJO("zhensheng", 18));

        System.out.println(bucket.get());
    }


    @Test
    public void testBucket2() throws IOException, ClassNotFoundException {

        RedissonBucket<TestPOJO> bucket = (RedissonBucket<TestPOJO>) client.<TestPOJO>getBucket("common.util.test.1");

        TestPOJO value = new TestPOJO("zhensheng", 18);

        String clazz = value.getClass().getName();

//        byte[] bytes = ByteBufUtil.getBytes(bucket.getCodec().getValueEncoder().encode(value));

//        bucket.set(value);

        String json = JsonUtils.toJson(value);

        Object decodeValue = JsonUtils.fromJson(json, Class.forName(clazz));

        System.out.println(decodeValue);

        RedissonBucket<Object> bytesBucket = (RedissonBucket<Object>) client.<Object>getBucket("common.util.test.1");

        bytesBucket.set(decodeValue);

        System.out.println(value);

        System.out.println(bucket.get());
    }


    @Test
    public void testBucket3() throws IOException, ClassNotFoundException {

        RedissonBucket<TestPOJO> bucket = (RedissonBucket<TestPOJO>) client.<TestPOJO>getBucket("common.util.test.2");

        TestPOJO value = new TestPOJO("zhensheng", 18);

        Kryo kryo = new Kryo();

        byte[] bytes;
        try (Output ou = new Output(new ByteArrayOutputStream())) {
            kryo.writeClassAndObject(ou, value);
            bytes = ou.toBytes();

            System.out.println(bytes);
        }

//        String clazz = value.getClass().getName();
//
//        byte[] bytes = ByteBufUtil.getBytes(bucket.getCodec().getValueEncoder().encode(value));
//

//        String json = JsonUtils.toJson(value);
//
//
//        System.out.println(decodeValue);

        Object decodeValue = null;
        try (Input input = new Input(bytes)) {

            decodeValue = kryo.readClassAndObject(input);

        }

        RedissonBucket<Object> bytesBucket = (RedissonBucket<Object>) client.<Object>getBucket("common.util.test.2");

        bytesBucket.set(decodeValue);

        System.out.println(value);

        System.out.println(bucket.get());
    }

    @Test
    public void testRemoteService() {

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPOJO implements Serializable {

        private String name;
        private int age;


    }

}