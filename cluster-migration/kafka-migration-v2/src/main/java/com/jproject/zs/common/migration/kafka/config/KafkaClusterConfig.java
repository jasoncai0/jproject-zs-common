package com.jproject.zs.common.migration.kafka.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import devmodule.component.databus.model.DatabusProperties;
import devmodule.component.databus.pub.DatabusPub;
import devmodule.component.databus.sub.DatabusSub;

/**
 * @author caizhensheng
 * @desc
 * @date 2023/1/30
 */

@Data
public class KafkaClusterConfig {


    private List<ClusterConfig> clusterConfigs;

    /**
     * 灰度写
     */
    private ProducerStrategy producerStrategy;

    private ConsumerStrategy consumerStrategy;


    public static enum DatabusType {
        /**
         * 通用的kafka
         */
        kafka,

        /**
         * 阿b的databus，IDC机房
         */
        databus

    }

    @Data
    public static class ClusterConfig {

        private String zone;

        private DatabusType type;

        /**
         * 当类型为kafka时，可以直接使用现成的producerFactoryBean和consumerFactoryBean 优先使用注入的bean进行构造
         */
        private String producerFactoryBean;

        /**
         * 当类型为kafka时，可以使用现成的consumerFactoryBean 优先使用注入的bean进行构造
         */
        private String consumerFactoryBean;
        /**
         * kafka bootstrap configs，如果producerFactoryBean consumerFactoryBean 为空，则#bootstrapConfig 必须提供
         */
        private Map<String, Object> bootstrapConfig;

        private DatabusProperties databusProperties;

        /**
         * 原本业务中的生产topic名称， 为kafka topic
         */
        private List<PubProperties> publishProperties = new ArrayList<>();


        /**
         * 原本业务中的消费topic名称，为kafka topic;
         * <p>
         * 注意key为格式为'{beanName}#{consumeMethodName}', 例如 'commentMsgHandler#onMessage', 如果如果消费的方法名不填写， 那么即默认为 onMessage
         * 方法名； 如果bean类型为{@link org.springframework.kafka.listener.MessageListener}那么，方法名也无需填写；
         */
        private Map<String, List<SubProperties>> messageListenerConsumeProperties = new HashMap<>();

        private Boolean producerAutoFlash = true;


    }

    @Data
    public static class SubProperties extends DatabusProperties {

        private String group;

        private String kafkaTopic;

        private String databusTopic;

        private int timeout = 50000;

        private int wait = 1000;


        public DatabusSub createDatabusSubscriber(DatabusProperties clusterConfig) {

            DatabusProperties properties = new DatabusProperties();
            properties.setSecret(Optional.ofNullable(this.getSecret())
                    .orElse(clusterConfig.getSecret()));
            properties.setAppkey(Optional.ofNullable(this.getAppkey())
                    .orElse(clusterConfig.getAppkey()));
            properties.setZone(Optional.ofNullable((this.getZone()))
                    .orElse(clusterConfig.getZone()));
            properties.getSub().setTopic(this.getDatabusTopic());
            properties.getSub().setGroup(this.getGroup());
            properties.getSub().setTimeout(this.getTimeout());
            properties.getSub().setWait(this.getWait());

            DatabusSub databusSub = new DatabusSub(properties);
            return databusSub;
        }


    }

    @Data
    public static class PubProperties extends DatabusProperties {


        private String group;

        /**
         * 特指kafka topic，如果映射多个kafka的topic，则在列表中进行一一映射；
         */
        private String kafkaTopic;

        /**
         * 如果是kafka， 无需设置， 如果是databus那么除了需要设置
         */
        private String databusTopic;

        private int timeout = 300;

        private int partitionSize = 1000;


        public DatabusPub createPublisher(DatabusProperties clusterConfig) {

            DatabusProperties properties = new DatabusProperties();
            properties.setSecret(Optional.ofNullable(this.getSecret())
                    .orElse(clusterConfig.getSecret()));
            properties.setAppkey(Optional.ofNullable(this.getAppkey())
                    .orElse(clusterConfig.getAppkey()));
            properties.setZone(Optional.ofNullable(this.getZone())
                    .orElse(clusterConfig.getZone()));
            properties.getPub().setTopic(this.getDatabusTopic());
            properties.getPub().setGroup(this.getGroup());
            properties.getPub().setTimeout(this.getTimeout());
            properties.getPub().setPartitionSize(this.getPartitionSize());

            return new DatabusPub(properties);
        }


    }

    @Data
    public static class DatabusConfig {


    }


    /**
     * @author caizhensheng
     * @desc
     * @date 2023/1/30
     */
    @Data
    public static class ProducerStrategy {


        /**
         * topic对应的zone灰度策略
         */
        //    @Value("#{${kafka.producerStrategy.topicGreyConfig}}")
        private Map<String, List<ZoneGreyConfig>> topicGreyConfig;

        /**
         * topic的默认写到的zone，可以为空；只要topic都已经配置好了灰度策略；
         */
//        @Value("#{${kafka.producerStrategy.topicProduceZoneDefault}}")
        private Map<String, String> topicProduceZoneDefault = new HashMap<>();


        @Data
        public static class ZoneGreyConfig {

            private String zone;

            /**
             * 0-100
             */
            private Integer grey;

        }
    }

    /**
     * @author caizhensheng
     * @desc
     * @date 2023/1/30
     */
    @Data
    public static class ConsumerStrategy {

        private StrategyType strategyType = StrategyType.READ_ALL_CLUSTER;


        public enum StrategyType {

            /**
             * 消费所有不同地区集群的消息
             */
            READ_ALL_CLUSTER,

            ;

        }

    }


}
