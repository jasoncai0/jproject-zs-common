package com.jproject.zs.common.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/11/1
 */
public class JProjectMetricServlet extends MetricsServlet {


    public JProjectMetricServlet() {
        this(JProjectCollectorRegistry.combinedRegistry);
    }

    public JProjectMetricServlet(CollectorRegistry registry) {
        super(registry);
    }


}
