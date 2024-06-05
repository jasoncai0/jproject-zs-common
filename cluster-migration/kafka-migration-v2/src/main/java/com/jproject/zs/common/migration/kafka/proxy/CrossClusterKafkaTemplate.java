package com.jproject.zs.common.migration.kafka.proxy;

import com.jproject.zs.common.migration.kafka.config.KafkaClusterConfig;
import com.jproject.zs.common.migration.kafka.config.KafkaClusterConfig.ProducerStrategy;
import com.jproject.zs.common.migration.kafka.config.KafkaClusterConfig.ProducerStrategy.ZoneGreyConfig;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 所有具有写操作的额template的合集，使用grey和default配置进行分流；
 *
 * @author caizhensheng
 * @desc
 * @date 2023/1/30
 */
@Slf4j
@RequiredArgsConstructor
public class CrossClusterKafkaTemplate<K, V> implements KafkaOperations<K, V> {

    /**
     * topic-> zone-> template
     */
    private final Map<String, Map<String, KafkaOperations<K, V>>> topicZoneTemplate;

    private final KafkaClusterConfig migrationGreyConfig;

    private Random random = new Random();

    private int mod = 10000;

    @PostConstruct
    public void checkGreyConfig() {

    }


    @Override
    public ListenableFuture<SendResult<K, V>> send(String topic, K key, V data) {
        return chooseProducerTemplate(topic, Optional.ofNullable(key).map(Object::hashCode).orElse(null))
                .send(topic, key, data);
    }

    @Override
    public ListenableFuture<SendResult<K, V>> send(String topic, Integer partition, K key, V data) {
        return chooseProducerTemplate(topic, partition)
                .send(topic, partition, key, data);
    }

    @Override
    public ListenableFuture<SendResult<K, V>> send(String topic, Integer partition, Long timestamp, K key, V data) {
        return chooseProducerTemplate(topic, partition)
                .send(topic, partition, timestamp, key, data);
    }

    @Override
    public ListenableFuture<SendResult<K, V>> send(ProducerRecord<K, V> record) {
        return chooseProducerTemplate(record.topic(), record.partition())
                .send(record);
    }


    @Override
    public void flush() {
        // flush all
        topicZoneTemplate.forEach((topic, zones) -> {
            zones.forEach((zone, template) -> template.flush());
        });
    }

    @Override
    public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets) {
        throw new UnsupportedOperationException("executeInTransaction unsupported");
    }

    @Override
    public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets, String consumerGroupId) {
        throw new UnsupportedOperationException("executeInTransaction unsupported");
    }


    @Override
    public ListenableFuture<SendResult<K, V>> sendDefault(V data) {
        throw new UnsupportedOperationException("topic not determined");
    }

    @Override
    public ListenableFuture<SendResult<K, V>> sendDefault(K key, V data) {
        throw new UnsupportedOperationException("topic not determined");
    }

    @Override
    public ListenableFuture<SendResult<K, V>> sendDefault(Integer partition, K key, V data) {
        throw new UnsupportedOperationException("topic not determined");
    }

    @Override
    public ListenableFuture<SendResult<K, V>> sendDefault(Integer partition, Long timestamp, K key, V data) {
        throw new UnsupportedOperationException("topic not determined");
    }


    @Override
    public ListenableFuture<SendResult<K, V>> send(String topic, V data) {
        return chooseProducerTemplate(topic, null).send(topic, data);

    }

    @Override
    public ListenableFuture<SendResult<K, V>> send(Message<?> message) {
        throw new UnsupportedOperationException("send message not supported");
    }

    @Override
    public List<PartitionInfo> partitionsFor(String topic) {
        throw new UnsupportedOperationException("list partitions not supported");
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() {
        throw new UnsupportedOperationException("metrics not supported");
    }

    @Override
    public <T> T execute(ProducerCallback<K, V, T> callback) {
        throw new UnsupportedOperationException("execute callback not supported");
    }

    @Override
    public <T> T executeInTransaction(OperationsCallback<K, V, T> callback) {
        throw new UnsupportedOperationException("executeInTransaction callback not supported");
    }


    private KafkaOperations<K, V> chooseProducerTemplate(String topic, Integer partitionKey) {

        List<ZoneGreyConfig> greyConfigs =
                Optional.ofNullable(migrationGreyConfig.getProducerStrategy())
                        .map(ProducerStrategy::getTopicGreyConfig)
                        .map(config -> config.get(topic))
                        .orElse(new ArrayList<>());

        String zone = Try.of(() -> {
            if (greyConfigs.isEmpty()) {
                String defaultZone = migrationGreyConfig.getProducerStrategy().getTopicProduceZoneDefault().get(topic);

                if (defaultZone == null) {
                    log.error(
                            "Fail to find default zone when grey and default config not provided");
                    throw new IllegalArgumentException(
                            String.format("cannot find suitable kafkaTemplate of topic=%s", topic));
                }
                return defaultZone;

            }

            // 当有灰度配置时，根据灰度配置进行分流，依赖partionkey的键；

            else {
                ZoneGreyConfig selectedGreyConfig = selectZoneByGreyAndPartitionKey(greyConfigs, partitionKey);
                return selectedGreyConfig.getZone();
            }
        }).get();

        // 当没有灰度配置时，只能使用默认区域进行
        return Optional.ofNullable(topicZoneTemplate.get(topic))
                .map(zoneMap -> zoneMap.get(zone))
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("cannot find target zone-topic  kafkaTemplate of topic=%s, zone=%s", topic,
                                zone)
                ));

    }


    private ZoneGreyConfig selectZoneByGreyAndPartitionKey(List<ZoneGreyConfig> greyConfigs, Integer partitionKey) {

        if (greyConfigs == null || greyConfigs.isEmpty()) {
            throw new UnsupportedOperationException("grey configs cannot be empty");
        }

        if (greyConfigs.size() == 1) {
            return greyConfigs.get(0);
        }

        int partitionHashCode = Optional.ofNullable(partitionKey)
                .map(Object::hashCode)
                .orElse(random.nextInt());

        int modValue = Try.of(() -> Math.abs(partitionHashCode)).getOrElse(0) % mod;

        int weightSum = greyConfigs.stream().map(grey -> grey.getGrey()).reduce(Integer::sum).orElse(0);

        AtomicInteger currentWeightSum = new AtomicInteger(0);

        return greyConfigs.stream()
                .filter(config -> {

                    currentWeightSum.addAndGet(config.getGrey());

                    long threshold = (long) currentWeightSum.get() * mod / weightSum;

                    return modValue < threshold;

                })
                .findFirst()
                .orElseGet(() -> {
                    log.error(
                            "Fail to hash to suitable zone by grey config, use first one, grey={}",
                            greyConfigs);
                    return greyConfigs.get(0);
                });


    }
}
