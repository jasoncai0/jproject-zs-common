package com.jproject.zs.common.metrics.labelconvertor;

import io.vavr.Tuple2;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/10/21
 */
public class QueryParamConvertor implements Function<HttpServletRequest, Map<String, String>> {

    private List<String> parameters;

    public QueryParamConvertor(List<String> parameters) {
        this.parameters = parameters;
    }

    public QueryParamConvertor(String... parameters) {
        this.parameters = Arrays.asList(parameters.clone());
    }

    @Override
    public Map<String, String> apply(HttpServletRequest request) {
        return parameters.stream()
                .map(param -> new Tuple2<>(param, request.getParameter(param)))
                .filter(v -> v._2 != null)
                .collect(Collectors.toMap(t -> t._1, t -> t._2));
    }
}
