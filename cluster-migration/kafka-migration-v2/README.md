# Quick Start

# 场景1. 使用kafka-migration进行

## springmvc项目


注入的bean类型调整为`CrossClusterKafkaTemplate`

```java

//KafkaTemplate kafkaTemplate
CrossClusterKafkaTemplate kafkaTemplate

```

此时对应的paladin配置中心需要有kafka-migration-json.json的配置键，其具体的参数格式如下：

```json
{
   "clusterConfigs": [
      {
         "zone": "gz",
         "type": "kafka",
         "bootstrapConfig": {
            "bootstrap.servers": "172.16.38.192:9090",
            "retries": "10",
            "batch.size": "16384",
            "linger.ms": "1",
            "buffer.memory": "33554432",
            "key.serializer": "org.apache.kafka.common.serialization.StringSerializer",
            "value.serializer": "org.apache.kafka.common.serialization.StringSerializer",
            "group.id": "group.id",
            "enable.auto.commit": "true",
            "session.timeout.ms": "300000",
            "max.poll.records": "100"
         },
         "publishProperties": [
            {
               "kafkaTopic": "topic-1"
            }
         ],
         "messageListenerConsumeProperties": {
            "gradeSummaryMsgHandler": [
               {
                  "kafkaTopic": "sample-grade-calculation-uat"
               }
            ],
            "commentMsgHandler": [
               {
                  "kafkaTopic": "sample-comment-uat"
               },
               {
                  "kafkaTopic": "sample-play-time-uat"
               }
            ],
            "replyMsgHandler": [
               {
                  "kafkaTopic": "sample-reply-uat"
               }
            ]
         }
      },
      {
         "zone": "sh",
         "type": "databus",
         "databusProperties": {
            "appkey": "xxxx",
            "secret": "xxxx",
            "zone": "sh001"
         },
         "publishProperties": [
            {
               "kafkaTopic": "sample-comment-uat",
               "databusTopic": "xxxxx-T",
               "group": "xxxxx"
            },
            {
               "kafkaTopic": "sample-reply-uat",
               "databusTopic": "xxxxx-T",
               "group": "xxxxx"
            },
            {
               "kafkaTopic": "sample-grade-calculation-uat",
               "databusTopic": "xxxxx-T",
               "group": "xxxxx"
            },
            {
               "kafkaTopic": "sample-play-time-uat",
               "databusTopic": "xxxxx-T",
               "group": "xxxxx"
            },
            {
               "kafkaTopic": "sample-comment-change",
               "databusTopic": "xxxxx-T",
               "group": "xxxxx"
            }
         ],
         "messageListenerConsumeProperties": {
            "gradeSummaryMsgHandler": [
               {
                  "kafkaTopic": "sample-grade-calculation-uat",
                  "databusTopic": "xxxx-T",
                  "group": "xxxxx"
               }
            ],
            "commentMsgHandler": [
               {
                  "kafkaTopic": "sample-comment-uat",
                  "databusTopic": "xxxx-T",
                  "group": "xxxxx"
               },
               {
                  "kafkaTopic": "sample-play-time-uat",
                  "databusTopic": "xxxx-T",
                  "group": "xxxxx"
               }
            ],
            "replyMsgHandler": [
               {
                  "kafkaTopic": "sample-reply-uat",
                  "databusTopic": "xxxx-T",
                  "group": "xxxxx"
               }
            ]
         }
      }
   ],
   "producerStrategy": {
      "topicGreyConfig": {
         "sample-comment-uat": [
            {
               "zone": "gz",
               "grey": "100"
            },
            {
               "zone": "sh",
               "grey": "0"
            }
         ]
      },
      "topicProduceZoneDefault": {
         "sample-reply-uat": "gz",
         "sample-grade-calculation-uat": "gz",
         "sample-play-time-uat": "gz",
         "sample-comment-change": "gz"
      }
   }
}

```

对原有需要变更如下

