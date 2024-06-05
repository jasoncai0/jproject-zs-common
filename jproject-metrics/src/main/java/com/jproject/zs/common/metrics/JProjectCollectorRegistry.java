package com.jproject.zs.common.metrics;

import com.jproject.zs.common.metrics.HotCollectorRecorder.LabelNode;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/10/31
 */
@Slf4j
public class JProjectCollectorRegistry extends CollectorRegistry {


    public static final JProjectCollectorRegistry myRegistry = new JProjectCollectorRegistry(true);

    public static final CollectorRegistry combinedRegistry = new CollectorRegistry() {
        @Override
        public Enumeration<Collector.MetricFamilySamples> filteredMetricFamilySamples(Set<String> includedNames) {
            Enumeration<Collector.MetricFamilySamples> defaultIter = defaultRegistry.filteredMetricFamilySamples(includedNames);

            Enumeration<Collector.MetricFamilySamples> iter = myRegistry.filteredMetricFamilySamples(includedNames);
            return new Enumeration<Collector.MetricFamilySamples>() {
                @Override
                public boolean hasMoreElements() {
                    return defaultIter.hasMoreElements() || iter.hasMoreElements();
                }

                @Override
                public Collector.MetricFamilySamples nextElement() {
                    if (defaultIter.hasMoreElements()) {
                        return defaultIter.nextElement();
                    }
                    return iter.nextElement();
                }
            };
        }
    };

    /**
     * name-> (lable-> count)
     * http_server_requests_total -> (path="/api/comment/add",id="7",rank_type="")
     */
    @Setter
    private HotCollectorRecorder hotCollectorRecorder = new HotCollectorRecorder();


    public JProjectCollectorRegistry() {
        this(false);
    }

    public JProjectCollectorRegistry(boolean autoDescribe) {
        super(autoDescribe);

    }

    public void incr(String collectorName, String[] labelsName, String[] labelValues) {
        hotCollectorRecorder.incr(collectorName, labelsName, labelValues);
    }


    @Override
    public Enumeration<Collector.MetricFamilySamples> filteredMetricFamilySamples(Set<String> includedNames) {

        Enumeration<Collector.MetricFamilySamples> result = super.filteredMetricFamilySamples(includedNames);


        Map<String, Set<LabelNode>> snapshot = hotCollectorRecorder.collectorsWithHottestLabel();


        return new Enumeration<Collector.MetricFamilySamples>() {
            @Override
            public boolean hasMoreElements() {
                return result.hasMoreElements();
            }

            @Override
            public Collector.MetricFamilySamples nextElement() {
                Collector.MetricFamilySamples next = result.nextElement();

                Set<LabelNode> limitLabels = Optional.ofNullable(snapshot.get(next.name))
                        .orElse(new HashSet<>());

                if (CollectionUtils.isEmpty(next.samples)) {
                    return next;
                }

                // 所有的sample

                List<Collector.MetricFamilySamples.Sample> filteredSamples = next.samples.stream().filter(sample -> {
                    return limitLabels.contains(new LabelNode(sample.labelNames.toArray(new String[0]), sample.labelValues.toArray(new String[0])));
                }).collect(Collectors.toList());

                return new Collector.MetricFamilySamples(next.name, next.type, next.help, filteredSamples);

            }
        };
    }


}
