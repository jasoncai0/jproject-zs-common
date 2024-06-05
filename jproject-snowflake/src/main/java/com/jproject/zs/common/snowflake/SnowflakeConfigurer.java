package com.jproject.zs.common.snowflake;

import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/12/23
 */
@Slf4j
public class SnowflakeConfigurer {

    /**
     * with serviceName, e.g. 'worker.id'
     */
    @Value("${snowflake.worker-id.zset-key}")
    private String workerIdScoreSetCacheKey;
    /**
     * exclusive, workerId max count
     */
    @Value("${snowflake.worker-id-bits:8}")
    private Integer maxWorkerIdBits;
    /**
     * exclusive, workerId max count
     */
    @Value("${snowflake.datacenter-id-bits:2}")
    private Integer datacenterIdBits;

    @Value("${snowflake.datacenter-id:0}")
    private Integer dataCenterId;

    @Value("${snowflake.worker-id.guard-interval-millis:1000}")
    private Long workerIdGuardIntervalMillis;
    /**
     * 受不了糟糕的封装，先本地实现一遍
     */

    @Resource
    private RedisOperations redisTemplate;

    @Bean
    public RedisWorkIdDistributor redisWorkIdDistributor() {
        return new RedisWorkIdDistributor(redisTemplate,
                workerIdScoreSetCacheKey,
                1 << maxWorkerIdBits,
                workerIdGuardIntervalMillis);
    }

    /**
     * 系统直接生成的id, generator， 实时上为了并发，在一个系统里面也可以生成多个generator， 用户可以使用同样的方式独立构造；
     *
     * @return
     */
    @Bean
    public IdGenerator idGenerator(RedisWorkIdDistributor redisWorkIdDistributor) {
        Integer workerId = redisWorkIdDistributor.workerId();
        IdGenerator generator = new IdGenerator(workerId, dataCenterId, maxWorkerIdBits, datacenterIdBits);
        log.info("Success to initialize the id generator with key={}, workerId={}", workerIdScoreSetCacheKey, workerId);
        return generator;
    }


}
