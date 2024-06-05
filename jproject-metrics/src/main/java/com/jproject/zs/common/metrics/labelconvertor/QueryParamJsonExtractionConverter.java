package com.jproject.zs.common.metrics.labelconvertor;

import com.jproject.zs.common.metrics.JsonUtils;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ?param='{"a":1,"b":2}'
 *
 * @author caizhensheng
 * @desc
 * @date 2022/10/21
 */
public class QueryParamJsonExtractionConverter extends QueryParamConvertor {

    private final String queryParamName;

    private final List<String> jsonFiledName;


    public QueryParamJsonExtractionConverter(String queryParamName, String... jsonFiledName) {
        super(queryParamName);
        this.queryParamName = queryParamName;
        this.jsonFiledName = Arrays.asList(jsonFiledName);
    }

    @Override
    public Map<String, String> apply(HttpServletRequest request) {
        Map<String, String> values = super.apply(request);

        if (values == null || !values.containsKey(queryParamName)) {
            return values;
        }

        return Try.of(() -> {
            return JsonUtils.readToMap(values.get(queryParamName))
                    .entrySet()
                    .stream()
                    .filter(entry -> jsonFiledName.contains(entry.getKey()))
                    .collect(Collectors
                            .toMap(entry -> entry.getKey(), entry -> String.valueOf(entry.getValue())));
        }).getOrElse(new HashMap<>());

    }
}
