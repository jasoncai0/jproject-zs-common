package com.jproject.zs.common.cache;

import com.jproject.zs.common.cache.util.CollectionUtils;
import io.vavr.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/6/8
 */
@Slf4j
@RequiredArgsConstructor
public class CacheMapRedisKeyImpl<T, ID> implements CacheMap<T, ID> {


    private final Function<String, T> singleKeyGetter;

    private final Function<List<String>, Map<String, T>> multiKeyGetter;

    private final BiConsumer<String, T> singleKeySetter;

    private final Consumer<Map<String, T>> multiKeySetter;

    private final Consumer<String> singleKeyCleaner;

    private final Consumer<List<String>> multiKeyCleaner;

    private final String singleKeyTemplate;

    private final Function<ID, String> cacheKeyIdGetter;

    private final boolean cacheDummy;

    private final T dummy;


    @Override
    public T writeValue(ID id, Supplier<T> supplier, boolean cacheValueAtOnce) {

        T result = supplier.get();

        if (cacheValueAtOnce) {
            updateCache(id, result);
        } else {
            cleanCacheById(id);
        }
        return result;
    }


    // todo 需要考虑的是，这个write批量是否确认时全部，
    @Override
    public Map<ID, T> writeBatchValue(Supplier<Map<ID, T>> supplier, boolean cacheValueAtOnce) {
        Map<ID, T> list = supplier.get();
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        if (cacheValueAtOnce) {
            updateBatchCache(list);
        } else {
            cleanCacheByIds(new ArrayList<>(list.keySet()));
        }
        return list;
    }

