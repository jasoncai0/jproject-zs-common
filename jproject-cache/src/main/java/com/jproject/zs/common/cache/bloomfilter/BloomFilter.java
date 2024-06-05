package com.jproject.zs.common.cache.bloomfilter;

import java.util.Collection;

/**
 * 参考guava {@link com.google.common.hash.BloomFilter}
 *
 * @author caizhensheng
 * @desc
 * @date 2023/1/5
 */
public interface BloomFilter<T> {


    boolean putAll(Collection<T> items);

    /**
     * {@link com.google.common.hash.BloomFilter#put(Object)}
     *
     * @param item
     * @return 是否发生bit变更
     */
    boolean put(T item);

    /**
     * 注意根据bf的特性， 这里是<b>可能</b>
     * {@link com.google.common.hash.BloomFilter#mightContain(Object)}
     *
     * @param item
     * @return
     */
    boolean mightContain(T item);

}
