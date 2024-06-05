package com.jproject.zs.common.redis.migration.redisson.factory;

import com.jproject.zs.common.redis.migration.redis.config.RedisAsyncConfig;
import com.jproject.zs.common.redis.migration.redisson.spi.MapOperations;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.redisson.RedissonMap;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/14
 */
@Data
@AllArgsConstructor(access = AccessLevel.PACKAGE, staticName = "wrap")
public class BroadcastMap<K, V> {

    private String name;

    private RedissonMap<K, V> redissonMap;

    private MapOperations mapOperations;

    private RedisAsyncConfig redisAsyncConfig;

    public void delete() {
        delete(redisAsyncConfig.isBroadcastEnabled());
    }

    public void delete(boolean broadcast) {
        mapOperations.delete(redissonMap, broadcast);
    }


    public V remove(K field) {
        return remove(field, redisAsyncConfig.isBroadcastEnabled());
    }

    public V put(K field, V value) {
        return put(field, value, redisAsyncConfig.isBroadcastEnabled());
    }


    public void expire(long ttl, TimeUnit timeUnit) {
        expire(ttl, timeUnit, redisAsyncConfig.isBroadcastEnabled());
    }


    public V remove(K field, boolean broadcast) {
        return mapOperations.<K, V>remove(redissonMap, field, broadcast);
    }

    public V put(K field, V value, boolean broadcast) {
        return mapOperations.put(redissonMap, field, value, broadcast);
    }


    public void expire(long ttl, TimeUnit timeUnit, boolean broadcast) {
        mapOperations.expire(redissonMap, ttl, timeUnit, broadcast);
    }

    public V get(K key) {
        return redissonMap.get(key);
    }

    public Map<K, V> getAll(Set<K> keys) {
        return redissonMap.getAll(keys);
    }

    public Collection<V> values() {
        return redissonMap.values();
    }

    public boolean containsKey(K key) {
        return redissonMap.containsKey(key);
    }

    public int size() {
        return redissonMap.size();
    }


}
