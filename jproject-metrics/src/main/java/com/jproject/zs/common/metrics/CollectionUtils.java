package com.jproject.zs.common.metrics;

import java.util.Collection;
import java.util.Map;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/11/3
 */
public class CollectionUtils {

    public static boolean isEmpty(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return (map == null || map.isEmpty());
    }
}
