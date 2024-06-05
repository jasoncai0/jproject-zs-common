package com.jproject.zs.common.cache.bloomfilter;

import com.google.common.hash.Funnel;
import com.google.common.hash.Hashing;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/1/5
 */
public class RedisBloomFilter<T> implements BloomFilter<T> {


    /**
     * 查重对应的空间容量
     */
    public final long capacity;
    /**
     * 容错率
     * <p>
     * 容错率越低，占用空间越大
     */
    public final double errorRate;
    private final Funnel<T> funnel;
    private final String redisKey;
    private final RedisOpt redisOpt;
    private final long aliveTime;
    private int numHashFunctions;
    private int bitSize;

    public RedisBloomFilter(String redisKey, Funnel<T> funnel, long capacity, double errorRate, RedisOpt redisOpt) {
        this(redisKey, funnel, capacity, errorRate, redisOpt, -1L);
    }


    public RedisBloomFilter(String redisKey, Funnel<T> funnel, long capacity, double errorRate, RedisOpt redisOpt, long aliveTime) {

        if (funnel == null) {
            throw new RuntimeException("Funnel can not be null!");
        }
        this.redisKey = redisKey;
        this.funnel = funnel;
        this.capacity = capacity;
        this.errorRate = errorRate;
        bitSize = optimalNumOfBits(capacity, errorRate);
        numHashFunctions = optimalNumOfHashFunctions(capacity, bitSize);
        this.redisOpt = redisOpt;
        this.aliveTime = aliveTime;
    }


    @Override
    public boolean putAll(Collection<T> items) {

        if (items == null || items.isEmpty()) {
            return false;
        }

        boolean isNewKey = true;
        boolean isExists = true;

        Set<Integer> offsets = items.stream()
                .filter(Objects::nonNull)
                .map(this::murmurHashOffset)
                .flatMap(offset -> Arrays.stream(offset).boxed())
                .collect(Collectors.toSet());

        for (int i : offsets) {
            if (redisOpt.setbit(redisKey, i, true)) {
                isNewKey = false;
            } else {
                isExists = false;
            }
        }
        if (isNewKey) {
            setExpirationTime(redisKey);
        }

        return isExists;

    }


    @Override
    public boolean put(T item) {
        if (item == null) {
            return false;
        }
        boolean isNewKey = true;
        boolean isExists = true;
        int[] offset = this.murmurHashOffset(item);
        for (int i : offset) {

            if (redisOpt.setbit(redisKey, i, true)) {
                // 一开始就是1
                isNewKey = false;
            } else {
                // 一开始是0 ，后来设置成1， 所以之前肯定不存在
                isExists = false;
            }
        }
        if (isNewKey) {
            setExpirationTime(redisKey);
        }
        // 返回是否变更
        return isExists;
    }

    @Override
    public boolean mightContain(T item) {

        int[] offset = this.murmurHashOffset(item);
        for (int i : offset) {
            if (!redisOpt.getbit(redisKey, i)) {
                return false;
            }
        }
        return true;
    }


    private void setExpirationTime(String key) {
        if (aliveTime > 0 && redisOpt.ttl(key) == -1) {
            redisOpt.expire(key, (int) aliveTime);
        }
    }


    /**
     * 计算hash offset数组
     */
    private int[] murmurHashOffset(T value) {
        int[] offset = new int[numHashFunctions];

        long hash64 = Hashing.murmur3_128().hashObject(value, funnel).asLong();
        int hash1 = (int) hash64;
        int hash2 = (int) (hash64 >>> 32);
        for (int i = 1; i <= numHashFunctions; i++) {
            int nextHash = hash1 + i * hash2;
            if (nextHash < 0) {
                nextHash = ~nextHash;
            }
            offset[i - 1] = nextHash % bitSize;
        }

        return offset;
    }

    /**
     * 计算bit数组长度
     */
    private int optimalNumOfBits(long n, double p) {
        if (p == 0) {
            p = Double.MIN_VALUE;
        }
        return (int) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    /**
     * 计算hash方法执行次数
     */
    private int optimalNumOfHashFunctions(long n, long m) {
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }

}
