package com.zading.todoapi.security;

public class AuthenticatedUser {
    private final Long id;
    private final String username;

    public AuthenticatedUser(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
