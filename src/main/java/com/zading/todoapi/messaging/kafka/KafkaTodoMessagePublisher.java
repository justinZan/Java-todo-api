package com.zading.todoapi.messaging.kafka;

import com.zading.todoapi.config.properties.MessagingProperties;
import com.zading.todoapi.messaging.MessagePublishException;
import com.zading.todoapi.messaging.TodoActionLogMessage;
import com.zading.todoapi.messaging.TodoMessagePublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka 生产者适配器。
 *
 * <p>等待 Broker 确认发送结果，便于在应用日志中明确知道消息是否成功写入 Kafka。
 * 真实高吞吐场景可以改成异步回调，但必须补充失败处理和监控。</p>
 */
@Component
@Profile("kafka")
public class KafkaTodoMessagePublisher implements TodoMessagePublisher {
    private final KafkaTemplate<String, TodoActionLogMessage> kafkaTemplate;
    private final MessagingProperties properties;

    public KafkaTodoMessagePublisher(
            KafkaTemplate<String, TodoActionLogMessage> kafkaTemplate,
            MessagingProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(TodoActionLogMessage message) {
        try {
            kafkaTemplate.send(
                    properties.kafka().topic(),
                    message.messageId().toString(),
                    message
            ).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MessagePublishException("Kafka 消息发送被中断", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new MessagePublishException("Kafka 消息发送失败", ex);
        }
    }
}
