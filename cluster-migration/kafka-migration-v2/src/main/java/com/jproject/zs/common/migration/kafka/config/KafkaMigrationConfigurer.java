package com.jproject.zs.common.migration.kafka.config;

import com.jproject.zs.common.migration.kafka.config.KafkaClusterConfig.ClusterConfig;
import com.jproject.zs.common.migration.kafka.config.KafkaClusterConfig.DatabusType;
import com.jproject.zs.common.migration.kafka.config.KafkaClusterConfig.PubProperties;
import com.jproject.zs.common.migration.kafka.config.KafkaClusterConfig.SubProperties;
import com.jproject.zs.common.migration.kafka.facade.Databus2KafkaTemplateFacade;
import com.jproject.zs.common.migration.kafka.proxy.CrossClusterKafkaTemplate;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.util.CollectionUtils;
import devmodule.component.databus.pub.DatabusPub;
import devmodule.component.databus.sub.DatabusSub;

/**
 * 使用方式即为import该configurer
 *
 * @author caizhensheng
 * @desc
 * @date 2023/1/30
 */
@Slf4j
@Configuration
public class KafkaMigrationConfigurer implements SmartLifecycle {


    @Autowired
    private MessageListenerContainerHolder holder;

    /**
     * @param clusterConfig
     * @return
     */
    @Bean
    public CrossClusterKafkaTemplate crossClusterKafkaTemplate(
            @Autowired KafkaClusterConfig clusterConfig,
            ApplicationContext applicationContext) {

        /*
         * topic-> zone-> template
         */
        Map<String, Map<String, KafkaOperations<?, ?>>> templates = new HashMap<>();

        Map<String, Map<String, KafkaOperations<?, ?>>> kafkaProducers = createKafkaProducers(
                clusterConfig, applicationContext.getBeansOfType(ProducerFactory.class));

        Map<String, Map<String, KafkaOperations<?, ?>>> databusProducers = createDatabusProducers(clusterConfig);

        addAllKafkaOperations(templates, kafkaProducers);
        addAllKafkaOperations(templates, databusProducers);

        return new CrossClusterKafkaTemplate(templates, clusterConfig);

    }

    private void addAllKafkaOperations(Map<String, Map<String, KafkaOperations<?, ?>>> collection,
            Map<String, Map<String, KafkaOperations<?, ?>>> add) {

        add.forEach((topic, zones) -> zones.forEach((zone, template) -> {
            Map<String, KafkaOperations<?, ?>> zoneCollection = collection
                    .computeIfAbsent(topic, k -> new HashMap<>());
            zoneCollection.put(zone, template);
        }));
    }


    private Map<String, Map<String, KafkaOperations<?, ?>>> createKafkaProducers(
            KafkaClusterConfig clusterConfig,
            Map<String, ProducerFactory> existedKafkaProducerFactoryBeans) {
        /*
         * topic-> zone-> template
         */
        Map<ClusterConfig, Tuple2<ProducerFactory<?, ?>, KafkaOperations<?, ?>>> kafkaProducerFactories = clusterConfig
                .getClusterConfigs()
                .stream()
                .filter(config -> config.getType() == DatabusType.kafka)
                .filter(config -> {
                    return !CollectionUtils.isEmpty(config.getPublishProperties());
                })
                .collect(Collectors.toMap(config -> config, config -> {

                    String producerFactoryBeanName = config.getProducerFactoryBean();

                    if (producerFactoryBeanName != null && !producerFactoryBeanName.isEmpty()) {

                        ProducerFactory<?, ?> bean = Optional.ofNullable(existedKafkaProducerFactoryBeans)
                                .map(beans -> beans.get(producerFactoryBeanName))
                                .orElseThrow(() -> new IllegalArgumentException("No suitable producer factory found, "
                                        + " bean called: " + producerFactoryBeanName));

                        return new Tuple2<>(bean, new KafkaTemplate<>(bean,
                                Optional.ofNullable(config.getProducerAutoFlash()).orElse(true)));

                    }

                    DefaultKafkaProducerFactory<Object, Object> producerFactory = new DefaultKafkaProducerFactory<>(
                            config.getBootstrapConfig());

                    return new Tuple2<>(producerFactory, new KafkaTemplate<>(producerFactory,
                            Optional.ofNullable(config.getProducerAutoFlash()).orElse(true))
                    );
                }, (o1, o2) -> o2));

        /*
         * kafka-topic-> zone-> template
         */
        Map<String, Map<String, KafkaOperations<?, ?>>> templates = new HashMap<>();

        kafkaProducerFactories.forEach((config, tuple) -> {
            // 所有的databus producer 配置
            config.getPublishProperties().forEach(databusTopic -> {
                // 一个databus的topic可能对应于多个kafka的topic，那么每个topic的kafkaTemplate都需要映射
                config.getPublishProperties().forEach(topic -> {
                    Map<String, KafkaOperations<?, ?>> zone2Template = templates
                            .computeIfAbsent(topic.getKafkaTopic(), k -> new HashMap<>());
                    zone2Template.put(config.getZone(), tuple._2);
                });
            });
        });

        return templates;
    }


