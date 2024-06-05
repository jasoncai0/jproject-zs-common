package com.jproject.zs.common.cache;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * 这个版本的cache服务要求cachevalue本身必须携带id，相信大多数时候是能够做到的，不能的时候其实也可以通过优化存储解决， 并不会带来多少损失
 *
 * @author caizhensheng
 * @desc
 * @date 2022/5/6
 */
public interface CacheService<T, ID> {


    default T writeValue(Supplier<T> supplier) {
        return writeValue(supplier, false);
    }

    /**
     * @param supplier
     * @param cacheValueAtOnce false 则kick缓存，true则直接刷新新值
     * @return
     */
    T writeValue(Supplier<T> supplier, boolean cacheValueAtOnce);


    default List<T> writeBatchValue(Supplier<List<T>> supplier) {
        return writeBatchValue(supplier, false);
    }

    // todo 需要考虑的是，这个write批量是否确认时全部，
    List<T> writeBatchValue(Supplier<List<T>> supplier, boolean cacheValueAtOnce);


    Optional<T> readValue(ID id, Supplier<Optional<T>> supplier);


    Optional<T> readCacheOnly(ID id);

    List<T> readCacheOnly(List<ID> ids);


    List<T> readValueByIds(List<ID> ids, Function<List<ID>, List<T>> valueGetter);

    void cleanCacheByValue(T cached);

    void cleanCacheById(ID cacheKey);

    void updateCache(T value);

    void updateBatchCache(List<T> values);


}
