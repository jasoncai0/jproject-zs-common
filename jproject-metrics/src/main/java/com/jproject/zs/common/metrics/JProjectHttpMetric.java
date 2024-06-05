package com.jproject.zs.common.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/10/21
 */

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JProjectHttpMetric {


    JProjectHttpMetricConvertor convertor() default JProjectHttpMetricConvertor.DEFAULT_CONVERTORS;

    /**
     * 是否记录时长，用于绘制pt99
     *
     * @return
     */
    boolean durationMetric() default false;


}