    private Map<String, Map<String, KafkaOperations<?, ?>>> createDatabusProducers(KafkaClusterConfig clusterConfig) {
        Map<ClusterConfig, Databus2KafkaTemplateFacade> dataProducers = clusterConfig.getClusterConfigs()
                .stream()
                .filter(config -> config.getType() == DatabusType.databus)
                .filter(config -> !CollectionUtils.isEmpty(config.getPublishProperties()))
                .collect(Collectors.toMap(config -> config, pubClusterConfig -> {
                    // 同个databus集群下的多个topic producer 的参数配置

                    // 需要注意kafka和databus之间可能是多对多的映射
                    Map<String, Tuple2<PubProperties, DatabusPub>> kafkaTopicMappingToDatabusPub = new HashMap<>();

                    Map<String, DatabusPub> databusTopic2Pub = new HashMap<>();

                    pubClusterConfig.getPublishProperties().forEach(pubProperties -> {

                        DatabusPub publisher = databusTopic2Pub.computeIfAbsent(pubProperties.getDatabusTopic(), k -> {
                            return pubProperties.createPublisher(pubClusterConfig.getDatabusProperties());
                        });

                        kafkaTopicMappingToDatabusPub.computeIfAbsent(pubProperties.getKafkaTopic(), k -> {
                            return new Tuple2<>(pubProperties, publisher);
                        });

                    });

                    return new Databus2KafkaTemplateFacade(kafkaTopicMappingToDatabusPub,
                            new ThreadPoolExecutor(10, 10,
                                    0, TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<>(),
                                    new ThreadFactoryBuilder().setNameFormat("Databus-Publish-executor-").build(),
                                    new AbortPolicy()));


                }, (o1, o2) -> o2));
        /**
         * topic-> zone-> template
         */
        Map<String, Map<String, KafkaOperations<?, ?>>> templates = new HashMap<>();

        dataProducers.forEach((config, producer) -> {
            config.getPublishProperties().forEach(topic -> {
                Map<String, KafkaOperations<?, ?>> zone2Template = templates
                        .computeIfAbsent(topic.getKafkaTopic(), k -> new HashMap<>());
                zone2Template.put(config.getZone(), producer);
            });
        });
        return templates;
    }


    @Bean
    public Map<ClusterConfig, ConsumerFactory> consumerFactoryMap(
            @Autowired KafkaClusterConfig kafkaClusterConfig,
            @Autowired(required = false) @Qualifier("kafkaKeyDeserializer") Deserializer kafkaKeyDeserializer,
            @Autowired(required = false) @Qualifier("kafkaValueDeserializer") Deserializer kafkaValueDeserializer,
            ApplicationContext applicationContext
    ) {

        Map<String, ConsumerFactory> consumerFactoryBeans = applicationContext.getBeansOfType(ConsumerFactory.class);

        Map<ClusterConfig, ConsumerFactory> consumerFactories = kafkaClusterConfig.getClusterConfigs()
                .stream()
                .filter(config -> config.getType() == DatabusType.kafka)
                .filter(config -> {
                    return config.getMessageListenerConsumeProperties() != null
                            && !config.getMessageListenerConsumeProperties().isEmpty();
                })
                .collect(Collectors.toMap(clusterConfig -> clusterConfig, clusterConfig -> {

                    String consumerFactoryBean = clusterConfig.getConsumerFactoryBean();

                    if (consumerFactoryBean != null && !consumerFactoryBean.isEmpty()) {

                        return Optional.ofNullable(consumerFactoryBeans)
                                .map(beans -> beans.get(consumerFactoryBean))
                                .orElseThrow(() -> new IllegalArgumentException("No suitable consumer bean "
                                        + "factory found, called" + consumerFactoryBean));
                    }

                    return new DefaultKafkaConsumerFactory<>(clusterConfig.getBootstrapConfig(),
                            Optional.ofNullable(kafkaKeyDeserializer).orElse(new StringDeserializer()),
                            Optional.ofNullable(kafkaValueDeserializer).orElse(new StringDeserializer())
                    );
                }, (o1, o2) -> o2));

        return consumerFactories;

    }


