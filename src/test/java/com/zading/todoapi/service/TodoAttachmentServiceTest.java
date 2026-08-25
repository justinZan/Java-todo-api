package com.zading.todoapi.service;

import com.zading.todoapi.config.properties.FileStorageProperties;
import com.zading.todoapi.exception.BusinessException;
import com.zading.todoapi.exception.TodoAttachmentNotFoundException;
import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoAttachment;
import com.zading.todoapi.repository.TodoAttachmentRepository;
import com.zading.todoapi.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoAttachmentServiceTest {
    @Mock
    private TodoAttachmentRepository todoAttachmentRepository;

    @Mock
    private TodoRepository todoRepository;

    @TempDir
    private Path tempDir;

    private TodoAttachmentService todoAttachmentService;

    @BeforeEach
    void setUp() {
        todoAttachmentService = new TodoAttachmentService(
                todoAttachmentRepository,
                todoRepository,
                new FileStorageProperties(tempDir.toString(), 100)
        );
    }

    @Test
    void shouldUploadAttachmentAndPersistMetadata() throws Exception {
        Todo todo = todo(10L, 1L);
        MultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8)
        );
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(todo));
        when(todoAttachmentRepository.save(any(TodoAttachment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TodoAttachment result = todoAttachmentService.uploadAttachment(1L, 10L, file);

        assertEquals("note.txt", result.getOriginalFilename());
        assertEquals("text/plain", result.getContentType());
        assertEquals(5L, result.getFileSize());
        assertTrue(result.getStoragePath().startsWith("todo-attachments/1/10/"));
        assertTrue(result.getStoragePath().endsWith(".txt"));
        assertTrue(Files.exists(tempDir.resolve(result.getStoragePath())));
        verify(todoAttachmentRepository).save(any(TodoAttachment.class));
    }

    @Test
    void shouldRejectAttachmentWhenItExceedsConfiguredSize() {
        Todo todo = todo(10L, 1L);
        MultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                "too large".getBytes(StandardCharsets.UTF_8)
        );
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(todo));
        todoAttachmentService = new TodoAttachmentService(
                todoAttachmentRepository,
                todoRepository,
                new FileStorageProperties(tempDir.toString(), 1)
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> todoAttachmentService.uploadAttachment(1L, 10L, file)
        );

        assertEquals("BAD_REQUEST", exception.getErrorCode().name());
        verify(todoAttachmentRepository, never()).save(any(TodoAttachment.class));
    }

    @Test
    void shouldRejectUnsafeOriginalFilename() {
        Todo todo = todo(10L, 1L);
        MultipartFile file = new MockMultipartFile(
                "file",
                "../secret.txt",
                "text/plain",
                "secret".getBytes(StandardCharsets.UTF_8)
        );
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(todo));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> todoAttachmentService.uploadAttachment(1L, 10L, file)
        );

        assertEquals("BAD_REQUEST", exception.getErrorCode().name());
        verify(todoAttachmentRepository, never()).save(any(TodoAttachment.class));
    }

    @Test
    void shouldReturnAttachmentsOnlyAfterTodoOwnershipCheck() {
        Todo todo = todo(10L, 1L);
        TodoAttachment attachment = new TodoAttachment();
        attachment.setId(20L);
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(todo));
        when(todoAttachmentRepository.findByTodoIdAndUserIdOrderByCreatedAtAscIdAsc(10L, 1L))
                .thenReturn(List.of(attachment));

        List<TodoAttachment> result = todoAttachmentService.getAttachments(1L, 10L);

        assertEquals(1, result.size());
        verify(todoAttachmentRepository).findByTodoIdAndUserIdOrderByCreatedAtAscIdAsc(10L, 1L);
    }

    @Test
    void shouldDownloadExistingAttachment() throws Exception {
        Todo todo = todo(10L, 1L);
        TodoAttachment attachment = attachment(20L, todo, "todo-attachments/1/10/note.txt");
        Path filePath = tempDir.resolve(attachment.getStoragePath());
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, "download content");
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(todo));
        when(todoAttachmentRepository.findByIdAndTodoIdAndUserId(20L, 10L, 1L))
                .thenReturn(Optional.of(attachment));

        TodoAttachmentDownload result = todoAttachmentService.downloadAttachment(1L, 10L, 20L);

        assertEquals("note.txt", result.originalFilename());
        assertEquals("text/plain", result.contentType());
        assertTrue(result.resource().exists());
        assertEquals("download content", new String(result.resource().getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void shouldRejectDownloadWhenAttachmentMetadataExistsButFileIsMissing() {
        Todo todo = todo(10L, 1L);
        TodoAttachment attachment = attachment(20L, todo, "todo-attachments/1/10/missing.txt");
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(todo));
        when(todoAttachmentRepository.findByIdAndTodoIdAndUserId(20L, 10L, 1L))
                .thenReturn(Optional.of(attachment));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> todoAttachmentService.downloadAttachment(1L, 10L, 20L)
        );

        assertEquals("INTERNAL_ERROR", exception.getErrorCode().name());
    }

    @Test
    void shouldDeleteMetadataAndPhysicalFile() throws Exception {
        Todo todo = todo(10L, 1L);
        TodoAttachment attachment = attachment(20L, todo, "todo-attachments/1/10/delete.txt");
        Path filePath = tempDir.resolve(attachment.getStoragePath());
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, "delete content");
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(todo));
        when(todoAttachmentRepository.findByIdAndTodoIdAndUserId(20L, 10L, 1L))
                .thenReturn(Optional.of(attachment));

        todoAttachmentService.deleteAttachment(1L, 10L, 20L);

        verify(todoAttachmentRepository).delete(attachment);
        assertFalse(Files.exists(filePath));
    }

    @Test
    void shouldThrowWhenAttachmentDoesNotExist() {
        Todo todo = todo(10L, 1L);
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(todo));
        when(todoAttachmentRepository.findByIdAndTodoIdAndUserId(20L, 10L, 1L))
                .thenReturn(Optional.empty());

        TodoAttachmentNotFoundException exception = assertThrows(
                TodoAttachmentNotFoundException.class,
                () -> todoAttachmentService.downloadAttachment(1L, 10L, 20L)
        );

        assertTrue(exception.getMessage().contains("20"));
    }

    private Todo todo(Long id, Long userId) {
        AppUser user = new AppUser("user-" + userId, "encoded-password");
        user.setId(userId);
        Todo todo = new Todo(id, "附件 Todo", false);
        todo.setUser(user);
        return todo;
    }

    private TodoAttachment attachment(Long id, Todo todo, String storagePath) {
        TodoAttachment attachment = new TodoAttachment();
        attachment.setId(id);
        attachment.setTodo(todo);
        attachment.setUser(todo.getUser());
        attachment.setOriginalFilename(storagePath.substring(storagePath.lastIndexOf('/') + 1));
        attachment.setContentType("text/plain");
        attachment.setFileSize(10L);
        attachment.setStoragePath(storagePath);
        return attachment;
    }
}
