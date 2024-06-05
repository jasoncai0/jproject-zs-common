package com.jproject.zs.common.cache.bloomfilter;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/1/5
 */
public interface RedisOpt {

    /***
     *
     * @param key
     * @param offset
     * @param value
     * @return 是否变更了
     */
    Boolean setbit(String key, long offset, boolean value);

    Boolean getbit(String key, long offset);

    Long ttl(String key);

    Long expire(String key, int seconds);

}
