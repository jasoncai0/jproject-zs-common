package com.jproject.zs.common.snowflake;

import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/12/23
 */
@Slf4j
@RequiredArgsConstructor
public class RedisWorkIdDistributor {

    /**
     * 受不了糟糕的封装，先本地实现一遍
     */
    private final RedisOperations redisTemplate;

    /**
     * with serviceName, e.g. 'worker.id'
     */
    private final String workerIdScoreSetCacheKey;
    /**
     * exclusive, workerId max count
     */
    private final Integer maxWorkerId;

    private final Long workerIdGuardIntervalMillis;

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

    private Map<Integer, AtomicBoolean> workerIdGuardStartedMap = new HashMap<>();


    @PostConstruct
    public void init() throws InterruptedException {

        boolean existed = redisTemplate.hasKey(workerIdScoreSetCacheKey);

        if (existed) {
            log.info("ordered workerId already allocated, start to try acquire...lets go");
            return;
        }

        boolean lock = lock();

        if (!lock) {
            Thread.sleep(300);
            init();
            return;
        }

        try {
            List<Tuple2<Integer, Long>> zset = new ArrayList<>();

            for (int i = 0; i < maxWorkerId; i++) {
                zset.add(new Tuple2<>(i, System.currentTimeMillis()));
            }

            addToZSet(zset);
            log.info("Finish allocate workId zset");
        } finally {
            unlock();
        }

    }


    public Integer workerId() {

        boolean lock = lock();

        if (!lock) {
            Try.run(() -> Thread.sleep(1000));
            return workerId();
        }

        try {
            Optional<Set<Integer>> zset = Optional.ofNullable(redisTemplate.zsetRange(workerIdScoreSetCacheKey, 0, -1));

            if (!zset.isPresent()) {
                throw new RuntimeException("Fail to initialize RedisWorkIdDistributor, no zset value found");
            }

            Integer workerId = zset.get()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> {
                        return new RuntimeException("Fail to initialize RedisWorkIdDistributor, zset found, but wokerId not found");
                    });

            AtomicBoolean workerIdGuardStarted = workerIdGuardStartedMap.computeIfAbsent(workerId, k -> new AtomicBoolean(false));

            refreshWorkerIdScore(workerId);

            if (workerIdGuardStarted.compareAndSet(false, true)) {

                executorService.scheduleAtFixedRate(() -> {
                    refreshWorkerIdScore(workerId);
                }, workerIdGuardIntervalMillis, workerIdGuardIntervalMillis, TimeUnit.MILLISECONDS);
            }
            return workerId;
        } finally {
            unlock();
        }
    }


    private void addToZSet(List<Tuple2<Integer, Long>> valueWithScores) {

        redisTemplate.zsetAdd(workerIdScoreSetCacheKey, valueWithScores);
    }


    private void refreshWorkerIdScore(Integer workerId) {
        Boolean r = redisTemplate.zsetAdd(workerIdScoreSetCacheKey, workerId, System.currentTimeMillis());
        log.info("Success to refresh workerId score, key={}, workerId={}", workerIdScoreSetCacheKey, workerId);
    }


    private boolean lock() {

        String key = workerIdScoreSetCacheKey + ".lock";
        boolean locked = redisTemplate.setIfAbsent(key, 1);

        if (locked) {
            redisTemplate.expire(key, 30, TimeUnit.SECONDS);
        }

        return locked;
    }


    private void unlock() {
        String key = workerIdScoreSetCacheKey + ".lock";
        redisTemplate.delete(key);
    }

}
