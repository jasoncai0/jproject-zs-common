package com.jproject.zs.common.http.hystrix;

import okhttp3.OkHttpClient;
import devmodule.venus.logging.support.Level;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Predicate;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/3/4
 */
public class CallHystrixAdapterFactory extends CallAdapter.Factory {

    private final OkHttpClient okClient;
    private final BreakerClient breaker;
    private final AppSignatureProperties signatureProperties;
    private final Level logLevel;
    private final HystrixProperties hystrixProperties;
    private final Predicate<Response<?>> hystrixBreakPredicate;

    public CallHystrixAdapterFactory(OkHttpClient okClient, BreakerClient breaker,
                                         AppSignatureProperties signatureProperties, Level logLevel,
                                         HystrixProperties hystrixProperties, Predicate<Response<?>> hystrixBreakPredicate) {

        this.okClient = okClient;
        this.breaker = breaker;
        this.signatureProperties = signatureProperties;
        this.logLevel = logLevel;
        this.hystrixProperties = hystrixProperties;
        this.hystrixBreakPredicate = hystrixBreakPredicate;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    @Override
    public CallAdapter<?, CallHystrixImpl> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(returnType) != Call.class) {
            return null;
        }

        return new CallAdapter<Object, CallHystrixImpl>() {
            @Override
            public Type responseType() {
                return getParameterUpperBound(0, (ParameterizedType) returnType);
            }

            @Override
            public CallHystrixImpl adapt(Call<Object> call) {
                return new CallHystrixImpl(call.request(), annotations, okClient, breaker, retrofit.baseUrl(),
                        signatureProperties, retrofit.responseBodyConverter(responseType(), annotations),
                        logLevel, hystrixProperties, hystrixBreakPredicate);
            }

        };
    }
}
