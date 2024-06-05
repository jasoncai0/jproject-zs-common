package com.jproject.zs.common.redis.migration.redis.spi;

import io.vavr.Tuple2;
import java.util.List;
import java.util.Map;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/12
 */
public interface RedisOperations {

    void del(byte[] keyBytes);

    void set(byte[] keyBytes, byte[] valueBytes, long expireSeconds);

    void setNx(byte[] keyBytes, byte[] valueBytes, long expireSeconds);


    void incr(byte[] keyBytes, long delta);

    void sadd(byte[] keyBytes, byte[][] data, long expireSeconds);


    void srem(byte[] keyBytes, byte[][] data);

    void mset(Map<byte[], byte[]> dataMap);

    void hset(byte[] keyBytes, byte[] hashKeyBytes, byte[] dataBytes, long expireSeconds);

    void hmset(byte[] keyBytes, Map<byte[], byte[]> dataMap, long expireSeconds);

    void zadd(byte[] keyBytes, List<Tuple2<byte[], Double>> values, long expireSeconds);

    void hincr(byte[] keyBytes, byte[] hashKeyBytes, long delta);

    void hdel(byte[] keyBytes, byte[] hashKeyBytes);

    void rpush(byte[] keyBytes, byte[] data);

    void rpushAll(byte[] keyBytes, byte[][] data, long expireSeconds);

    void lpush(byte[] keyBytes, byte[][] data);

    void rpop(byte[] keyBytes);

    void lpop(byte[] keyBytes);

    void lpushTrim(byte[] keyBytes, byte[] data, int limit);

    void expire(byte[] keyBytes, long expireSeconds);

    void mexpire(List<byte[]> keys, long expireSeconds);


    /**
     * redisson 扩展指令
     */
    void numberSet(byte[] keyBytes, byte[] valueBytes, long expireSeconds);
}
