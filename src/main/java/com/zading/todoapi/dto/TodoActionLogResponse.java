package com.zading.todoapi.dto;

import com.zading.todoapi.model.TodoAction;

import java.time.LocalDateTime;

public class TodoActionLogResponse {
    private Long id;
    private TodoAction action;
    private String description;
    private LocalDateTime createdAt;

    public TodoActionLogResponse(Long id, TodoAction action, String description, LocalDateTime createdAt) {
        this.id = id;
        this.action = action;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public TodoAction getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
