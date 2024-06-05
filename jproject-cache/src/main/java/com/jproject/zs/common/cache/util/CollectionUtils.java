package com.jproject.zs.common.cache.util;

import java.util.Collection;
import java.util.Map;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/5/7
 */
public class CollectionUtils {

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
}
