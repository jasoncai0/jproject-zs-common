package com.jproject.zs.common.ratelimit;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface JRateLimiter {
    // QPS,每秒允许往令牌桶添加令牌的数据。
    double permitsPerSecond();

    // 获取令牌最大的等待时间
    long timeout() default 10L;

    // 时间单位：默认：毫秒
    TimeUnit timeunit() default TimeUnit.MILLISECONDS;
}
