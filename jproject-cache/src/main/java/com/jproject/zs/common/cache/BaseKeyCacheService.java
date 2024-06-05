package com.jproject.zs.common.cache;

import com.jproject.zs.common.cache.util.CollectionUtils;
import io.vavr.Tuple2;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/5/6
 */
@RequiredArgsConstructor
public class BaseKeyCacheService<T, ID> implements CacheService<T, ID> {


//    private final RedissonClient redissonClient;

    private final Function<String, T> singleValueGetter;
    private final Function<List<String>, Map<String, T>> multiValueGetter;
    private final BiConsumer<String, T> singleValueSetter;
    private final Consumer<Map<String, T>> multiValueSetter;

    private final Consumer<String> singleKeyCleaner;

//    private final Consumer<List<String>> multiCleaner;

    private final String singleKeyTemplate;

    private final Function<T, ID> idGetter;

    private final Function<ID, String> cacheKeyIdGetter;

    private final boolean cacheDummy;

    private final T dummy;

//    private final long cacheMillis;


    @Override
    public T writeValue(Supplier<T> supplier, boolean cacheValueAtOnce) {

        T result = supplier.get();

        if (cacheValueAtOnce) {
            updateCache(result);
        } else {
            cleanCacheByValue(result);
        }
        return result;
    }


    // todo 需要考虑的是，这个write批量是否确认时全部，
    @Override
    public List<T> writeBatchValue(Supplier<List<T>> supplier, boolean cacheValueAtOnce) {
        List<T> list = supplier.get();
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        if (cacheValueAtOnce) {
            updateBatchCache(list);
        } else {
            list.forEach(this::cleanCacheByValue);
        }
        return list;
    }

    // TODO optimize： supplier-> function
    @Override
    public Optional<T> readValue(ID id, Supplier<Optional<T>> supplier) {

        String cacheKey = getSingleCacheKey(cacheKeyIdGetter.apply(id));

        T cached = singleValueGetter.apply(cacheKey);
        if (cached == null) {

            cached = supplier.get().orElse(null);
            if (cached == null) {

                if (cacheDummy) {
                    singleValueSetter.accept(cacheKey, dummy);
                }

            } else {
                singleValueSetter.accept(cacheKey, cached);
            }
        }

        if (cacheDummy && Objects.equals(cached, dummy)) {
            cached = null;
        }
        return Optional.ofNullable(cached);
    }

    @Override
    public Optional<T> readCacheOnly(ID id) {
        return getWithDummy(getSingleCacheKey(cacheKeyIdGetter.apply(id)), dummy);
    }

    @Override
    public List<T> readCacheOnly(List<ID> ids) {
        throw new UnsupportedOperationException("unsupported in current version");
    }

    @Override
    public List<T> readValueByIds(List<ID> ids, Function<List<ID>, List<T>> valueGetter) {

        List<T> result = new ArrayList<>();
        Map<ID, T> resultMap = new HashMap<>();

        List<ID> absentIds = new ArrayList<>();

        // try fetch by single cache
        // TODO load-by-multiGet e.g: RBatch batch = redissonClient.createBatch();
        tryLoadBySingleCacheKey(ids, absentIds::add, resultMap::put);

        // load
        getAndUpdateBatchCacheValues(absentIds, (id, item) -> resultMap.put(idGetter.apply(item), item), valueGetter);


        // update dummy
        Map<String, T> dummys = new HashMap<>();
        ids.forEach(id -> {
            T r = resultMap.get(id);
            if (r != null) {
                result.add(r);
            } else if (cacheDummy) {
                dummys.put(getSingleCacheKey(cacheKeyIdGetter.apply(id)), dummy);
//                getSingleBucket(cacheKeyGetter.apply(id)).set(dummy, cacheMillis, TimeUnit.MILLISECONDS);
            }
        });


        if (!CollectionUtils.isEmpty(dummys)) {
            multiValueSetter.accept(dummys);
        }


        return result.stream()
                .filter(r -> {
                    return !(cacheDummy && Objects.equals(r, dummy));
                }).collect(Collectors.toList());
    }


