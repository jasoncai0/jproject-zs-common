package com.jproject.zs.common.http.interceptor;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import okio.Buffer;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/3/11
 */
@RequiredArgsConstructor
public class SignInterceptor implements Interceptor {

    private final String appId;

    private final String appKey;

    private final Gson gson = new Gson();

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request.Builder newBuilder = chain.request().newBuilder();

        MediaType rawMediaType = chain.request().body().contentType();


        String bodyText = Try.of(() -> {
            final Buffer buffer = new Buffer();
            if (chain.request().body() != null) {
                chain.request().body().writeTo(buffer);
            }
            return buffer.readUtf8();
        }).getOrElse("");

        Map<String, String> rawBody = Try.of(() -> {
            return gson.<Map<String, String>>fromJson(bodyText, new TypeToken<Map<String, String>>() {
            }.getType());
        }).getOrElse(new HashMap<>());

        HashMap<String, String> result = new HashMap<>(rawBody);

        if (!result.containsKey("app_id")) {
            result.put("app_id", appId);
        }

        result.put("sign", sign(result, appKey));

        return chain.proceed(newBuilder
                .post(RequestBody.create(rawMediaType, gson.toJson(result)))
                .build());
    }


    public String sign(Map<String, String> rawBody, String appKey) {

        String signRawText = rawBody.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((o1, o2) -> o1 + "&" + o2)
                .orElse("") + appKey;

        System.out.println(signRawText);


        return DigestUtils.md5Hex(signRawText);

    }


}
