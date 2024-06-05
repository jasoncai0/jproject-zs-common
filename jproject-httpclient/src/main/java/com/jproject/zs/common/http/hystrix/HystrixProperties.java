package com.jproject.zs.common.http.hystrix;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/3/4
 */
@Data
@Accessors(chain = true)
public class HystrixProperties {

    private boolean enabled = true;

    private List<String> standByHosts;

    /**
     * 至少volumn请求失败，熔断器才发挥起作用
     * {@link com.netflix.hystrix.HystrixCommandProperties#default_circuitBreakerRequestVolumeThreshold}
     */
    private int volumn = 20;
    /**
     * 根据错误率，超过下面的errorRate的阈值时，触发熔断，熔断后该时间内所有请求将走熔断逻辑
     * {@link com.netflix.hystrix.HystrixCommandProperties#default_circuitBreakerSleepWindowInMilliseconds}
     */
    private int sleep = 5000;

    /**
     * 单位请求数内，出错率，一旦超过该比率，将会触发熔断
     * {@link com.netflix.hystrix.HystrixCommandProperties#default_circuitBreakerErrorThresholdPercentage}
     */
    private int errorRate = 50;

    /*
     * {@link com.netflix.hystrix.HystrixCommandProperties#default_fallbackIsolationSemaphoreMaxConcurrentRequests}
     */
    private int fallbackMaxConcurrency = 10;

    /**
     * {@link com.netflix.hystrix.HystrixThreadPoolProperties#default_coreSize}
     */
    private int threads = 10;

    /**
     * {@link com.netflix.hystrix.HystrixThreadPoolProperties#default_maximumSize}
     */
    private Integer maxThreads = null;


//    /**
//     * 半开状态，经过sleep时间的熔断状态后进入该状态，该状态下跟正常无熔断时一样，所有请求放行，
//     * 但是会计算halfOpen个请求的错误率，一旦低于errorRate，便可恢复，否则继续再次进入熔断状态，以此类推。
//     */
//    private int halfOpen = 10;

}
