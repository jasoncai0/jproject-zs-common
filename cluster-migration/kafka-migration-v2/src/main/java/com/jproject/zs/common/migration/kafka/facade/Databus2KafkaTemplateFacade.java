package com.jproject.zs.common.migration.kafka.facade;

import com.alibaba.fastjson.JSON;
import com.jproject.zs.common.migration.kafka.config.KafkaClusterConfig.PubProperties;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
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
import org.springframework.util.concurrent.SettableListenableFuture;
import devmodule.component.databus.pub.DatabusPub;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/2/2
 */
@Slf4j
@RequiredArgsConstructor
public class Databus2KafkaTemplateFacade<K, V> implements KafkaOperations<K, V> {

    /**
     * kafka topic -> databusPublisher
     */
    private final Map<String, Tuple2<PubProperties, DatabusPub>> databusPubs;

    private final ExecutorService databusPublishCallbackExecutor;

    @Override
    public ListenableFuture<SendResult<K, V>> send(String topic, V data) {
        return doSend(topic, null, data);
    }


    @Override
    public ListenableFuture<SendResult<K, V>> send(String topic, K key, V data) {
        return doSend(topic, toNullableKeyString(key), data);
    }

    @Override
    public ListenableFuture<SendResult<K, V>> send(String topic, Integer partition, K key, V data) {
        return doSend(topic, toNullableKeyString(key), data);
    }

    @Override
    public ListenableFuture<SendResult<K, V>> send(String topic, Integer partition, Long timestamp, K key, V data) {
        return doSend(topic, toNullableKeyString(key), data);
    }

    @Override
    public ListenableFuture<SendResult<K, V>> send(ProducerRecord<K, V> record) {
        return doSend(record.topic(), toNullableKeyString(record.key()), record.value());
    }


    private ListenableFuture<SendResult<K, V>> doSend(String topic, String pubKey, V data) {
        Tuple2<PubProperties, DatabusPub> publisher = selectPublisherByKafkaTopic(topic);

        String dataString = toJsonString(data);

        SettableListenableFuture<SendResult<K, V>> result = new SettableListenableFuture<>();

        CompletableFuture.runAsync(() -> {

            if (pubKey == null) {

                publisher._2.pub(dataString);
            } else {

                publisher._2.pub(pubKey, dataString);
            }

        }, databusPublishCallbackExecutor).whenComplete((r, t) -> {

            if (t != null) {
                result.setException(t);
                log.error("Fail to publish the databus data, topic={}, data={}", topic, data, t);
            }
            result.set(dummyResult(new ProducerRecord<>(topic, data)));
        });

        return result;
    }


    private String toJsonString(Object data) {

        return Try.of(() -> {
            if (data instanceof String) {
                return ((String) data);
            } else {
                return JSON.toJSONString(data);
            }

        }).getOrElseThrow(t -> {
            log.error("Fail to serialize the data, data={}", data, t);
            return new IllegalArgumentException("Fai lto serialize the data");
        });
    }


    private String toNullableKeyString(K key) {
        return Try.of(() -> {
            if (key == null) {
                return null;
            }
            return toJsonString(key);
        }).getOrNull();
    }

    @Override
    public void flush() {
        // do nothing
    }

    @Override
    public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets) {

    }

    @Override
    public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets, String consumerGroupId) {

    }

    private SendResult<K, V> dummyResult(ProducerRecord<K, V> producerRecord) {

        return new SendResult<>(producerRecord, null);
    }

    private Tuple2<PubProperties, DatabusPub> selectPublisherByKafkaTopic(String kafkaTopic) {
        return Optional.ofNullable(databusPubs.get(kafkaTopic))
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("No suitable databus publisher found, kafka topic=%s", kafkaTopic)));
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
        throw new UnsupportedOperationException("execute callback not supported");
    }
}
