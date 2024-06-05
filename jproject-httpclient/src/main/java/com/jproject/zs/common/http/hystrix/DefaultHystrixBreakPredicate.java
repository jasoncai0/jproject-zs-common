package com.jproject.zs.common.http.hystrix;

import retrofit2.Response;

import java.util.function.Predicate;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/3/8
 */
public class DefaultHystrixBreakPredicate implements Predicate<Response<?>> {
    
    @Override
    public boolean test(Response<?> response) {
        return false;
    }
    
    
}
