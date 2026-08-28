package com.zading.todoapi.messaging.rabbitmq;

import com.zading.todoapi.config.properties.MessagingProperties;
import com.zading.todoapi.messaging.TodoActionLogMessage;
import com.zading.todoapi.messaging.TodoMessagePublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** RabbitMQ 生产者适配器。 */
@Component
@Profile("rabbitmq")
public class RabbitTodoMessagePublisher implements TodoMessagePublisher {
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;

    public RabbitTodoMessagePublisher(
            RabbitTemplate rabbitTemplate,
            MessagingProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(TodoActionLogMessage message) {
        MessagingProperties.Rabbit rabbit = properties.rabbit();
        rabbitTemplate.convertAndSend(
                rabbit.exchange(),
                rabbit.routingKey(),
                message,
                amqpMessage -> {
                    amqpMessage.getMessageProperties().setMessageId(message.messageId().toString());
                    return amqpMessage;
                }
        );
    }
}