    // 允许databus设置相同的databusTopic，可以用与合并多个kafka topic 到同一个databus topic
    @Bean
    public List<DatabusSub> databusMessageListeners(
            @Autowired KafkaClusterConfig kafkaClusterConfig,
            @Autowired(required = false) Map<String, MessageListener> messageListeners) {
        if (messageListeners == null || messageListeners.isEmpty()) {
            return new ArrayList<>();
        }

//        List<Tuple3<DatabusSub, String, SubProperties>> databusSubMap = new ArrayList<>();

        // zone,  topic , group
        Map<Tuple3<String, String, String>, Tuple3<DatabusSub, SubProperties, List<String>>>
                databusSubMap = new HashMap<>();

        kafkaClusterConfig
                .getClusterConfigs()
                .stream()
                .filter(config -> config.getType() == DatabusType.databus)
                .filter(clusterConfig -> !CollectionUtils.isEmpty(clusterConfig.getMessageListenerConsumeProperties()))
                .forEach(clusterConfig -> {
                    clusterConfig.getMessageListenerConsumeProperties()
                            .forEach((listener, consumerProperties) -> {

                                consumerProperties.forEach(consumerProperty -> {
                                    databusSubMap.computeIfAbsent(
                                            new Tuple3<>(clusterConfig.getZone(), consumerProperty.getDatabusTopic(),
                                                    consumerProperty.getGroup()),
                                            k -> {
                                                return new Tuple3<>(consumerProperty.createDatabusSubscriber(
                                                        clusterConfig.getDatabusProperties()),
                                                        consumerProperty,
                                                        new ArrayList<>());
                                            }
                                    )._3.add(listener);

                                });
                            });
                });

        databusSubMap.values().forEach((databusSub) -> {
            try {
                ConsumerCallbackHelper.startSubscribe(databusSub._1, databusSub._3, databusSub._2, messageListeners);
            } catch (Throwable t) {
                log.error("Fail to start databus subscription, databus={}", databusSub._2);
            }
        });

        return databusSubMap.values().stream()
                .map(databusSubStringTuple2 -> databusSubStringTuple2._1)
                .collect(Collectors.toList());


    }


    @Bean
    public MessageListenerContainerHolder kafkaMessageListenerContainers(
            @Autowired(required = false) Map<ClusterConfig, ConsumerFactory> consumerFactories,
            @Autowired(required = false) Map<String, MessageListener> messageListeners
    ) {

        if (messageListeners == null || messageListeners.isEmpty()) {
            return new MessageListenerContainerHolder(new ArrayList<>());
        }

        // register kafka message listeners
        return new MessageListenerContainerHolder(consumerFactories.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getType() == DatabusType.kafka)
                .flatMap(factoryEntry ->
                        factoryEntry.getKey().getMessageListenerConsumeProperties().entrySet().stream()
                                .flatMap(beanConsumeTopics -> {
                                    try {
                                        List<KafkaMessageListenerContainer> containers = ConsumerCallbackHelper
                                                .kafkaListenerContainer(
                                                        beanConsumeTopics.getKey(), beanConsumeTopics.getValue(),
                                                        messageListeners, factoryEntry.getValue()
                                                );

                                        return containers.stream();
                                    } catch (Throwable t) {
                                        log.error("Fail to initialize kafka consumer, topic={} ",
                                                beanConsumeTopics.getKey(), t);
                                        return null;
                                    }
                                }))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

    }

    @Override
    public void start() {
        holder.getListeners().forEach(Lifecycle::start);
    }

    @Override
    public void stop() {
        holder.getListeners().forEach(Lifecycle::stop);
    }

    @Override
    public boolean isRunning() {
        return holder.getListeners().stream()
                .map(Lifecycle::isRunning)
                .reduce((o1, o2) -> o1 & o2)
                .orElse(true);
    }

    @Data
    @AllArgsConstructor
    public static class MessageListenerContainerHolder {

        private List<MessageListenerContainer> listeners;
    }

}