    private void tryLoadBySingleCacheKey(List<ID> ids,
                                         Consumer<ID> absenConsumer,
                                         BiConsumer<ID, T> existedConsumer) {
        Map<String, ID> loadingIds = ids.stream().map(id -> {
            return new Tuple2<String, ID>(getSingleCacheKey(cacheKeyIdGetter.apply(id)), id);
        }).collect(Collectors.toMap(pair -> pair._1, pair -> pair._2, (o1, o2) -> o2));

        Map<String, T> loadedCache = multiValueGetter.apply(new ArrayList<>(loadingIds.keySet()));


        ids.forEach(id -> {
            String cacheKey = getSingleCacheKey(cacheKeyIdGetter.apply(id));
            if (!loadedCache.containsKey(cacheKey)) {
                absenConsumer.accept(id);
                return;
            }

            T loaded = loadedCache.get(cacheKey);

            if (loaded == null) {
                absenConsumer.accept(id);
            } else {
                existedConsumer.accept(id, loaded);
            }
        });


    }



    @Override
    public void cleanCacheByValue(T cached) {

        if (cached == null) {
            return;
        }
        ID id = idGetter.apply(cached);

        cleanCacheById(id);

    }

    @Override
    public void cleanCacheById(ID cacheKey) {
        if (cacheKey != null) {
            String key = getSingleCacheKey(cacheKeyIdGetter.apply(cacheKey));

            singleKeyCleaner.accept(key);
        }
    }

    @Override
    public void updateCache(T value) {
        this.setCacheWithDummy(
                getSingleCacheKey(cacheKeyIdGetter.apply(idGetter.apply(value))),
                dummy, () -> Optional.ofNullable(value));

    }

    @Override
    public void updateBatchCache(List<T> values) {

        this.updateBatchCacheValues(values);
    }


    private String getSingleCacheKey(String keyWithoutPrefix) {
        return String.format(singleKeyTemplate, keyWithoutPrefix);
    }


    private void getAndUpdateBatchCacheValues(List<ID> absentIds, BiConsumer<ID, T> existedConsumer,
                                              Function<List<ID>, List<T>> batchValueGetter) {

        if (CollectionUtils.isEmpty(absentIds)) {
            return;
        }

        // load
        Map<String, T> loadedBatch = batchValueGetter.apply(absentIds)
                .stream()
                .collect(Collectors.toMap(value -> {
                    ID id = idGetter.apply(value);

                    existedConsumer.accept(id, value);

                    return getSingleCacheKey(cacheKeyIdGetter.apply(id));

                }, value -> value, (o1, o2) -> o2));

        multiValueSetter.accept(loadedBatch);
    }


    private void updateBatchCacheValues(List<T> values) {


        if (CollectionUtils.isEmpty(values)) {
            return;
        }

        // load
        Map<String, T> loadedBatch = values
                .stream()
                .collect(Collectors.toMap(value -> {
                    ID id = idGetter.apply(value);

                    return getSingleCacheKey(cacheKeyIdGetter.apply(id));

                }, value -> value, (o1, o2) -> o2));


        multiValueSetter.accept(loadedBatch);
    }

    /**
     * 注意这个方法的空值判断写死是Objects::isNull
     */
    protected Optional<T> getAndSetCacheWithDummy(String cacheKey, T dummy, Supplier<Optional<T>> valueGetter) {
        T cached = singleValueGetter.apply(cacheKey);
        if (cached == null) {

            cached = valueGetter.get().orElse(null);
            if (cached == null) {

                if (cacheDummy) {
                    singleValueSetter.accept(cacheKey, dummy);
                }

            } else {
                singleValueSetter.accept(cacheKey, cached);
            }
        }

        if (cacheDummy && Objects.equals(cached, dummy)) {
            cached = null;
        }
        return Optional.ofNullable(cached);
    }


    protected void setCacheWithDummy(String cacheKey, T dummy, Supplier<Optional<T>> valueGetter) {

        T valTryPut = valueGetter.get().orElse(cacheDummy ? dummy : null);

        if (valTryPut == null) {
            singleKeyCleaner.accept(cacheKey);
        } else {
            singleValueSetter.accept(cacheKey, valTryPut);
        }
    }


    /**
     * 注意这个方法的空值判断写死是Objects::isNull
     */
    protected Optional<T> getWithDummy(String cacheKey, T dummy) {
        T cached = singleValueGetter.apply(cacheKey);


        if (cacheDummy && Objects.equals(cached, dummy)) {
            cached = null;
        }
        return Optional.ofNullable(cached);
    }
}

