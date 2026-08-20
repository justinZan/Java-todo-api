package com.zading.todoapi.event;

import com.zading.todoapi.model.TodoAction;

import java.util.Objects;

public record TodoActionLogEvent(
        Long todoId,
        Long userId,
        TodoAction action,
        String description
) {
    public TodoActionLogEvent {
        Objects.requireNonNull(todoId, "todoId 不能为空");
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(action, "action 不能为空");
        Objects.requireNonNull(description, "description 不能为空");
    }
}
