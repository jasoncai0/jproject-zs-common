package com.jproject.zs.common.redis.migration.redisson.factory;

import com.jproject.zs.common.redis.migration.redis.broadcast.RedisOperationsBroadcast;
import com.jproject.zs.common.redis.migration.redisson.spi.RedisOperationRedissonCallback;
import io.vavr.Tuple2;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/17
 */
@NoArgsConstructor
@AllArgsConstructor
public class RedisOperationsBroadcastMockImpl extends RedisOperationsBroadcast {

    @Setter
    private RedisOperationRedissonCallback redisOperationRedissonCallback;

    @Override
    public void del(byte[] keyBytes) {
        redisOperationRedissonCallback.del(keyBytes);
    }

    @Override
    public void set(byte[] keyBytes, byte[] valueBytes, long expireSeconds) {
        redisOperationRedissonCallback.set(keyBytes, valueBytes, expireSeconds);
    }

    @Override
    public void setNx(byte[] keyBytes, byte[] valueBytes, long expireSeconds) {
        redisOperationRedissonCallback.setNx(keyBytes, valueBytes, expireSeconds);
    }

    @Override
    public void incr(byte[] keyBytes, long delta) {
        redisOperationRedissonCallback.incr(keyBytes, delta);
    }

    @Override
    public void sadd(byte[] keyBytes, byte[][] data, long expireSeconds) {
        redisOperationRedissonCallback.sadd(keyBytes, data, expireSeconds);
    }

    @Override
    public void srem(byte[] keyBytes, byte[][] data) {
        redisOperationRedissonCallback.srem(keyBytes, data);
    }

    @Override
    public void mset(Map<byte[], byte[]> dataMap) {
        redisOperationRedissonCallback.mset(dataMap);
    }

    @Override
    public void hset(byte[] keyBytes, byte[] hashKeyBytes, byte[] dataBytes, long expireSeconds) {
        redisOperationRedissonCallback.hset(keyBytes, hashKeyBytes, dataBytes, expireSeconds);
    }

    @Override
    public void hmset(byte[] keyBytes, Map<byte[], byte[]> dataMap, long expireSeconds) {
        redisOperationRedissonCallback.hmset(keyBytes, dataMap, expireSeconds);
    }

    @Override
    public void zadd(byte[] keyBytes, List<Tuple2<byte[], Double>> values, long expireSeconds) {
        redisOperationRedissonCallback.zadd(keyBytes, values, expireSeconds);
    }

    @Override
    public void hincr(byte[] keyBytes, byte[] hashKeyBytes, long delta) {
        redisOperationRedissonCallback.hincr(keyBytes, hashKeyBytes, delta);
    }

    @Override
    public void hdel(byte[] keyBytes, byte[] hashKeyBytes) {
        redisOperationRedissonCallback.hdel(keyBytes, hashKeyBytes);
    }

    @Override
    public void rpush(byte[] keyBytes, byte[] data) {
        redisOperationRedissonCallback.rpush(keyBytes, data);
    }

    @Override
    public void rpushAll(byte[] keyBytes, byte[][] data, long expireSeconds) {
        redisOperationRedissonCallback.rpushAll(keyBytes, data, expireSeconds);
    }

    @Override
    public void lpush(byte[] keyBytes, byte[][] data) {
        redisOperationRedissonCallback.lpush(keyBytes, data);
    }

    @Override
    public void rpop(byte[] keyBytes) {
        redisOperationRedissonCallback.rpop(keyBytes);
    }

    @Override
    public void lpop(byte[] keyBytes) {
        redisOperationRedissonCallback.lpop(keyBytes);
    }

    @Override
    public void lpushTrim(byte[] keyBytes, byte[] data, int limit) {
        redisOperationRedissonCallback.lpushTrim(keyBytes, data, limit);
    }

    @Override
    public void expire(byte[] keyBytes, long expireSeconds) {
        redisOperationRedissonCallback.expire(keyBytes, expireSeconds);
    }

    @Override
    public void mexpire(List<byte[]> keys, long expireSeconds) {
        redisOperationRedissonCallback.mexpire(keys, expireSeconds);
    }

    @Override
    public void numberSet(byte[] keyBytes, byte[] valueBytes, long expireSeconds) {
        redisOperationRedissonCallback.numberSet(keyBytes, valueBytes, expireSeconds);
    }
}
