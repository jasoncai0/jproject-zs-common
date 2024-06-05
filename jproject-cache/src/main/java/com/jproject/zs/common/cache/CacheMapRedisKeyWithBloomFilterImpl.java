package com.jproject.zs.common.cache;

import com.jproject.zs.common.cache.bloomfilter.RedisBloomFilter;
import com.jproject.zs.common.cache.bloomfilter.RedisOpt;
import com.google.common.base.Charsets;
import com.google.common.hash.Funnels;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 使用redisKey 作为缓存值，使用BF进行空值的方穿透；
 * <p>
 * 使用时特别需要注意的是，需要加热存量数据，保证已存在数据已经全部装配，避免混淆不存在的数据和未装载的数据；
 *
 * @author caizhensheng
 * @desc
 * @date 2023/1/6
 */
public class CacheMapRedisKeyWithBloomFilterImpl<E, ID> implements CacheMap<E, ID> {

    private CacheMapRedisKeyImpl<E, ID> valueCache;

    /**
     * 存放存在的数据的bf；
     */
    private RedisBloomFilter<CharSequence> bloomFilter;

    private Function<ID, String> cacheKeyIdGetter;

    public CacheMapRedisKeyWithBloomFilterImpl(Function<String, E> singleKeyGetter,
                                               Function<List<String>, Map<String, E>> multiKeyGetter,
                                               BiConsumer<String, E> singleKeySetter,
                                               Consumer<Map<String, E>> multiKeySetter,
                                               Consumer<String> singleKeyCleaner,
                                               Consumer<List<String>> multiKeyCleaner,
                                               String singleKeyTemplate,
                                               Function<ID, String> cacheKeyIdGetter,

                                               String bloomFilterRedisKey, long capacity, double errorRate,

                                               RedisOpt redisOpt
    ) {

        this.valueCache = new CacheMapRedisKeyImpl<>(singleKeyGetter, multiKeyGetter, singleKeySetter, multiKeySetter, singleKeyCleaner, multiKeyCleaner,
                singleKeyTemplate, cacheKeyIdGetter, false, null);

        this.bloomFilter = new RedisBloomFilter<>(bloomFilterRedisKey, Funnels.stringFunnel(Charsets.UTF_8), capacity, errorRate, redisOpt);

        this.cacheKeyIdGetter = cacheKeyIdGetter;
    }


    @Override
    public E writeValue(ID id, Supplier<E> supplier, boolean cacheValueAtOnce) {
        try {
            return valueCache.writeValue(id, supplier, cacheValueAtOnce);
        } finally {
            this.bloomFilter.put(cacheKeyIdGetter.apply(id));
        }
    }

    @Override
    public Map<ID, E> writeBatchValue(Supplier<Map<ID, E>> supplier, boolean cacheValueAtOnce) {
        Map<ID, E> loadedValues = new HashMap<>();
        try {
            return valueCache.writeBatchValue(() -> {
                Map<ID, E> values = supplier.get();
                loadedValues.putAll(values);
                return values;
            }, cacheValueAtOnce);
        } finally {
            if (!loadedValues.isEmpty()) {
                this.bloomFilter.putAll(loadedValues.keySet().stream().map(cacheKeyIdGetter).distinct().collect(Collectors.toList()));
            }
        }
    }

    @Override
    public Optional<E> readValue(ID id, Supplier<Optional<E>> supplier) {
        if (bloomFilter.mightContain(cacheKeyIdGetter.apply(id))) {
            return valueCache.readValue(id, supplier);
        } else {
            return Optional.empty();
        }
    }


    @Override
    public Map<ID, E> readValueByIds(List<ID> ids, Function<List<ID>, Map<ID, E>> valueGetter) {

        List<ID> mightContainIds = ids.stream().distinct()
                .filter(id -> {
                    return bloomFilter.mightContain(cacheKeyIdGetter.apply(id));
                })
                .collect(Collectors.toList());


        return valueCache.readValueByIds(mightContainIds, valueGetter);
    }


    @Override
    public Optional<E> readCacheOnly(ID id) {
        return valueCache.readCacheOnly(id);
    }

    @Override
    public Map<ID, E> readCacheOnly(List<ID> ids) {
        return valueCache.readCacheOnly(ids);
    }


    @Override
    public void cleanCacheById(ID cacheKey) {
        throw new UnsupportedOperationException("delete operation is not supported in bf");
    }

    @Override
    public void cleanCacheByIds(List<ID> cacheKey) {
        throw new UnsupportedOperationException("delete operation is not supported in bf");
    }

    @Override
    public void updateCache(ID id, E value) {
        try {
            valueCache.updateCache(id, value);
        } finally {
            bloomFilter.put(cacheKeyIdGetter.apply(id));
        }
    }

    @Override
    public void updateBatchCache(Map<ID, E> values) {
        try {
            valueCache.updateBatchCache(values);
        } finally {
            bloomFilter.putAll(values.keySet().stream().map(cacheKeyIdGetter).distinct().collect(Collectors.toList()));
        }
    }
}
