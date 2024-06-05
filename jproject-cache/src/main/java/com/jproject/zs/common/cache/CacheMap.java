package com.jproject.zs.common.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/6/8
 */
public interface CacheMap<T, ID> {

    default T writeValue(ID id, Supplier<T> supplier) {
        return writeValue(id, supplier, false);
    }

    /**
     * @param supplier
     * @param cacheValueAtOnce false 则kick缓存，true则直接刷新新值
     * @return
     */
    T writeValue(ID id, Supplier<T> supplier, boolean cacheValueAtOnce);

    default Map<ID, T> writeBatchValue(Supplier<Map<ID, T>> supplier) {
        return writeBatchValue(supplier, false);
    }

    // todo 需要考虑的是，这个write批量是否确认时全部，
    Map<ID, T> writeBatchValue(Supplier<Map<ID, T>> supplier, boolean cacheValueAtOnce);

    Optional<T> readValue(ID id, Supplier<Optional<T>> supplier);

    Map<ID, T> readValueByIds(List<ID> ids, Function<List<ID>, Map<ID, T>> valueGetter);

    Optional<T> readCacheOnly(ID id);

    Map<ID, T> readCacheOnly(List<ID> ids);

    void cleanCacheById(ID cacheKey);

    void cleanCacheByIds(List<ID> cacheKey);

    void updateCache(ID id, T value);

    void updateBatchCache(Map<ID, T> values);
}
