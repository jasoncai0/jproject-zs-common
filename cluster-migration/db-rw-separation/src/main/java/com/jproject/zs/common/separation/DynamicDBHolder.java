package com.jproject.zs.common.separation;

import lombok.extern.slf4j.Slf4j;

/**
 * <p></p>
 *
 * @author : dinglei
 * @date : 2023/1/31
 **/
@Slf4j
public class DynamicDBHolder {

    private static final ThreadLocal<String> CONTEXT_DB = new ThreadLocal<>();

    /**
     * 切换数据源
     *
     * @param datasourceKey
     */
    public static void set(String datasourceKey) {
        CONTEXT_DB.set(datasourceKey);
    }

    public static String get() {
        return CONTEXT_DB.get();
    }

    public static void remove() {
        CONTEXT_DB.remove();
    }
}
