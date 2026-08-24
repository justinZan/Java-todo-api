package com.zading.todoapi.service;

import com.zading.todoapi.config.properties.FileStorageProperties;
import com.zading.todoapi.exception.BusinessException;
import com.zading.todoapi.exception.ErrorCode;
import com.zading.todoapi.exception.TodoAttachmentNotFoundException;
import com.zading.todoapi.exception.TodoNotFoundException;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoAttachment;
import com.zading.todoapi.repository.TodoAttachmentRepository;
import com.zading.todoapi.repository.TodoRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class TodoAttachmentService {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final TodoAttachmentRepository todoAttachmentRepository;
    private final TodoRepository todoRepository;
    private final FileStorageProperties fileStorageProperties;

    public TodoAttachmentService(
            TodoAttachmentRepository todoAttachmentRepository,
            TodoRepository todoRepository,
            FileStorageProperties fileStorageProperties
    ) {
        this.todoAttachmentRepository = todoAttachmentRepository;
        this.todoRepository = todoRepository;
        this.fileStorageProperties = fileStorageProperties;
    }

    @Transactional
    public TodoAttachment uploadAttachment(Long userId, Long todoId, MultipartFile file) {
        Todo todo = getVisibleTodo(userId, todoId);
        validateFile(file);

        Path rootLocation = getRootLocation();
        String originalFilename = normalizeOriginalFilename(file.getOriginalFilename());
        String storedFilename = generateStoredFilename(originalFilename);
        String storagePath = "todo-attachments/" + userId + "/" + todoId + "/" + storedFilename;
        Path targetPath = resolveStoragePath(rootLocation, storagePath);

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);

            TodoAttachment attachment = new TodoAttachment();
            attachment.setTodo(todo);
            attachment.setUser(todo.getUser());
            attachment.setOriginalFilename(originalFilename);
            attachment.setStoredFilename(storedFilename);
            attachment.setContentType(normalizeContentType(file.getContentType()));
            attachment.setFileSize(file.getSize());
            attachment.setStoragePath(storagePath);

            return todoAttachmentRepository.save(attachment);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存附件失败");
        } catch (RuntimeException ex) {
            deleteFileQuietly(targetPath);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<TodoAttachment> getAttachments(Long userId, Long todoId) {
        getVisibleTodo(userId, todoId);
        return todoAttachmentRepository.findByTodoIdAndUserIdOrderByCreatedAtAscIdAsc(todoId, userId);
    }

    @Transactional(readOnly = true)
    public TodoAttachmentDownload downloadAttachment(Long userId, Long todoId, Long attachmentId) {
        TodoAttachment attachment = getAttachment(userId, todoId, attachmentId);
        Path filePath = resolveStoragePath(getRootLocation(), attachment.getStoragePath());

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "附件文件不存在");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            return new TodoAttachmentDownload(
                    attachment.getOriginalFilename(),
                    attachment.getContentType(),
                    attachment.getFileSize(),
                    resource
            );
        } catch (MalformedURLException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "附件文件路径不正确");
        }
    }

    @Transactional
    public void deleteAttachment(Long userId, Long todoId, Long attachmentId) {
        TodoAttachment attachment = getAttachment(userId, todoId, attachmentId);
        Path filePath = resolveStoragePath(getRootLocation(), attachment.getStoragePath());

        todoAttachmentRepository.delete(attachment);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除附件文件失败");
        }
    }

    private Todo getVisibleTodo(Long userId, Long todoId) {
        return todoRepository.findByIdAndUserIdAndDeletedFalse(todoId, userId)
                .orElseThrow(() -> new TodoNotFoundException(todoId));
    }

    private TodoAttachment getAttachment(Long userId, Long todoId, Long attachmentId) {
        getVisibleTodo(userId, todoId);

        return todoAttachmentRepository.findByIdAndTodoIdAndUserId(attachmentId, todoId, userId)
                .orElseThrow(() -> new TodoAttachmentNotFoundException(attachmentId));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("附件不能为空");
        }

        if (file.getSize() > fileStorageProperties.maxFileSizeBytes()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "附件大小不能超过 " + fileStorageProperties.maxFileSizeBytes() + " 字节");
        }
    }

    private String normalizeOriginalFilename(String originalFilename) {
        String cleanedFilename = StringUtils.cleanPath(originalFilename == null ? "attachment" : originalFilename);

        if (cleanedFilename.contains("..")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "附件文件名不安全");
        }

        if (cleanedFilename.isBlank()) {
            return "attachment";
        }

        return cleanedFilename;
    }

    private String generateStoredFilename(String originalFilename) {
        return UUID.randomUUID() + getExtension(originalFilename);
    }

    private String getExtension(String originalFilename) {
        int lastDotIndex = originalFilename.lastIndexOf(".");

        if (lastDotIndex < 0 || lastDotIndex == originalFilename.length() - 1) {
            return "";
        }

        return originalFilename.substring(lastDotIndex);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }

        return contentType;
    }

    private Path getRootLocation() {
        return Paths.get(fileStorageProperties.rootLocation()).toAbsolutePath().normalize();
    }

    private Path resolveStoragePath(Path rootLocation, String storagePath) {
        Path resolvedPath = rootLocation.resolve(storagePath).normalize();

        if (!resolvedPath.startsWith(rootLocation)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "附件路径不安全");
        }

        return resolvedPath;
    }

    private void deleteFileQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
