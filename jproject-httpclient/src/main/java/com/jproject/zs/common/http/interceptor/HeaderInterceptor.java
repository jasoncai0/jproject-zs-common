package com.jproject.zs.common.http.interceptor;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/3/10
 */
@Setter
@Accessors(chain = true)
@NoArgsConstructor
public class HeaderInterceptor implements Interceptor {

    private String caller;

    private String userAgent;

    private Supplier<String> clientIpGetter;

    private Supplier<String> requestIdGetter;

    /**
     * false override header,
     * true  add header
     */
    private boolean addHeader = false;

    public HeaderInterceptor(String caller, String userAgent,
                                        Supplier<String> clientIpGetter,
                                        Supplier<String> requestIdGetter) {
        this.caller = caller;
        this.userAgent = userAgent;
        this.clientIpGetter = clientIpGetter;
        this.requestIdGetter = requestIdGetter;
    }

    public HeaderInterceptor(String caller,
                                        String userAgent,
                                        Supplier<String> clientIpGetter,
                                        Supplier<String> requestIdGetter,
                                        boolean addHeader) {
        this.caller = caller;
        this.userAgent = userAgent;
        this.clientIpGetter = clientIpGetter;
        this.requestIdGetter = requestIdGetter;
        this.addHeader = addHeader;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request.Builder newBuilder = chain.request().newBuilder();

        BiConsumer<String, String> headerCallback;

        if (addHeader) {
            headerCallback = newBuilder::addHeader;
        } else {
            headerCallback = newBuilder::header;
        }

        if (StringUtils.isNotEmpty(userAgent)) {
            headerCallback.accept("User-Agent", userAgent);
        }

        String clientIp = Optional.ofNullable(clientIpGetter).map(Supplier::get).orElse(null);
        if (StringUtils.isNotEmpty(clientIp)) {
            headerCallback.accept("X-BACKEND-REAL-IP", clientIp);
        }

        String requestId = Optional.ofNullable(requestIdGetter).map(Supplier::get).orElse(null);

        if (StringUtils.isNotEmpty(requestId)) {
            headerCallback.accept("Request-Id", requestId);
        }

        if (StringUtils.isNotEmpty(caller)) {
            headerCallback.accept("caller", caller);
        }
        return chain.proceed(newBuilder.build());
    }
}
