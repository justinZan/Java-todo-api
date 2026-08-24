package com.zading.todoapi.dto;

import java.time.LocalDateTime;

public class TodoAttachmentResponse {
    private Long id;
    private Long todoId;
    private String originalFilename;
    private String contentType;
    private long fileSize;
    private LocalDateTime createdAt;

    public TodoAttachmentResponse(
            Long id,
            Long todoId,
            String originalFilename,
            String contentType,
            long fileSize,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.todoId = todoId;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTodoId() {
        return todoId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
