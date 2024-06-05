package com.jproject.zs.common.redis.migration.redisson.factory;

import com.jproject.zs.common.redis.migration.redis.seralize.StringBytesCodec;
import com.jproject.zs.common.redis.migration.redisson.mq.RedissonMsgHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/5/17
 */
public class RedisMsgHandlerTest extends BaseBroadcastRedissonClientTest {


    private RedissonMsgHandler redissonMsgHandler;
    private StringBytesCodec decoder = new StringBytesCodec();
    private String msg = "{\"v\":\"CgJHWhIQYXN5bmMucmVkaXMuZGF0YRgHIP///////////wEqHWJjZy5saWZlLnY0LmRlZmF1bHRf\\nMTcyOTM2NDA0MgsDATExNjc2MDU5tjpnAQABAZrin42FYgHAcAHsw4iNhWIBUFJPxAGo9q2NhWIB\\nMTAzNDm2AWJjZy5ncm91cC5ucy52MS41Ni5kZWZhdWz0AWRlZmF1bPQBSEFJTcEBMTE2NzYwNTm2\\nAdimiY2FYgGos/akAQ==\"}";

    @BeforeEach
    public void init1() {

        redisAsyncConfig.setBroadcastEnabled(true);

        redissonMsgHandler = new RedissonMsgHandler(
                redisOperationsBroadcast2To1,
                redisAsyncConfig,

                null

        );
    }

    @Test
    public void test() {

        redissonMsgHandler.asyncRedisCommand(msg);


    }


}
