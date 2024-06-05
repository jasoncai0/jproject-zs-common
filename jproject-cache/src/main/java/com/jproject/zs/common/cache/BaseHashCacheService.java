package com.jproject.zs.common.cache;

import com.jproject.zs.common.cache.util.CollectionUtils;
import io.vavr.Tuple2;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Collectors;


/**
 * @author caizhensheng
 * @desc
 * @date 2022/5/6
 * @deprecated 不稳定
 */
@Deprecated
@RequiredArgsConstructor
public class BaseHashCacheService<E, KEY, FIELD> implements CacheService<E, Tuple2<KEY, FIELD>> {


    private final BiFunction<String, String, E> singleFieldGetter;

    private final BiFunction<String, List<String>, Map<String, E>> multiFiledLoader;

    private final BiConsumer<Tuple2<String, String>, E> singleFieldSetter;

    private final BiConsumer<String, String> singleFieldCleaner;

    private final Consumer<String> valueCacheKeyExpireTimeRefresher;

    private final Function<String, List<String>> singleKeyGetter;

    private final BiConsumer<String, List<String>> allFiledSetter;

    private final Consumer<String> singleKeyCleaner;


    private final String allFieldsKeyTemplate;
    private final String partialExistedKeyTemplate;
    private final String dummyKeyTemplate;
    private final Function<E, KEY> keyGetter;
    private final Function<E, FIELD> fieldGetter;
    private final Function<KEY, String> keySerializer;
    private final Function<FIELD, String> fieldSerializer;
    private final E dummy;


    @Override
    public E writeValue(Supplier<E> supplier, boolean cacheValueAtOnce) {

        E result = supplier.get();

        if (cacheValueAtOnce) {
            updateCache(result);
        } else {
            cleanCacheByValue(result);
        }

        return result;
    }

    @Override
    public List<E> writeBatchValue(Supplier<List<E>> supplier, boolean cacheValueAtOnce) {

        List<E> values = supplier.get();

        if (CollectionUtils.isEmpty(values)) {
            return values;
        }

        if (cacheValueAtOnce) {
            updateBatchCache(values);
        } else {
            values.forEach(this::cleanCacheByValue);
        }

        return values;
    }

    @Override
    public Optional<E> readValue(Tuple2<KEY, FIELD> tuple2, Supplier<Optional<E>> supplier) {

        return getAndSetCacheWithDummy(tuple2._1, tuple2._2, supplier);
    }

    @Override
    public Optional<E> readCacheOnly(Tuple2<KEY, FIELD> tuple2) {
        return getWithDummy(tuple2._1, tuple2._2);
    }

    @Override
    public List<E> readCacheOnly(List<Tuple2<KEY, FIELD>> tuple2s) {
        throw new UnsupportedOperationException("当前版本下没有必要支持");
    }

    public <C extends Collection<E>> C readAllValuesByKeyName(KEY key, Function<KEY, C> readByKeyFunction, Function<Collection<E>, C> adapter) {

        String allFiledCacheKey = getOrBuildAllFiledsBucket(key);

        List<String> allFieldIds = singleKeyGetter.apply(allFiledCacheKey);

        if (allFieldIds == null) {

            return reloadAllValuesAndUpdateCache(allFiledCacheKey, key, readByKeyFunction);

        } else {

            String cacheMap = getOrBuildPartialExistedMap(key);

            Map<String, E> allExisted = multiFiledLoader.apply(cacheMap, allFieldIds);

            if (allExisted.keySet().containsAll(allFieldIds)) {
                return adapter.apply(allExisted.values());
            } else {
                return reloadAllValuesAndUpdateCache(allFiledCacheKey, key, readByKeyFunction);
            }
        }
    }


    private <C extends Collection<E>> C reloadAllValuesAndUpdateCache(String allFieldsCacheKey, KEY key, Function<KEY, C> readByKeyFunction) {

        C allValues = readByKeyFunction.apply(key);


        String cacheMap = getOrBuildPartialExistedMap(key);


        String dummyMap = getOrBuildDummyMap(key);


        valueCacheKeyExpireTimeRefresher.accept(cacheMap);


        allValues.forEach(value -> updateLoadedValue(cacheMap, dummyMap, value, fieldSerializer.apply(fieldGetter.apply(value))));


        allFiledSetter.accept(allFieldsCacheKey, allValues.stream().map(value -> fieldSerializer.apply(fieldGetter.apply(value))).collect(Collectors.toList()));


        return allValues;
    }

    @Override
    public List<E> readValueByIds(List<Tuple2<KEY, FIELD>> tuple2s, Function<List<Tuple2<KEY, FIELD>>, List<E>> valueGetter) {
        //
        throw new UnsupportedOperationException("当前版本下没有必要支持");
    }


