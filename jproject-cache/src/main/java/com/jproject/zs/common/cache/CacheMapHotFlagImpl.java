package com.jproject.zs.common.cache;

import com.jproject.zs.common.cache.util.CollectionUtils;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 该组件的目的的是为了防止缓存穿透，实现方式，每个key缓存两个数据
 * 1.过期时间更短的flag
 * 2.实际的缓存值，
 * <p>
 * load数据过程：
 * 1.当load数据时发现缓存值存在而flag不存在，那么返回值，并异步reload数据，更新flag及缓存值
 * 2.当load数据存在且flag也存在，直接返回；
 * 2.当load 数据也不存在时，重新load数据，并返回值， 更新flag及缓存值；
 *
 * @author caizhensheng
 * @desc
 * @date 2023/1/4
 */
@Slf4j
public class CacheMapHotFlagImpl<E, ID> implements CacheMap<E, ID> {

    private CacheMap<E, ID> valueCache;

    private Function<String, Boolean> hotFlagSetterIfAbsent;
    private Consumer<String> hotFlagSetter;
    private Consumer<List<String>> hotFlagMultiSetter;
    private Consumer<String> singleKeyCleaner;
    private Consumer<List<String>> multiKeyCleaner;

    private Function<ID, String> cacheKeyIdGetter;
    private String hotFlagKeyTemplate;
    private ExecutorService reloadExecutor;


    public CacheMapHotFlagImpl(Function<String, E> singleKeyGetter,
                               Function<List<String>, Map<String, E>> multiKeyGetter,
                               BiConsumer<String, E> singleKeySetter,
                               Consumer<Map<String, E>> multiKeySetter,
                               Consumer<String> singleKeyCleaner,
                               Consumer<List<String>> multiKeyCleaner,
                               String singleKeyTemplate,
                               Function<ID, String> cacheKeyIdGetter,
                               boolean cacheDummy, E dummy,
                               Function<String, Boolean> hotFlagSetterIfAbsent,
                               Consumer<String> hotFlagSetter,
                               Consumer<List<String>> hotFlagMultiSetter,
                               // 注意需要继承context的时候就及时的修饰executor
                               ExecutorService cacheValueReloadAsyncExecutor) {

        this.valueCache = new CacheMapRedisKeyImpl<>(singleKeyGetter, multiKeyGetter,
                singleKeySetter, multiKeySetter,
                singleKeyCleaner, multiKeyCleaner,
                singleKeyTemplate, cacheKeyIdGetter,
                cacheDummy, dummy);
        this.hotFlagSetterIfAbsent = hotFlagSetterIfAbsent;
        this.hotFlagSetter = hotFlagSetter;
        this.hotFlagMultiSetter = hotFlagMultiSetter;
        this.cacheKeyIdGetter = cacheKeyIdGetter;
        this.singleKeyCleaner = singleKeyCleaner;
        this.multiKeyCleaner = multiKeyCleaner;
        this.hotFlagKeyTemplate = singleKeyTemplate + ".flag";
        this.reloadExecutor = cacheValueReloadAsyncExecutor;
    }


    public CacheMapHotFlagImpl(
            CacheMap<E, ID> valueCache,
            String hotFlagKeyTemplate,
            Consumer<String> singleKeyCleaner,
            Consumer<List<String>> multiKeyCleaner,
            Function<ID, String> cacheKeyIdGetter,
            Function<String, Boolean> hotFlagSetterIfAbsent,
            Consumer<String> hotFlagSetter,
            Consumer<List<String>> hotFlagMultiSetter,
            // 注意需要继承context的时候就及时的修饰executor
            ExecutorService cacheValueReloadAsyncExecutor) {

        this.valueCache = valueCache;
        this.hotFlagSetterIfAbsent = hotFlagSetterIfAbsent;
        this.hotFlagSetter = hotFlagSetter;
        this.hotFlagMultiSetter = hotFlagMultiSetter;
        this.cacheKeyIdGetter = cacheKeyIdGetter;
        this.singleKeyCleaner = singleKeyCleaner;
        this.multiKeyCleaner = multiKeyCleaner;
        this.hotFlagKeyTemplate = hotFlagKeyTemplate;
        this.reloadExecutor = cacheValueReloadAsyncExecutor;
    }


    @Override
    public E writeValue(ID id, Supplier<E> supplier, boolean cacheValueAtOnce) {
        try {
            return valueCache.writeValue(id, supplier, cacheValueAtOnce);
        } finally {
            if (cacheValueAtOnce) {
                Try.run(() -> hotFlagSetter.accept(hotFlagCacheKey(id)));
            } else {
                Try.run(() -> singleKeyCleaner.accept(hotFlagCacheKey(id)));
            }
        }
    }

