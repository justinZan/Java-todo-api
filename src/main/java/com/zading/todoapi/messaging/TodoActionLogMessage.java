package com.zading.todoapi.messaging;

import com.zading.todoapi.event.TodoActionLogEvent;
import com.zading.todoapi.model.TodoAction;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 在消息中传递的 Todo 操作日志数据。
 *
 * <p>消息不携带 JPA Entity，只携带消费者处理所需的基本字段，
 * 并使用 messageId 支持重复消费时的幂等判断。</p>
 */
public record TodoActionLogMessage(
        UUID messageId,
        Long todoId,
        Long userId,
        TodoAction action,
        String description,
        Instant occurredAt
) {
    public TodoActionLogMessage {
        Objects.requireNonNull(messageId, "messageId 不能为空");
        Objects.requireNonNull(todoId, "todoId 不能为空");
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(action, "action 不能为空");
        Objects.requireNonNull(description, "description 不能为空");
        Objects.requireNonNull(occurredAt, "occurredAt 不能为空");
    }

    public static TodoActionLogMessage from(TodoActionLogEvent event) {
        return new TodoActionLogMessage(
                UUID.randomUUID(),
                event.todoId(),
                event.userId(),
                event.action(),
                event.description(),
                Instant.now()
        );
    }

    public TodoActionLogEvent toEvent() {
        return new TodoActionLogEvent(todoId, userId, action, description);
    }
}
