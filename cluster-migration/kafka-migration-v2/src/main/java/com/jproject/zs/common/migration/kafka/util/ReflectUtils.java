package com.jproject.zs.common.migration.kafka.util;

import java.lang.reflect.Method;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/2/27
 */
public class ReflectUtils {


    public static Method getPublicMethod(Class<?> clazz, String methodName, Class<?>... paramTypes)
            throws SecurityException {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException var4) {
            return null;
        }
    }

}
