package com.jproject.zs.common.redis.migration.redisson.serialize;

import com.jproject.zs.common.redis.migration.redisson.broadcast.BucketOperationsTest.TestPOJO;
import com.google.common.collect.Lists;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/14
 */
class KryoSerializerTest {


    private static KryoSerializer kryoSerializer = new KryoSerializer();

    @Test
    void serialize() {

        String str = new String(kryoSerializer.serialize(new TestPOJO("zhenzheng", 1)), StandardCharsets.UTF_8);

        System.out.println(str);

        Object v = kryoSerializer.deserialize(str.getBytes(StandardCharsets.UTF_8));

        System.out.println(v);
    }

    @Test
    void serializeList() {
    }

    @Test
    void deserialize() {
    }

    @Test
    void deserializeList() {

        ArrayList<TestPOJO> values = Lists.newArrayList(
                new TestPOJO("zhenzheng1", 1),
                new TestPOJO("zhenzheng2", 2),
                new TestPOJO("zhenzheng3", 3)
        );

        ;

        List<Object> r = kryoSerializer.deserializeList(kryoSerializer.serializeList(values));

        System.out.println(r);

    }
}