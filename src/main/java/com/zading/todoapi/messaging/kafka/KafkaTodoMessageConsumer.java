package com.zading.todoapi.messaging.kafka;

import com.zading.todoapi.messaging.TodoActionLogMessage;
import com.zading.todoapi.messaging.TodoActionLogMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Kafka 消费者适配器。
 *
 * <p>处理失败时抛出异常，Spring Kafka 会按照 RetryableTopic 的配置重试；
 * 多次失败后消息进入 DLT（Dead Letter Topic）。</p>
 */
@Component
@Profile("kafka")
public class KafkaTodoMessageConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaTodoMessageConsumer.class);

    private final TodoActionLogMessageHandler messageHandler;

    public KafkaTodoMessageConsumer(TodoActionLogMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1_000L)
    )
    @KafkaListener(
            topics = "${app.messaging.kafka.topic}",
            groupId = "${app.messaging.kafka.consumer-group}"
    )
    public void consume(TodoActionLogMessage message) {
        messageHandler.handle(message);
    }

    @DltHandler
    public void consumeDeadLetter(
            TodoActionLogMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        log.error("Kafka 消息进入死信 Topic，topic={}, messageId={}", topic, message.messageId());
    }
}
