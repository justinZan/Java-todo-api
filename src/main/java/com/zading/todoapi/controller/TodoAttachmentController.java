package com.zading.todoapi.controller;

import com.zading.todoapi.dto.ApiResponse;
import com.zading.todoapi.dto.TodoAttachmentResponse;
import com.zading.todoapi.model.TodoAttachment;
import com.zading.todoapi.security.AuthenticatedUser;
import com.zading.todoapi.service.TodoAttachmentDownload;
import com.zading.todoapi.service.TodoAttachmentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/todos/{todoId}/attachments")
public class TodoAttachmentController {
    private final TodoAttachmentService todoAttachmentService;

    public TodoAttachmentController(TodoAttachmentService todoAttachmentService) {
        this.todoAttachmentService = todoAttachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TodoAttachmentResponse>> uploadAttachment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long todoId,
            @RequestParam("file") MultipartFile file
    ) {
        TodoAttachment attachment = todoAttachmentService.uploadAttachment(currentUser.getId(), todoId, file);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{attachmentId}")
                .buildAndExpand(attachment.getId())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.created(toResponse(attachment)));
    }

    @GetMapping
    public ApiResponse<List<TodoAttachmentResponse>> getAttachments(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long todoId
    ) {
        List<TodoAttachmentResponse> attachments = todoAttachmentService.getAttachments(currentUser.getId(), todoId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.success(attachments);
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long todoId,
            @PathVariable Long attachmentId
    ) {
        TodoAttachmentDownload download = todoAttachmentService.downloadAttachment(currentUser.getId(), todoId, attachmentId);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(download.originalFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(parseMediaType(download.contentType()))
                .contentLength(download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(download.resource());
    }

    @DeleteMapping("/{attachmentId}")
    public ApiResponse<Void> deleteAttachment(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long todoId,
            @PathVariable Long attachmentId
    ) {
        todoAttachmentService.deleteAttachment(currentUser.getId(), todoId, attachmentId);
        return ApiResponse.success("删除附件成功", null);
    }

    private TodoAttachmentResponse toResponse(TodoAttachment attachment) {
        return new TodoAttachmentResponse(
                attachment.getId(),
                attachment.getTodo().getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getCreatedAt()
        );
    }

    private MediaType parseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
