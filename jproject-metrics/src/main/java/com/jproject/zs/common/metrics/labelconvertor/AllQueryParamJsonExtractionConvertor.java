package com.jproject.zs.common.metrics.labelconvertor;

import com.jproject.zs.common.metrics.JsonUtils;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/10/24
 */
@RequiredArgsConstructor
public class AllQueryParamJsonExtractionConvertor extends AllQueryParamConvertor {

    private final String jsonFields;


    @Override
    public Map<String, String> apply(HttpServletRequest request) {
        Map<String, String> rawMetricMap = super.apply(request);

        if (rawMetricMap == null || !rawMetricMap.containsKey(jsonFields)) {
            return rawMetricMap;
        }

        return Try.of(() -> {
            Map<String, String> result = new HashMap<>(rawMetricMap);


            result.putAll(JsonUtils.readToMap(rawMetricMap.get(jsonFields))
                    .entrySet()
                    .stream()
                    .collect(Collectors
                            .toMap(entry -> entry.getKey(), entry -> String.valueOf(entry.getValue()))));

            return result;

        }).getOrElse(rawMetricMap);

    }
}