1. 检查项目中kafka相关bean的参数配置，一般为applicationContext-kafka.xml,
   需要统一将生产相关DefaultKafkaProducerFactory和消费相关DefaultKafkaConsumerFactory、KafkaMessageListenerContainer移除；
   （但是保留原本的messageListener相关bean）；参考如下

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.springframework.org/schema/beans
         http://www.springframework.org/schema/beans/spring-beans.xsd">

  <!-- 定义producer的参数 -->
  <!--    <bean id="producerProperties" class="java.util.HashMap">-->
  <!--        <constructor-arg>-->
  <!--            <map>-->
  <!--                <entry key="bootstrap.servers" value="${kafka.bootstrap.servers}"/>-->
  <!--                <entry key="retries" value="${kafka.producer.retries}"/>-->
  <!--                <entry key="batch.size" value="${kafka.producer.batch.size}"/>-->
  <!--                <entry key="linger.ms" value="${kafka.producer.linger.ms}"/>-->
  <!--                <entry key="buffer.memory" value="${kafka.producer.buffer.memory}"/>-->
  <!--                <entry key="key.serializer" value="org.apache.kafka.common.serialization.StringSerializer"/>-->
  <!--                <entry key="value.serializer" value="org.apache.kafka.common.serialization.StringSerializer"/>-->
  <!--            </map>-->
  <!--        </constructor-arg>-->
  <!--    </bean>-->

  <!-- 创建 kafka template 需要使用的 producer factory bean -->
  <!--    <bean id="producerFactory" class="org.springframework.kafka.core.DefaultKafkaProducerFactory">-->
  <!--        <constructor-arg ref="producerProperties"/>-->
  <!--    </bean>-->

  <!-- 创建 kafka template bean，使用的时候，只需要注入这个bean，即可使用template的send消息方法 -->
  <!--    <bean id="kafkaTemplate" class="org.springframework.kafka.core.KafkaTemplate">-->
  <!--        <constructor-arg ref="producerFactory"/>-->
  <!--        <constructor-arg name="autoFlush" value="true"/>-->
  <!--    </bean>-->

  <!-- consumer -->
  <bean id="kafkaKeyDeserializer" class="org.apache.kafka.common.serialization.StringDeserializer"/>
  <bean id="kafkaValueDeserializer" class="org.apache.kafka.common.serialization.StringDeserializer"/>

  <!--    <bean id="consumerFactory" class="org.springframework.kafka.core.DefaultKafkaConsumerFactory">-->
  <!--        <constructor-arg>-->
  <!--            <map>-->
  <!--                <entry key="bootstrap.servers" value="${kafka.bootstrap.servers}"/>-->
  <!--                <entry key="group.id" value="${kafka.group.id}"/>-->
  <!--                <entry key="enable.auto.commit" value="${kafka.consumer.enable.auto.commit}"/>-->
  <!--                <entry key="session.timeout.ms" value="${kafka.consumer.session.timeout.ms}"/>-->
  <!--                <entry key="max.poll.records" value="${kafka.consumer.max.poll.records}"/>-->
  <!--            </map>-->
  <!--        </constructor-arg>-->
  <!--        <constructor-arg name="keyDeserializer" ref="kafkaKeyDeserializer"/>-->
  <!--        <constructor-arg name="valueDeserializer" ref="kafkaValueDeserializer"/>-->
  <!--    </bean>-->

  <bean id="gradeSummaryMsgHandler" class="com.jproject.zs.common.api.mq.GradeSummaryMsgHandler"/>
  <bean id="kafkaUnknownErrorHandler" class="com.jproject.zs.common.api.mq.KafkaUnknownErrorHandler"/>

  <!--    <bean id="gradeSummaryListener" class="org.springframework.kafka.listener.KafkaMessageListenerContainer">-->
  <!--        <constructor-arg name="consumerFactory" ref="consumerFactory"/>-->
  <!--        <constructor-arg name="containerProperties">-->
  <!--            <bean class="org.springframework.kafka.listener.config.ContainerProperties">-->
  <!--                <constructor-arg>-->
  <!--                    <array>-->
  <!--                        <value>${kafka.topic.grade.calculation}</value>-->
  <!--                    </array>-->
  <!--                </constructor-arg>-->
  <!--                <property name="messageListener" ref="gradeSummaryMsgHandler"/>-->
  <!--                <property name="genericErrorHandler" ref="kafkaUnknownErrorHandler"/>-->
  <!--            </bean>-->
  <!--        </constructor-arg>-->
  <!--    </bean>-->

  <bean id="commentMsgHandler" class="com.jproject.zs.common.api.mq.CommentMsgHandler"/>

  <!--    <bean id="commentListener" class="org.springframework.kafka.listener.KafkaMessageListenerContainer">-->
  <!--        <constructor-arg name="consumerFactory" ref="consumerFactory"/>-->
  <!--        <constructor-arg name="containerProperties">-->
  <!--            <bean class="org.springframework.kafka.listener.config.ContainerProperties">-->
  <!--                <constructor-arg>-->
  <!--                    <array>-->
  <!--                        <value>${kafka.topic.comment}</value>-->
  <!--                        <value>${kafka.topic.comment.play.time}</value>-->
  <!--                    </array>-->
  <!--                </constructor-arg>-->
  <!--                <property name="messageListener" ref="commentMsgHandler"/>-->
  <!--                <property name="genericErrorHandler" ref="kafkaUnknownErrorHandler"/>-->
  <!--            </bean>-->
  <!--        </constructor-arg>-->
  <!--    </bean>-->

  <bean id="replyMsgHandler" class="com.jproject.zs.common.api.mq.ReplyMsgHandler"/>

  <!--    <bean id="replyListener" class="org.springframework.kafka.listener.KafkaMessageListenerContainer">-->
  <!--        <constructor-arg name="consumerFactory" ref="consumerFactory"/>-->
  <!--        <constructor-arg name="containerProperties">-->
  <!--            <bean class="org.springframework.kafka.listener.config.ContainerProperties">-->
  <!--                <constructor-arg>-->
  <!--                    <array>-->
  <!--                        <value>${kafka.topic.reply}</value>-->
  <!--                    </array>-->
  <!--                </constructor-arg>-->
  <!--                <property name="messageListener" ref="replyMsgHandler"/>-->
  <!--                <property name="genericErrorHandler" ref="kafkaUnknownErrorHandler"/>-->
  <!--            </bean>-->
  <!--        </constructor-arg>-->
  <!--    </bean>-->
</beans>
```

## springboot项目

那么参数的构造方式可以直接使用ConfigurationProperties会简单很多,示例如下：

```java
import org.springframework.context.annotation.Configuration;

@Import(KafkaMigrationConfigurer.class)
@Configuration
public class MigrationConfigurer {

    @Bean
    @ConfigurationProperties("kafka")
    public KafkaClusterConfig kafkaClusterConfig() {
        return new KafkaClusterConfig();
    }
}
```

其参数格式使用yaml格式，格式如下，可以放在已有的配置键中，如application.yaml中

```yaml

```

# 场景2 使用kafka-migration做redis-缓存组件的淘汰通知

## 配合jproject-cache组件使用体验更佳

###    


