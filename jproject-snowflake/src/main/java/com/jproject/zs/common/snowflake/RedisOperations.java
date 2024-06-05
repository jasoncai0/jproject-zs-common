package com.jproject.zs.common.snowflake;

import io.vavr.Tuple2;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/6/5
 */
public interface RedisOperations {


    /**
     * 是否包含某key值 用于初始化
     * @param key
     * @return
     */
    <K>  boolean  hasKey(K key);


    /**
     * zsetAdd
     * @param key
     * @param value
     * @param score
     * @return
     * @param <K>
     * @param <V>
     */
   <K,V> Boolean zsetAdd(K key, V value, double score);

    /**
     * batch zsetAdd
     * @param key
     * @param valueWithScores
     * @return
     * @param <K>
     * @param <V>
     */
   <K,V> Boolean zsetAdd(K key, List<Tuple2<Integer, Long>> valueWithScores);


    /**
     * 获取zset
     * @param key
     * @param start
     * @param end
     * @return
     * @param <K>
     * @param <V>
     */
   <K, V> Set<V> zsetRange(K key, long start, long end);


    /**
     * cas
     * @param key
     * @param value
     * @return
     * @param <K>
     * @param <V>
     */
    <K, V>Boolean setIfAbsent(K key, V value);


    /**
     * expire
     * @param key
     * @param timeout
     * @param unit
     * @return
     * @param <K>
     */
    <K> Boolean expire(K key, final long timeout, final TimeUnit unit);

    /**
     * delete
     * @param key
     * @return
     * @param <K>
     */
    <K> Boolean delete(K key);
}
