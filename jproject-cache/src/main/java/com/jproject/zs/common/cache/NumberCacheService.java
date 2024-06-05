package com.jproject.zs.common.cache;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/5/11
 */
public interface NumberCacheService<ID> {


    Optional<Long> readValue(ID id, Supplier<Optional<Long>> valueGetter);

    void updateValue(ID id, Long value);

    /**
     * 如果不存在则null，  如果存在则在与那里的基础上+1
     *
     * @param id
     * @return
     **/
    Optional<Long> incr(ID id, long delta);


    void cleanById(ID id);

}