    // 对于写操作，如果只是删除hash中的field，那么values操作就可能是错的，因为可能只包含部分
    @Override
    public void cleanCacheByValue(E cached) {
        if (cached == null) {
            return;
        }

        cleanCacheById(new Tuple2<>(keyGetter.apply(cached), fieldGetter.apply(cached)));

    }

    @Override
    public void cleanCacheById(Tuple2<KEY, FIELD> cacheKey) {
        if (cacheKey == null || cacheKey._1 == null || cacheKey._2 == null) {
            return;
        }
        String field = fieldSerializer.apply(cacheKey._2);

        // 当发生更新的时候，更新全局表，TODO 目前比较粗暴不区分增删改查，直接全部删除
        singleKeyCleaner.accept(getOrBuildAllFiledsBucket(cacheKey._1));

        singleFieldCleaner.accept(getOrBuildPartialExistedMap(cacheKey._1), field);

        singleFieldCleaner.accept(getOrBuildDummyMap(cacheKey._1), field);

    }

    @Override
    public void updateCache(E value) {
        cleanCacheByValue(value);
        getAndSetCacheWithDummy(keyGetter.apply(value), fieldGetter.apply(value), () -> Optional.ofNullable(value));
    }

    @Override
    public void updateBatchCache(List<E> values) {
        throw new UnsupportedOperationException("当前版本下不支持");
    }


    /**
     * 注意这个方法的空值判断写死是Objects::isNull
     */
    protected Optional<E> getAndSetCacheWithDummy(KEY key, FIELD field, Supplier<Optional<E>> loader) {

        String filedKey = Optional.ofNullable(field).map(fieldSerializer).orElse(null);
        if (filedKey == null) {
            return Optional.empty();
        }


        String existedMap = getOrBuildPartialExistedMap(key);

        E existed = singleFieldGetter.apply(existedMap, filedKey);

        if (existed != null) {

            return Optional.of(existed);

        }


        String dummyMap = getOrBuildDummyMap(key);

        E dummyValue = singleFieldGetter.apply(dummyMap, filedKey);

        if (dummyValue != null) {
            return Optional.empty();
        }

        //  如果两者都不存在，那么我们就真实的
        E loaded = loader.get().orElse(null);

        updateLoadedValue(existedMap, dummyMap, loaded, filedKey);

        return Optional.ofNullable(loaded);

    }


    /**
     * 注意这个方法的空值判断写死是Objects::isNull
     */
    protected Optional<E> getWithDummy(KEY key, FIELD field) {

        String filedKey = Optional.ofNullable(field).map(fieldSerializer).orElse(null);
        if (filedKey == null) {
            return Optional.empty();
        }


        String existedMap = getOrBuildPartialExistedMap(key);

        E existed = singleFieldGetter.apply(existedMap, filedKey);

        if (existed != null) {

            return Optional.of(existed);

        }


        String dummyMap = getOrBuildDummyMap(key);

        E dummyValue = singleFieldGetter.apply(dummyMap, filedKey);

        if (dummyValue != null) {
            return Optional.empty();
        }


        return Optional.empty();

    }

    private void updateLoadedValue(String partialExisted,

                                   String dummyCache,

                                   E presentValue, String fieldKey) {


        if (presentValue != null) {

            singleFieldSetter.accept(new Tuple2<>(partialExisted, fieldKey), presentValue);

            singleFieldCleaner.accept(dummyCache, fieldKey);

        } else {

            singleFieldCleaner.accept(partialExisted, fieldKey);

            singleFieldSetter.accept(new Tuple2<>(dummyCache, fieldKey), dummy);
        }

    }


//    /**
//     * 该方法在不存在时不会创建，
//     * <p>
//     * 能够判断是否已经有已经数据
//     *
//     * @param key
//     * @return
//     */
//    private Optional<RMap<String, E>> getPartialExistedMap(KEY key) {
//
//        String keyName = String.format(partialExistedKeyTemplate, keySerializer.apply(key));
//
//        long count = redissonKeys.countExists(keyName);
//
//        if (count == 0) {
//            return Optional.empty();
//        }
//
//        return Optional.of(redissonClient.getMap(keyName));
//    }


    private String getOrBuildAllFiledsBucket(KEY key) {
        return String.format(allFieldsKeyTemplate, keySerializer.apply(key));
    }

    // 明确在的缓存
    private String getOrBuildPartialExistedMap(KEY key) {
        return String.format(partialExistedKeyTemplate, keySerializer.apply(key));
    }

    // 明确不在的缓存 ， dummy map 代表的是明确不在池子中的对象
    private String getOrBuildDummyMap(KEY key) {
        return String.format(dummyKeyTemplate, keySerializer.apply(key));
    }

}
