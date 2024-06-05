package com.jproject.zs.common.cache;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/1/6
 */
@Slf4j
public class CacheMapMetricImpl<E, ID> implements CacheMap<E, ID> {

    private final CacheMap<E, ID> delegate;
    private final Long printIntervalMillis;
    private final String cacheName;
    private ScheduledExecutorService scheduledExecutorService;
    private AtomicBoolean printStartted = new AtomicBoolean(false);

    private AtomicLong totalQuery = new AtomicLong();
    private AtomicLong totalAbsents = new AtomicLong();

    private AtomicLong roundQuery = new AtomicLong();
    private AtomicLong roundAbsents = new AtomicLong();


    public CacheMapMetricImpl(CacheMap<E, ID> delegate, String cacheName, Long internalMillis) {
        this.delegate = delegate;
        this.cacheName = cacheName;
        this.printIntervalMillis = internalMillis;
        this.startPrintStatistics(cacheName, internalMillis);
    }

    public void startPrintStatistics(String cacheName, Long intervalMillis) {

        if (!printStartted.compareAndSet(false, true)) {
            return;
        }

        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }

        scheduledExecutorService.scheduleAtFixedRate(() -> {

            long queryOfThisRound = roundQuery.getAndSet(0);

            long absentsOfThisRound = roundAbsents.getAndSet(0);

            totalAbsents.addAndGet(absentsOfThisRound);
            totalQuery.addAndGet(queryOfThisRound);


            log.info("CacheKey={}, [interval/total] hit ratio=[{}/{}], absents=[{}/{}], total=[{}/{}],", cacheName,
                    (queryOfThisRound - absentsOfThisRound) * 1.0 / queryOfThisRound,
                    (totalQuery.get() - totalAbsents.get()) * 1.0 / totalQuery.get(),
                    absentsOfThisRound, totalAbsents.get(), queryOfThisRound, totalQuery.get()
            );
        }, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }


    @Override
    public E writeValue(ID id, Supplier<E> supplier, boolean cacheValueAtOnce) {
        return delegate.writeValue(id, supplier, cacheValueAtOnce);
    }

    @Override
    public Map<ID, E> writeBatchValue(Supplier<Map<ID, E>> supplier, boolean cacheValueAtOnce) {
        return delegate.writeBatchValue(supplier, cacheValueAtOnce);
    }

    @Override
    public Optional<E> readValue(ID id, Supplier<Optional<E>> supplier) {
        try {
            return delegate.readValue(id, () -> {
                roundAbsents.incrementAndGet();
                return supplier.get();
            });
        } finally {
            roundQuery.incrementAndGet();
        }
    }

    @Override
    public Map<ID, E> readValueByIds(List<ID> ids, Function<List<ID>, Map<ID, E>> valueGetter) {
        try {
            return delegate.readValueByIds(ids, absents -> {
                roundAbsents.addAndGet(absents.stream().distinct().count());
                return valueGetter.apply(absents);
            });
        } finally {
            roundQuery.addAndGet(ids.stream().distinct().count());
        }
    }

    @Override
    public Optional<E> readCacheOnly(ID id) {
        return delegate.readCacheOnly(id);
    }

    @Override
    public Map<ID, E> readCacheOnly(List<ID> ids) {
        return delegate.readCacheOnly(ids);
    }

    @Override
    public void cleanCacheById(ID cacheKey) {
        delegate.cleanCacheById(cacheKey);
    }

    @Override
    public void cleanCacheByIds(List<ID> cacheKey) {
        delegate.cleanCacheByIds(cacheKey);
    }

    @Override
    public void updateCache(ID id, E value) {
        delegate.updateCache(id, value);
    }

    @Override
    public void updateBatchCache(Map<ID, E> values) {
        delegate.updateBatchCache(values);
    }
}
