package com.jproject.zs.common.http.hystrix;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ReflectUtil;
import com.jproject.zs.common.http.exception.JProjectHttpException;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import io.vavr.control.Try;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/3/4
 */
@Slf4j
@RequiredArgsConstructor
public class CallHystrixImpl<T> extends Call<T> {

    private final HystrixProperties hystrixProperties;

    // 默认predict永远false， 永远不以response的httpCode 做熔遁啊
    private final Predicate<Response<?>> hystrixBreakPredicate;



    @Override
    public Response<T> execute(){
        Response<T> rsp = doExecute();

        if (hystrixBreakPredicate.test(rsp)) {
            throw new JProjectHttpException(rsp.code(), Try.of(() -> rsp.errorBody().string()).getOrElse(""))
                    .setResponse(rsp);

        }
        return rsp;
    }


    private Response<T> doExecute() throws Exception {

        //10秒钟内至少10此请求失败，熔断器才发挥起作用
        //熔断器中断请求30秒后会进入半打开状态,放部分流量过去重试
        //错误率达到50开启熔断保护
        HystrixThreadPoolProperties.Setter threadPoolSetter = HystrixThreadPoolProperties.Setter()
                .withCoreSize(hystrixProperties.getThreads());

        if (hystrixProperties.getMaxThreads() != null) {
            threadPoolSetter
                    .withAllowMaximumSizeToDivergeFromCoreSize(true)
                    .withMaximumSize(hystrixProperties.getMaxThreads());
        }

        HystrixCommand.Setter setter = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(urlWithoutQuery()))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withCircuitBreakerRequestVolumeThreshold(hystrixProperties.getVolumn())//10秒钟内至少10此请求失败，熔断器才发挥起作用
                        .withCircuitBreakerSleepWindowInMilliseconds(hystrixProperties.getSleep())//熔断器中断请求30秒后会进入半打开状态,放部分流量过去重试
                        .withCircuitBreakerErrorThresholdPercentage(hystrixProperties.getErrorRate()) //错误率达到50开启熔断保护
                        .withFallbackIsolationSemaphoreMaxConcurrentRequests(hystrixProperties.getFallbackMaxConcurrency())
                        .withExecutionTimeoutEnabled(false)) // 使用http超时
                .andThreadPoolPropertiesDefaults(threadPoolSetter); //线程池为30


        HystrixCommand<Response<T>> command = new HystrixCommand<Response<T>>(setter) {

            @Override
            protected Response<T> run() throws Exception {
                return CallHystrixImpl.super.execute();
            }

            @SneakyThrows
            @Override
            protected Response<T> getFallback() {

                HttpUrl rawUrl = null;

                String fallbackUrl = null;
                try {

                    rawUrl = CallHystrixImpl.this.request().url();


                    fallbackUrl = selectHost(rawUrl.host(), hystrixProperties.getStandByHosts());

                    HttpUrl fallbackHttpUrl = HttpUrl.get(fallbackUrl);

                    Request newRequest = CallHystrixImpl.this.request().newBuilder()
                            .url(rawUrl.newBuilder().scheme(fallbackHttpUrl.scheme())
                                    .host(fallbackHttpUrl.host())
                                    .port(fallbackHttpUrl.port())
                                    .build())
                            .build();


                    Call<T> newCall = CallHystrixImpl.this.clone();

                    ReflectUtil.setFieldValue(newCall, "rawRequest", newRequest);


                    return newCall.execute(options);
                } catch (Throwable t) {

                    log.warn("Encounter failure in fallback, rawUrl={}, fallbackHost={} ",
                            Optional.ofNullable(rawUrl).orElse(null), fallbackUrl, t);
                    throw t;
                } finally {
                    log.info("Hit hystrix fallback, rawUrl={}, fallbackHost={}", Optional.ofNullable(rawUrl).orElse(null), fallbackUrl);
                }

            }


            private String selectHost(String baseHost, List<String> standByHosts) {
                if (CollectionUtil.isEmpty(standByHosts)) {
                    throw new UnsupportedOperationException("No fallback available.");
                }

                Collections.shuffle(standByHosts);
                return standByHosts.get(0);
            }
        };

        return command.execute();

    }


    private String urlWithoutQuery() {
        return this.request().url().newBuilder().query(null).fragment(null).build().toString();
    }




}
