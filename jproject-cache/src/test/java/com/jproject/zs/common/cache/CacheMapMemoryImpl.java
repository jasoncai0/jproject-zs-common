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
public class CacheMapMemoryImpl {

    private final Map<String, String> cache = new HashMap<>();


    @Getter
    private CacheMapRedisKeyImpl<String, String> cacheHelper;


    public CacheMapMemoryImpl(boolean cacheDummy, String dummy) {

        cacheHelper = new CacheMapRedisKeyImpl<String, String>(key -> {
            System.out.println("load key=" + key + " current cache=" + cache);

            return cache.get(key);

        }, keys -> {
            System.out.println("load keys=" + keys + " current cache=" + cache);
            return keys.stream()
                    .filter(cache::containsKey)
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
        }, keys -> {
            System.out.println("clean keys=" + keys);
            keys.forEach(key -> {
                cache.remove(key);
            });

        }, "test.key.%s", id -> id, cacheDummy, dummy);
    }
}