    // TODO optimize： supplier-> function
    @Override
    public Optional<T> readValue(ID id, Supplier<Optional<T>> supplier) {
        String cacheKey = getSingleCacheKey(cacheKeyIdGetter.apply(id));

        T cached = singleKeyGetter.apply(cacheKey);
        if (cached == null) {

            cached = supplier.get().orElse(null);
            if (cached == null) {

                if (cacheDummy) {
                    singleKeySetter.accept(cacheKey, dummy);
                }

            } else {
                singleKeySetter.accept(cacheKey, cached);
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
    public Map<ID, T> readCacheOnly(List<ID> ids) {

        if (CollectionUtils.isEmpty(ids)) {
            return new HashMap<>();
        }

        Map<String, ID> keys = ids.stream().filter(id -> id != null)
                .collect(Collectors.toMap(id -> getSingleCacheKey(cacheKeyIdGetter.apply(id)), id -> id, (o1, o2) -> o2));

        if (CollectionUtils.isEmpty(keys)) {
            return new HashMap<>();
        }

        return Optional.ofNullable(multiKeyGetter
                .apply(new ArrayList<>(keys.keySet())))
                .orElse(new HashMap<>())
                .entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> keys.get(entry.getKey()),
                        entry -> entry.getValue(), (o1, o2) -> o2
                ));
    }

    @Override
    public Map<ID, T> readValueByIds(List<ID> ids, Function<List<ID>, Map<ID, T>> valueGetter) {
        Map<ID, T> result = new HashMap<>();
        Map<ID, T> resultMap = new HashMap<>();

        List<ID> absentIds = new ArrayList<>();

        // try fetch by single cache
        // TODO load-by-multiGet e.g: RBatch batch = redissonClient.createBatch();
        tryLoadBySingleCacheKey(ids, absentIds::add, resultMap::put);

        // load
        getAndUpdateBatchCacheValues(absentIds, resultMap::put, valueGetter);

        // update dummy
        Map<String, T> dummys = new HashMap<>();
        ids.forEach(id -> {
            T r = resultMap.get(id);
            if (r != null) {
                result.put(id, r);
            } else if (cacheDummy) {
                dummys.put(getSingleCacheKey(cacheKeyIdGetter.apply(id)), dummy);
            }
        });

        if (!CollectionUtils.isEmpty(dummys)) {
            multiKeySetter.accept(dummys);
        }

        return result.entrySet().stream()
                .filter(r -> {
                    return !(cacheDummy && Objects.equals(r.getValue(), dummy));
                })
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }


    private void tryLoadBySingleCacheKey(List<ID> allIds,
                                         Consumer<ID> absenConsumer,
                                         BiConsumer<ID, T> existedConsumer) {
        Map<String, ID> loadingIds = allIds.stream().map(id -> {
            return new Tuple2<String, ID>(getSingleCacheKey(cacheKeyIdGetter.apply(id)), id);
        }).collect(Collectors.toMap(pair -> pair._1, pair -> pair._2, (o1, o2) -> o2));

        Map<String, T> loadedCache = multiKeyGetter.apply(new ArrayList<>(loadingIds.keySet()));


        allIds.forEach(id -> {
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
    public void cleanCacheById(ID cacheKey) {

        if (cacheKey != null) {
            String key = getSingleCacheKey(cacheKeyIdGetter.apply(cacheKey));

            singleKeyCleaner.accept(key);
        }
    }

    @Override
    public void cleanCacheByIds(List<ID> cacheKey) {
        if (CollectionUtils.isEmpty(cacheKey)) {
            return;
        }

        List<String> keys = cacheKey.stream()
                .filter(Objects::nonNull)
                .map(id -> getSingleCacheKey(cacheKeyIdGetter.apply(id)))
                .collect(Collectors.toList());

        multiKeyCleaner.accept(keys);

    }

    @Override
    public void updateCache(ID id, T value) {


        String cacheKey = getSingleCacheKey(cacheKeyIdGetter.apply(id));

        this.setCacheWithDummy(cacheKey, dummy, () -> Optional.ofNullable(value));

    }

    @Override
    public void updateBatchCache(Map<ID, T> values) {
        this.updateBatchCacheValues(values);
    }


    private String getSingleCacheKey(String keyWithoutPrefix) {
        return String.format(singleKeyTemplate, keyWithoutPrefix);
    }


    private void getAndUpdateBatchCacheValues(List<ID> absentIds,
                                              BiConsumer<ID, T> existedConsumer,
                                              Function<List<ID>, Map<ID, T>> batchValueGetter) {

        if (CollectionUtils.isEmpty(absentIds)) {
            return;
        }

        // load
        Map<String, T> loadedBatch = batchValueGetter.apply(absentIds)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> {
                    ID id = entry.getKey();

                    existedConsumer.accept(id, entry.getValue());

                    return getSingleCacheKey(cacheKeyIdGetter.apply(id));

                }, entry -> entry.getValue(), (o1, o2) -> o2));

        multiKeySetter.accept(loadedBatch);
    }


    private void updateBatchCacheValues(Map<ID, T> values) {

        if (CollectionUtils.isEmpty(values)) {
            return;
        }

        List<String> absentCacheKeys = new ArrayList<>();

        // load
        Map<String, T> loadedBatch = values
                .entrySet()
                .stream()
                .filter(entry -> {
                    if (entry.getValue() == null) {
                        absentCacheKeys.add(getSingleCacheKey(cacheKeyIdGetter.apply(entry.getKey())));
                        return false;

                    }
                    return true;
                })
                .collect(Collectors.toMap(entry -> {
                    ID id = entry.getKey();
                    return getSingleCacheKey(cacheKeyIdGetter.apply(id));
                }, entry -> entry.getValue(), (o1, o2) -> o2));

        if (!CollectionUtils.isEmpty(absentCacheKeys)) {
            multiKeyCleaner.accept(absentCacheKeys);
        }
        if (!CollectionUtils.isEmpty(loadedBatch)) {
            multiKeySetter.accept(loadedBatch);
        }

    }

    /**
     * 注意这个方法的空值判断写死是Objects::isNull
     */
    protected Optional<T> getAndSetCacheWithDummy(String cacheKey, T dummy, Supplier<Optional<T>> valueGetter) {
        T cached = singleKeyGetter.apply(cacheKey);
        if (cached == null) {

            cached = valueGetter.get().orElse(null);
            if (cacheDummy && cached == null) {
                // 防止无效的缓存
                cached = dummy;
                singleKeySetter.accept(cacheKey, cached);
            } else {
                singleKeySetter.accept(cacheKey, cached);
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
            singleKeySetter.accept(cacheKey, valTryPut);
        }
    }


    /**
     * 注意这个方法的空值判断写死是Objects::isNull
     */
    protected Optional<T> getWithDummy(String cacheKey, T dummy) {
        T cached = singleKeyGetter.apply(cacheKey);


        if (cacheDummy && Objects.equals(cached, dummy)) {
            cached = null;
        }
        return Optional.ofNullable(cached);
    }


}
