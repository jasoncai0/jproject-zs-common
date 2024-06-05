package com.jproject.zs.common.cache;

import org.redisson.api.RedissonClient;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/5/7
 */
public class CacheServiceRedissonHashImpl<E, KEY, FIELD> extends BaseHashCacheService<E, KEY, FIELD> {

    private final RedissonClient redissonClient;

    private final long cacheMillis;

    public CacheServiceRedissonHashImpl(RedissonClient redissonClient,
                                        String allFieldsKeyTemplate,
                                        String partialExistedKeyTemplate, String dummyKeyTemplate,
                                        Function<E, KEY> keyGetter, Function<E, FIELD> fieldGetter,
                                        Function<KEY, String> keySerializer,
                                        Function<FIELD, String> fieldSerializer, E dummy,
                                        long cacheMillis) {
        super((key, filed) -> {
                    return redissonClient.<String, E>getMap(key).get(filed);
                },
                (key, fields) -> {
                    return redissonClient.<String, E>getMap(key).getAll(new HashSet<>(fields));
                },
                (tuple, value) -> {
                    redissonClient.getMap(tuple._1).put(tuple._2, value);
                },
                (key, field) -> {
                    redissonClient.getMap(key).remove(field);
                },

                (valueCacheKey) -> {
                    redissonClient.<String, E>getMap(valueCacheKey).expire(cacheMillis, TimeUnit.MILLISECONDS);
                },

                allFieldsCacheKey -> {
                    return redissonClient.<List<String>>getBucket(allFieldsCacheKey).get();
                },

                (allFieldsCacheKey, allFields) -> {
                    redissonClient.<List<String>>getBucket(allFieldsCacheKey).set(allFields, cacheMillis, TimeUnit.MILLISECONDS);
                },
                (allFieldsCacheKey) -> {
                    redissonClient.getBucket(allFieldsCacheKey).delete();
                },
                allFieldsKeyTemplate,
                partialExistedKeyTemplate, dummyKeyTemplate, keyGetter, fieldGetter, keySerializer, fieldSerializer, dummy);
        this.redissonClient = redissonClient;
        this.cacheMillis = cacheMillis;

    }
}
