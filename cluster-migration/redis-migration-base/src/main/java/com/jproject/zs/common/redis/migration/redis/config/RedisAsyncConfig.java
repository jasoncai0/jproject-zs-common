package com.jproject.zs.common.redis.migration.redis.config;

import lombok.Data;

/**
 * <p></p>
 *
 * @author : dinglei
 * @date : 2023/2/17
 **/
@Data
public class RedisAsyncConfig {

    private String zone;

    private boolean broadcastEnabled = false;

    private boolean broadcastAsync = true;

    /**
     * 如果十分关心顺序性可以设置成1
     */
    private int broadcastAsyncThreadNum = 10;


}
