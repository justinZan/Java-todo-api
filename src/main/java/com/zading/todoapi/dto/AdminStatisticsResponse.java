package com.zading.todoapi.dto;

public class AdminStatisticsResponse {
    private long totalUsers;
    private long totalTodos;
    private long activeTodos;
    private long completedTodos;
    private long pendingTodos;
    private long deletedTodos;

    public AdminStatisticsResponse(
            long totalUsers,
            long totalTodos,
            long activeTodos,
            long completedTodos,
            long pendingTodos,
            long deletedTodos
    ) {
        this.totalUsers = totalUsers;
        this.totalTodos = totalTodos;
        this.activeTodos = activeTodos;
        this.completedTodos = completedTodos;
        this.pendingTodos = pendingTodos;
        this.deletedTodos = deletedTodos;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalTodos() {
        return totalTodos;
    }

    public long getActiveTodos() {
        return activeTodos;
    }

    public long getCompletedTodos() {
        return completedTodos;
    }

    public long getPendingTodos() {
        return pendingTodos;
    }

    public long getDeletedTodos() {
        return deletedTodos;
    }
}
