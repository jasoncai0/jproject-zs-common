package com.jproject.zs.common.migration.kafka.config;

import com.jproject.zs.common.migration.kafka.config.KafkaClusterConfig.SubProperties;
import com.jproject.zs.common.migration.kafka.util.ReflectUtils;
import com.google.common.base.Splitter;
import io.vavr.control.Try;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.util.StringUtils;
import devmodule.component.databus.sub.DatabusSub;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/2/17
 */
@Slf4j
public class ConsumerCallbackHelper {


    public static List<KafkaMessageListenerContainer> kafkaListenerContainer(

            String listenerAndMethod,

            List<SubProperties> subPropertiesList,
            Map<String, MessageListener> messageListeners,
            ConsumerFactory consumerFactory
    ) {

        String onMessage = "onMessage";

        List<String> listenerWithMethod = Splitter.on("#").splitToList(listenerAndMethod);

        String listenerBeanName = listenerWithMethod.get(0);
        String method = listenerWithMethod.size() >= 2 ? listenerWithMethod.get(1) : onMessage;

        Object listener = messageListeners.get(listenerBeanName);

        Map<String, List<SubProperties>> groupBatchProperties = subPropertiesList.stream()
                .collect(Collectors.groupingBy(properties -> Optional.ofNullable(properties.getGroup()).orElse("")));

        return groupBatchProperties.entrySet().stream().map(entry -> {

            String[] consumeTopics = entry.getValue().stream().map(SubProperties::getKafkaTopic)
                    .collect(Collectors.toList())
                    .toArray(new String[0]);
            String groupId = entry.getKey();

            if (listener instanceof MessageListener && onMessage.equals(method)) {

                ContainerProperties containerProperties =
                        new ContainerProperties(consumeTopics);

                containerProperties.setMessageListener(listener);

                if (!StringUtils.isEmpty(groupId)) {
                    containerProperties.setGroupId(groupId);
                }

                return new KafkaMessageListenerContainer(consumerFactory, containerProperties);
            } else {

                Method onMessageMethod = ReflectUtils
                        .getPublicMethod(listener.getClass(), method, ConsumerRecord.class);

                assert onMessageMethod != null;
                if (onMessageMethod.isAnnotationPresent(KafkaListener.class)) {
                    log.warn("Method annotated with @KafkaListener, please check consume registry, bean={}",
                            listenerBeanName);
                }

                ContainerProperties containerProperties = new ContainerProperties(consumeTopics);

                containerProperties.setMessageListener(
                        (MessageListener) data -> Try.run(() -> onMessageMethod.invoke(listener, data)).onFailure(t -> {
                            log.error("Fail to handle kafka message callback, data={}", data, t);
                        }));

                if (!StringUtils.isEmpty(groupId)) {
                    containerProperties.setGroupId(groupId);
                }
                return new KafkaMessageListenerContainer(consumerFactory, containerProperties);
            }
        }).collect(Collectors.toList());


    }


    // 每个databusTopic都要有单独的databusSub
    public static void startSubscribe(
            DatabusSub databus,
            List<String> listenerAndMethods,
            SubProperties subProperties,
            Map<String, MessageListener> messageListeners
    ) {

        final String onMessage = "onMessage";

        List<Consumer<ConsumerRecord>> consumeCallbacks = new ArrayList<>();

        listenerAndMethods.stream().map(listenerAndMethod -> {
            List<String> listenerWithMethod = Splitter.on("#").splitToList(listenerAndMethod);

            String listenerBeanName = listenerWithMethod.get(0);
            String methodName = listenerWithMethod.size() >= 2 ? listenerWithMethod.get(1) : onMessage;

            Object messageListener = Optional.ofNullable(messageListeners.get(listenerBeanName))
                    .orElseThrow(() -> {
                        return new IllegalArgumentException(
                                String.format("No messagelistener called %s found, initialize the databus sub failed"
                                        , listenerAndMethod));
                    });

//            Consumer<ConsumerRecord> consumeCallback;

            if (messageListener instanceof MessageListener && onMessage.equals(methodName)) {
                return (Consumer<ConsumerRecord>) message -> ((MessageListener) messageListener).onMessage(message);
            } else {

                Method method = ReflectUtils.getPublicMethod(messageListener.getClass(), methodName,
                        ConsumerRecord.class);
                if (method == null) {
                    throw new IllegalArgumentException(
                            String.format("No method called %s found in bean %s, please check "
                                    + "kafka-migration config", methodName, listenerBeanName));
                }
                return (Consumer<ConsumerRecord>) message -> {
                    Try.run(() -> method.invoke(messageListener, message)).onFailure(t -> {
                        log.error("Fail to call onMessage method handle kafka message ", t);
                    });
                };
            }
        }).forEach(callback -> consumeCallbacks.add(callback));

        Thread consumeThread = new Thread(() -> {
            databus.sub(json -> {

                Try.run(() -> {

                    String jsonStr = json.toJSONString();

                    // 仍然装作是kafka消息保证上层兼容
                    ConsumerRecord<Object, String> message = new ConsumerRecord<>(
                            Optional.ofNullable(subProperties.getKafkaTopic())
                                    .orElse(subProperties.getDatabusTopic()), 0, 0, null, jsonStr);

                    consumeCallbacks.forEach(consumeCallback -> {
                        Try.run(() -> consumeCallback.accept(message))
                                .onFailure(t -> log.error("Fail to handle the consumed databus msg topic={}, msg={}",
                                        subProperties.getDatabusTopic(), json, t));
                    });

                }).onFailure(t -> {
                    log.error("Fail to handle the consumed databus msg, msg={}", json, t);
                });
            });


        }, subProperties.getDatabusTopic() + "Databus-Consume-Thread");
        consumeThread.setDaemon(true);
        consumeThread.start();
    }


}
