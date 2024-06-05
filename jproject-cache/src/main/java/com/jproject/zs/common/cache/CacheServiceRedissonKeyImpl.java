package com.jproject.zs.common.cache;

import org.redisson.api.RedissonClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/5/7
 */
public class CacheServiceRedissonKeyImpl<T, ID> extends BaseKeyCacheService<T, ID> {

    private final RedissonClient redissonClient;

    private final Long cacheMillis;

    private final boolean cacheDummy;

    public CacheServiceRedissonKeyImpl(RedissonClient redissonClient, String singleKeyTemplate, Function<T, ID> idGetter,
                                       Function<ID, String> cacheKeyGetter, T dummy, long cacheMillis) {
        this(redissonClient, singleKeyTemplate, idGetter, cacheKeyGetter, true, dummy, cacheMillis);
    }

    public CacheServiceRedissonKeyImpl(RedissonClient redissonClient, String singleKeyTemplate, Function<T, ID> idGetter,
                                       Function<ID, String> cacheKeyGetter, boolean cacheDummy, T dummy, long cacheMillis) {
        super((key) -> redissonClient.<T>getBucket(key).get(),
                (keys) -> {
                    Map<String, T> result = new HashMap<>();
                    keys.forEach(key -> {
                        result.put(key, redissonClient.<T>getBucket(key).get());
                    });
                    return result;
                },
                (key, value) -> {
                    redissonClient.getBucket(key).set(value, cacheMillis, TimeUnit.MILLISECONDS);
                },
                (valueMap) -> {
                    valueMap.forEach((k, v) -> {
                        redissonClient.getBucket(k).set(v, cacheMillis, TimeUnit.MILLISECONDS);
                    });
                },
                (key) -> {
                    redissonClient.getBucket(key).delete();
                },
                singleKeyTemplate, idGetter, cacheKeyGetter, cacheDummy, dummy);

        this.redissonClient = redissonClient;
        this.cacheMillis = cacheMillis;
        this.cacheDummy = cacheDummy;
    }
}
