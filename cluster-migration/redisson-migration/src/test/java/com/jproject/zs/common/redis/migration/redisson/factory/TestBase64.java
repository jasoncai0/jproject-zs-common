package com.jproject.zs.common.redis.migration.redisson.factory;

import com.jproject.zs.common.redis.migration.redisson.factory.BaseBroadcastRedissonClientTest.TestPOJO;
import com.jproject.zs.common.redis.migration.redisson.serialize.KryoSerializer;
import java.io.IOException;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/5/17
 */
public class TestBase64 {


    private static BASE64Encoder encoder = new BASE64Encoder();
    private static BASE64Decoder decoder = new BASE64Decoder();
    private static Base64.Encoder encoder2 = Base64.getEncoder();
    private static Base64.Decoder decoder2 = Base64.getDecoder();
    TestPOJO test = new TestPOJO<Long>(
            "zhengsheasdfadgadfadfang2zhengsheasdfadgadfadfang2zhengsheasdfadgadfadfang2zhengsheasdfadgadfadfang2zhengsheasdfa"
                    + "dgadfadfang2zhengsheasdfadgadfadfang2", 2, 2L);

    @Test
    public void test() {
        KryoSerializer kryo = new KryoSerializer();
        String encode = encoder.encode(kryo.serialize(test));

        String encode2 = encoder2.encodeToString(kryo.serialize(test));

        System.out.println(encode);
        System.out.println(encode2);

        System.out.println("---");

        try {
            System.out.println(
                    kryo.deserialize(decoder.decodeBuffer(encode)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(
                kryo.deserialize(decoder2.decode(encode2)));

    }
}
