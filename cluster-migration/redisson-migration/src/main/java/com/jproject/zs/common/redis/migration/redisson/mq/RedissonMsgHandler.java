package com.jproject.zs.common.redis.migration.redisson.mq;

import com.jproject.zs.common.redis.migration.redis.config.RedisAsyncConfig;
import com.jproject.zs.common.redis.migration.redis.spi.BaseRedisMsgHandler;
import com.jproject.zs.common.redis.migration.redis.spi.RedisOperations;
import devmodule.component.databus.sub.DatabusSub;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/17
 */
public class RedissonMsgHandler extends BaseRedisMsgHandler {

    public RedissonMsgHandler(
            RedisOperations redisOperations,
            RedisAsyncConfig redisAsyncConfig,
            DatabusSub redisAsyncConsumer) {
        super(redisOperations, redisAsyncConfig, redisAsyncConsumer);
    }
}
