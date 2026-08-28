package com.zading.todoapi.messaging.kafka;

import com.zading.todoapi.config.properties.MessagingProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

/**
 * Kafka 消息拓扑配置。
 *
 * <p>只有启用 kafka profile 时才创建 Topic Bean，因此默认运行不会连接 Kafka。</p>
 */
@Configuration
@Profile("kafka")
@EnableKafka
public class KafkaMessagingConfig {
    @Bean
    public NewTopic todoActionLogTopic(MessagingProperties properties) {
        MessagingProperties.Kafka kafka = properties.kafka();
        return TopicBuilder.name(kafka.topic())
                .partitions(kafka.partitions())
                .replicas(kafka.replicas())
                .build();
    }
}
