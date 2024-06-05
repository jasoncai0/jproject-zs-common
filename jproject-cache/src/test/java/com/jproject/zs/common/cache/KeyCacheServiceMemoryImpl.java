package com.jproject.zs.common.cache;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/11/15
 */
public class KeyCacheServiceMemoryImpl {

    private final Map<String, String> cache = new HashMap<>();


    @Getter
    private BaseKeyCacheService<String, String> cacheHelper;


    public KeyCacheServiceMemoryImpl(boolean cacheDummy, String dummy) {

        cacheHelper = new BaseKeyCacheService<>(key -> {
            System.out.println("load key=" + key + " current cache=" + cache);

            return cache.get(key);

        }, keys -> {
            System.out.println("load keys=" + keys + " current cache=" + cache);
            return keys.stream().filter(cache::containsKey)
                    .collect(Collectors.toMap(key -> key, cache::get, (o1, o2) -> o2));
        }, (key, val) -> {
            System.out.println("set cache key=" + key + ", value=" + val);
            cache.put(key, val);
        }, kvMap -> {
            System.out.println("set cache map=" + kvMap);
            cache.putAll(kvMap);
        }, key -> {
            System.out.println("clean key=" + key);
            cache.remove(key);
        }, "test.key.%s", id -> id, key -> key, cacheDummy, dummy);
    }

    private Map<String, String> cache() {
        return cache;
    }
}
