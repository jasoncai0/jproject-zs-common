package com.jproject.zs.common.metrics;

import io.prometheus.client.Collector;
import io.vavr.control.Try;
import lombok.Data;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/11/1
 */
public class HotCollectorRecorder {

    /**
     * name-> (lable-> count)
     * http_server__requests_total -> (path="/api/comment/add",id="7",rank_type="")
     */
    private final Map<String, Map<LabelNode, AtomicLong>> hotLabelTimesCnt = new ConcurrentHashMap<>();
    private final AtomicLong lastPollTs = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastResetTs = new AtomicLong(System.currentTimeMillis());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private RefreshStrategy refreshStrategy = RefreshStrategy.REFRESH_AT_POLL_WITH_MIN_INTERVAL;
    private Integer refreshIntervalMillis = 5 * 60 * 1000;
    @Setter
    private Integer hotLabelLimit = 100;

    public HotCollectorRecorder(RefreshStrategy refreshStrategy,
                                Integer refreshIntervalMillis,
                                Integer hotLabelLimit) {
        this.refreshStrategy = refreshStrategy;
        this.refreshIntervalMillis = refreshIntervalMillis;
        this.hotLabelLimit = hotLabelLimit;
        this.init();
    }

    public HotCollectorRecorder() {
        this.init();
    }

    public void init() {

        switch (refreshStrategy) {
            case REFRESH_AT_FIX_RATE: {
                scheduler.scheduleAtFixedRate(this::resetTimes, refreshIntervalMillis, refreshIntervalMillis, TimeUnit.MILLISECONDS);
                break;
            }
            case REFRESH_AT_POLL:
            case REFRESH_AT_POLL_WITH_MIN_INTERVAL:
            default: {
                break;
            }
        }
    }

    public void incr(String collectorName, String[] labelsName, String[] labelValues) {
        Map<LabelNode, AtomicLong> labelCnt = hotLabelTimesCnt.computeIfAbsent(collectorName, k -> new ConcurrentHashMap<>());

        labelCnt.computeIfAbsent(new LabelNode(labelsName, labelValues), k -> new AtomicLong()).incrementAndGet();
    }

    public void resetTimes() {

        Map<String, Set<LabelNode>> hottestSnapshot = doCollectorsWithHottestLabel(false);

        hotLabelTimesCnt.forEach((collectorName, labelTimesMap) -> {

            Iterator<Map.Entry<LabelNode, AtomicLong>> iterator = labelTimesMap.entrySet().iterator();

            Set<LabelNode> hottestLabels = Optional.ofNullable(hottestSnapshot.get(collectorName))
                    .orElse(new HashSet<>());


            while (iterator.hasNext()) {

                Map.Entry<LabelNode, AtomicLong> labelNodeEntry = iterator.next();

                if (hottestLabels.contains(labelNodeEntry.getKey())) {
                    labelNodeEntry.getValue().set(0);
                } else {
                    iterator.remove();
                }
            }
        });
    }

    public Map<String, Set<LabelNode>> collectorsWithHottestLabel() {
        return doCollectorsWithHottestLabel(true);
    }

    private Map<String, Set<LabelNode>> doCollectorsWithHottestLabel(boolean refresh) {

        return Try.of(() -> {
            synchronized (hotLabelTimesCnt) {
                Map<String, Map<LabelNode, AtomicLong>> times = new HashMap<>(hotLabelTimesCnt);

                if (refresh) {
                    switch (refreshStrategy) {
                        case REFRESH_AT_POLL: {
                            resetTimes();
                            lastResetTs.set(System.currentTimeMillis());
                            break;
                        }// do-nothing
                        case REFRESH_AT_POLL_WITH_MIN_INTERVAL: {
                            if (lastResetTs.get() + refreshIntervalMillis < System.currentTimeMillis()) {
                                resetTimes();
                                lastResetTs.set(System.currentTimeMillis());
                            }
                            break;
                        }
                        case REFRESH_AT_FIX_RATE:
                        default: {
                            break;
                        }
                    }

                    lastPollTs.set(System.currentTimeMillis());
                }
                return times;

            }
        }).getOrElse(new HashMap<>()).entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> {

            Map<LabelNode, AtomicLong> labelTimes = entry.getValue();

            return labelTimes.entrySet().stream().sorted(new Comparator<Map.Entry<LabelNode, AtomicLong>>() {
                @Override
                public int compare(Map.Entry<LabelNode, AtomicLong> o1, Map.Entry<LabelNode, AtomicLong> o2) {
                    return Long.compare(o2.getValue().longValue(), o1.getValue().longValue());
                }
            }).limit(hotLabelLimit).map(e -> e.getKey()).collect(Collectors.toSet());

        }, (o1, o2) -> o2));
    }


    public enum RefreshStrategy {

        REFRESH_AT_POLL,

        REFRESH_AT_FIX_RATE,

        REFRESH_AT_POLL_WITH_MIN_INTERVAL

    }


    @Data
    public static class LabelNode {
        private String[] labelNames;

        private String[] labelValues;

        public LabelNode(String[] labelNames, String[] labelValues) {
            this.labelNames = labelNames;
            this.labelValues = labelValues;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LabelNode labelNode = (LabelNode) o;
            return Arrays.equals(labelNames, labelNode.labelNames) && Arrays.equals(labelValues, labelNode.labelValues);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(labelNames);
            result = 31 * result + Arrays.hashCode(labelValues);
            return result;
        }

        public boolean match(Collector.MetricFamilySamples next) {
            // 区别主要在于histogram bucket中添加label le
            Collector.MetricFamilySamples.Sample sample = next.samples.get(next.samples.size() - 1);

            return this.equals(new LabelNode(sample.labelNames.toArray(new String[0]), sample.labelValues.toArray(new String[]{})));
        }
    }

}
