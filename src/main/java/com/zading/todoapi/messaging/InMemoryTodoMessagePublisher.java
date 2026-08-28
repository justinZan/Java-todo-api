package com.zading.todoapi.messaging;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 默认消息实现。
 *
 * <p>不启动 Kafka 或 RabbitMQ 时，直接在当前应用内调用统一消息处理器。
 * 第 16 周的 Spring Event 仍负责异步触发，因此本地可以验证完整业务链路。</p>
 */
@Component
@Profile("!kafka & !rabbitmq")
public class InMemoryTodoMessagePublisher implements TodoMessagePublisher {
    private final TodoActionLogMessageHandler messageHandler;

    public InMemoryTodoMessagePublisher(TodoActionLogMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void publish(TodoActionLogMessage message) {
        messageHandler.handle(message);
    }
}
