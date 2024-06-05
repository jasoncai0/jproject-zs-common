package com.jproject.zs.common.ratelimit;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import com.jproject.zs.common.common.util.JsonUtils;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.*;

@Component
public class LimitMapInit implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(LimitMapInit.class);
    // key:requestURI, value:guava_rateLimiter
    public static Map<String, RateLimiter> limiterMap = Maps.newConcurrentMap();

    @Autowired
    @Lazy
    private RequestMappingHandlerMapping handlerMapping;

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> mappingMethod = handlerMapping.getHandlerMethods();

        Try.run(() -> {
            Optional.ofNullable(mappingMethod).orElse(Maps.newHashMap()).entrySet().stream()
                    .filter(entryFilter -> AnnotatedElementUtils.hasAnnotation(entryFilter.getValue().getMethod(), JRateLimiter.class))
                    .forEach(entry -> {
                        HandlerMethod handlerMethod = entry.getValue();
                        Method method = handlerMethod.getMethod();

                        JRateLimiter permits = method.getAnnotation(JRateLimiter.class);

                        // restful风格request不适用
                        entry.getKey().getPatternsCondition().getPatterns()
                                .stream().filter(e -> !e.contains("{"))
                                .forEach(uri -> {
                                    if (uri.startsWith("//")) {
                                        uri = uri.substring(1);
                                    }

                                    limiterMap.put(uri, RateLimiter.create(permits.permitsPerSecond()));
                                });

                        log.info("inited_limitMap success size : {}, {}", limiterMap.size(), JsonUtils.toJson(limiterMap));
                    });
        }).onFailure(e -> {
            log.error("inited_limitMap error", e);
        });
    }
}
