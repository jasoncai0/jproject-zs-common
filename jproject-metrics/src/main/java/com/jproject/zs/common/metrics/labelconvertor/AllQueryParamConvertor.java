package com.jproject.zs.common.metrics.labelconvertor;

import com.jproject.zs.common.metrics.CollectionUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/10/24
 */
public class AllQueryParamConvertor implements Function<HttpServletRequest, Map<String, String>> {

    @Override
    public Map<String, String> apply(HttpServletRequest request) {
        Map<String, String[]> allParams = request.getParameterMap();
        if (CollectionUtils.isEmpty(allParams)) {
            return Collections.emptyMap();
        }
        return allParams.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey(),
                        entry -> Joiner.on(",").join(entry.getValue())));
    }


    public Function<HttpServletRequest, List<Map<String, String>>> toConvertorList() {
        return request -> {
            return Lists.newArrayList(apply(request));
        };
    }
}