    @Override
    public Map<ID, E> writeBatchValue(Supplier<Map<ID, E>> supplier, boolean cacheValueAtOnce) {
        Map<ID, E> writeValues = new HashMap<>();
        try {
            return valueCache.writeBatchValue(() -> {
                Map<ID, E> values = supplier.get();
                writeValues.putAll(values);
                return values;
            }, cacheValueAtOnce);
        } finally {
            Try.run(() -> {
                if (!writeValues.isEmpty()) {
                    if (cacheValueAtOnce) {
                        hotFlagMultiSetter.accept(writeValues.keySet().stream().map(this::hotFlagCacheKey).collect(Collectors.toList()));
                    } else {
                        multiKeyCleaner.accept(writeValues.keySet().stream().map(this::hotFlagCacheKey).collect(Collectors.toList()));
                    }
                }
            });
        }
    }

    /**
     * 注意这个场景不太适合返回的数据会返Optional.empty()的场景
     *
     * @param id
     * @param supplier
     * @return
     */
    @Override
    public Optional<E> readValue(ID id, Supplier<Optional<E>> supplier) {

        AtomicBoolean hitCache = new AtomicBoolean(true);
        Optional<E> loadedValue = valueCache.readValue(id, new Supplier<Optional<E>>() {
            @Override
            public Optional<E> get() {
                hitCache.set(false);
                return Optional.empty();
            }
        });

        if (hitCache.get()) {
            String hotFlagCacheKey = hotFlagCacheKey(id);

            if (hotFlagSetterIfAbsent.apply(hotFlagCacheKey)) {
                // try reload async
                Try.run(() -> {
                    reloadExecutor.submit(() -> {
                        Optional<E> reloadValue = supplier.get();
                        reloadValue.ifPresent(e -> valueCache.updateCache(id, e));
                    });
                }).onFailure(t -> {
                    log.error("Fail to reload the value, when hot flag expired, id={}, value={}", id, loadedValue.get(), t);
                });
            }
        } else {
            // hitCache==false; 如果是首次load该key，补偿一个flag，后续理论上都不该出现
            Try.run(() -> hotFlagSetter.accept(hotFlagCacheKey(id)));
        }

        return loadedValue;


    }


    @Override
    public Map<ID, E> readValueByIds(List<ID> ids, Function<List<ID>, Map<ID, E>> valueGetter) {

        Set<ID> idsNotHitCache = new HashSet<>();

        Set<ID> idsHitCache = new HashSet<>(ids);

        Map<ID, E> loadedValues = valueCache.readValueByIds(ids, absentIds -> {
            idsNotHitCache.addAll(absentIds);
            idsHitCache.removeAll(absentIds);

            return valueGetter.apply(absentIds);
        });


        if (!CollectionUtils.isEmpty(idsHitCache)) {

            // 缓存命中，则检查flag

            List<ID> idsNeedReload = idsHitCache.stream().filter(hitCacheId -> {
                // cas lock 成功， 需要reload
                return hotFlagSetterIfAbsent.apply(hotFlagCacheKey(hitCacheId));
            }).collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(idsNeedReload)) {
                Try.run(() -> {
                    reloadExecutor.submit(() -> {
                        Map<ID, E> reloadValues = valueGetter.apply(idsNeedReload);
                        valueCache.updateBatchCache(reloadValues);
                    });
                }).onFailure(t -> {
                    log.error("Fail to reload the batch value, when hot flag expired, id={}, ", idsNeedReload, t);
                });
            }
        }
        if (!CollectionUtils.isEmpty(idsNotHitCache)) {
            // 不命中缓存， 完全从数据库load的，刷新一下flag
            Try.run(() -> hotFlagMultiSetter.accept(idsNotHitCache.stream().map(this::hotFlagCacheKey).collect(Collectors.toList())));
        }

        return loadedValues;

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
        try {
            valueCache.cleanCacheById(cacheKey);
        } finally {
            Try.run(() -> singleKeyCleaner.accept(hotFlagCacheKey(cacheKey)));
        }

    }

    @Override
    public void cleanCacheByIds(List<ID> cacheKey) {
        try {
            valueCache.cleanCacheByIds(cacheKey);
        } finally {
            Try.run(() -> multiKeyCleaner.accept(cacheKey.stream().map(this::hotFlagCacheKey).distinct().collect(Collectors.toList())));
        }
    }

    @Override
    public void updateCache(ID id, E value) {
        try {
            valueCache.updateCache(id, value);
        } finally {
            Try.run(() -> hotFlagSetter.accept(hotFlagCacheKey(id)));
        }

    }

    @Override
    public void updateBatchCache(Map<ID, E> values) {

        try {
            valueCache.updateBatchCache(values);
        } finally {
            Try.run(() -> hotFlagMultiSetter.accept(values.keySet().stream().map(this::hotFlagCacheKey).distinct().collect(Collectors.toList())));
        }
    }


    private String hotFlagCacheKey(ID id) {
        return String.format(hotFlagKeyTemplate, cacheKeyIdGetter.apply(id));
    }
}
