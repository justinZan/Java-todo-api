package com.zading.todoapi.messaging.rabbitmq;

import com.zading.todoapi.messaging.TodoActionLogMessage;
import com.zading.todoapi.messaging.TodoActionLogMessageHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 消费者适配器。
 *
 * <p>方法正常返回时由容器自动 ACK；处理抛异常时进入 Spring Retry，
 * 最终拒绝且不重新入队，由队列的 dead-letter 配置转发到死信队列。</p>
 */
@Component
@Profile("rabbitmq")
public class RabbitTodoMessageConsumer {
    private final TodoActionLogMessageHandler messageHandler;

    public RabbitTodoMessageConsumer(TodoActionLogMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @RabbitListener(
            queues = "${app.messaging.rabbit.queue}",
            containerFactory = "todoRabbitListenerContainerFactory"
    )
    public void consume(TodoActionLogMessage message) {
        messageHandler.handle(message);
    }
}
