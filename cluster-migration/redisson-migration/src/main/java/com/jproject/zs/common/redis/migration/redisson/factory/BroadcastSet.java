package com.jproject.zs.common.redis.migration.redisson.factory;

import com.jproject.zs.common.redis.migration.redis.config.RedisAsyncConfig;
import com.jproject.zs.common.redis.migration.redisson.spi.SetOperations;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.redisson.RedissonSet;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/4/14
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, staticName = "wrap")
public class BroadcastSet<V> {

    private String name;

    private RedissonSet<V> set;

    private SetOperations setOperations;

    private RedisAsyncConfig redisAsyncConfig;

    public boolean remove(V value) {
        return remove(value, redisAsyncConfig.isBroadcastEnabled());
    }

    public boolean add(V value) {
        return add(value, redisAsyncConfig.isBroadcastEnabled());
    }


    public boolean remove(V value, boolean broadcast) {
        return setOperations.remove(set, value, broadcast);
    }

    public boolean add(V value, boolean broadcast) {
        return setOperations.add(set, value, broadcast);
    }


    public Set<V> values() {
        return set.readAll();
    }

    public int size() {
        return set.size();
    }


}
