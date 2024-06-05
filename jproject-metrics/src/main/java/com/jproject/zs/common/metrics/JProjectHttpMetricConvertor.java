package com.jproject.zs.common.metrics;

import com.jproject.zs.common.metrics.labelconvertor.AllQueryParamConvertor;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 其实不适合放在组件中，业务方可以根据需要自行定制
 *
 * @author caizhensheng
 * @desc
 * @date 2022/10/21
 */
public enum JProjectHttpMetricConvertor {

    /**
     * 提取所有参数
     */
    DEFAULT_CONVERTORS(new AllQueryParamConvertor().toConvertorList()),

    ;

    @Getter
    private final Function<HttpServletRequest, List<Map<String, String>>> convertors;


    JProjectHttpMetricConvertor(Function<HttpServletRequest, List<Map<String, String>>> convertor) {
        this.convertors = convertor;
    }
}
