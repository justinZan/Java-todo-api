package com.zading.todoapi.service;

import org.springframework.core.io.Resource;

public record TodoAttachmentDownload(
        String originalFilename,
        String contentType,
        long fileSize,
        Resource resource
) {
}
