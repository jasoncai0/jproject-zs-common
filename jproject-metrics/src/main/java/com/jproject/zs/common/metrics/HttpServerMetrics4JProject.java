package com.jproject.zs.common.metrics;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.vavr.Tuple3;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author caizhensheng
 * @desc
 * @date 2022/10/21
 */
@Slf4j
public class HttpServerMetrics4JProject {


    private final static JProjectCollectorRegistry collectorRegistry = JProjectCollectorRegistry.myRegistry;


    public static volatile Tuple3<Counter, String, String[]> HTTP_SERVER_TOTAL;
    public static volatile Tuple3<Histogram, String, String[]> HTTP_SERVER_DURATION;
    public static volatile Tuple3<Counter, String, String[]> HTTP_SERVER_CODE;
    private static String NAME_SPACE = "http_server";
    private static String SUBSYSTEM = "_requests";

    public static void stats(String path, Long cost,
                             int code,
                             List<String> fieldProjection,
                             Map<String, String> extra, boolean durationMetricEnabled) {

        String[] filedValues = fieldProjection.stream()
                .map(field -> Optional.ofNullable(extra.get(field)).orElse(""))
                .collect(Collectors.toList())
                .toArray(new String[0]);

        incr(getHttpServerTotalCounter(fieldProjection), join(new String[]{path}, filedValues));


        if (durationMetricEnabled) {
            observe(getHttpServerDuration(fieldProjection), join(new String[]{path}, filedValues), (double) cost);

        }

        incr(getHttpServerCodeCounter(fieldProjection), join(new String[]{path, String.valueOf(code)}, filedValues));
    }

    private static void incr(Tuple3<Counter, String, String[]> counterStringTuple3, String[] labelsValues) {

        Try.run(() -> {
            collectorRegistry.incr(counterStringTuple3._2, counterStringTuple3._3, labelsValues);
        }).onFailure(t -> {
            log.warn("Fail to update hot collector, name={} labels={}, values={}", counterStringTuple3._2, counterStringTuple3._3, labelsValues, t);
        });
        counterStringTuple3._1.labels(labelsValues).inc();
    }

    private static void observe(Tuple3<Histogram, String, String[]> counterStringTuple3, String[] labelsValues, double amt) {
        Try.run(() -> {
            collectorRegistry.incr(counterStringTuple3._2, counterStringTuple3._3, labelsValues);
        }).onFailure(t -> {
            log.warn("Fail to update hot collector, name={} labels={}, values={}", counterStringTuple3._2, counterStringTuple3._3, labelsValues, t);
        });
        counterStringTuple3._1.labels(labelsValues).observe(amt);
    }


    protected static Tuple3<Counter, String, String[]> getHttpServerTotalCounter(List<String> fields) {

        if (HTTP_SERVER_TOTAL == null) {
            synchronized (HttpServerMetrics4JProject.class) {
                if (HTTP_SERVER_TOTAL == null) {
                    Counter counter = Counter.build()
                            .namespace(NAME_SPACE)
                            .subsystem(SUBSYSTEM)
                            .name("total")
                            .help("http server counter")
                            .labelNames(join(new String[]{"path"}, fields.toArray(new String[0])))
                            .create()
                            .register(JProjectCollectorRegistry.myRegistry);


                    HTTP_SERVER_TOTAL = new Tuple3<>(counter, counter.describe().get(0).name, join(new String[]{"path"}, fields.toArray(new String[0])));
                }
            }
        }

        return HTTP_SERVER_TOTAL;

    }

    private static Tuple3<Histogram, String, String[]> getHttpServerDuration(List<String> fields) {
        if (HTTP_SERVER_DURATION == null) {
            synchronized (HttpServerMetrics4JProject.class) {
                if (HTTP_SERVER_DURATION == null) {
                    Histogram histogram = Histogram.build()
                            .namespace(NAME_SPACE)
                            .subsystem(SUBSYSTEM)
                            .name("duration_ms")
                            .help("http server histogram")
                            .labelNames(join(new String[]{"path"}, fields.toArray(new String[0])))
                            .buckets(new double[]{5.0D, 10.0D, 25.0D, 50.0D, 100.0D, 250.0D, 500.0D, 1000.0D, 2500.0D, 5000.0D, 10000.0D})
                            .create()
                            .register(JProjectCollectorRegistry.myRegistry);

                    HTTP_SERVER_DURATION = new Tuple3<>(histogram, histogram.describe().get(0).name, join(new String[]{"path"}, fields.toArray(new String[0])));
                }
            }
        }
        return HTTP_SERVER_DURATION;
    }

    protected static Tuple3<Counter, String, String[]> getHttpServerCodeCounter(List<String> fields) {
        if (HTTP_SERVER_CODE == null) {
            synchronized (HttpServerMetrics4JProject.class) {
                if (HTTP_SERVER_CODE == null) {
                    Counter counter = Counter.build()
                            .namespace(NAME_SPACE)
                            .subsystem(SUBSYSTEM)
                            .name("code_total")
                            .help("http server code")
                            .labelNames(join(new String[]{"path", "code"}, fields.toArray(new String[0])))
                            .create()
                            .register(JProjectCollectorRegistry.myRegistry);

                    HTTP_SERVER_CODE = new Tuple3<>(counter, counter.describe().get(0).name, join(new String[]{"path"}, fields.toArray(new String[0])));
                }
            }

        }
        return HTTP_SERVER_CODE;
    }


    protected static String[] join(String[] left, String[] right) {
        String[] result = new String[left.length + right.length];

        System.arraycopy(left, 0, result, 0, left.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }


}
