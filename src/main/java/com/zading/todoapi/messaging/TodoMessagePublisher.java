package com.zading.todoapi.messaging;

/**
 * 消息发布抽象。
 *
 * <p>业务层只依赖这个接口，不直接依赖 KafkaTemplate 或 RabbitTemplate。</p>
 */
public interface TodoMessagePublisher {
    void publish(TodoActionLogMessage message);
}
