package com.zading.todoapi.dto;

import com.zading.todoapi.model.TodoPriority;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminTodoResponse {
    private Long id;
    private String title;
    private boolean completed;
    private boolean deleted;
    private TodoPriority priority;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private String username;

    public AdminTodoResponse(
            Long id,
            String title,
            boolean completed,
            boolean deleted,
            TodoPriority priority,
            LocalDate dueDate,
            LocalDateTime completedAt,
            LocalDateTime deletedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long userId,
            String username
    ) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.deleted = deleted;
        this.priority = priority;
        this.dueDate = dueDate;
        this.completedAt = completedAt;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public TodoPriority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
