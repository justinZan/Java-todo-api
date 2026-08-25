package com.zading.todoapi.dto;

import com.zading.todoapi.model.UserRole;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String username;
    private UserRole role;
    private LocalDateTime createdAt;

    public UserResponse(Long id, String username, UserRole role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
