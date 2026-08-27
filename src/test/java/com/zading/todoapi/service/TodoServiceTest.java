package com.zading.todoapi.service;

import com.zading.todoapi.event.TodoEventPublisher;
import com.zading.todoapi.exception.TodoNotFoundException;
import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoAction;
import com.zading.todoapi.model.TodoPriority;
import com.zading.todoapi.repository.TodoActionLogRepository;
import com.zading.todoapi.repository.TodoRepository;
import com.zading.todoapi.repository.UserRepository;
import com.zading.todoapi.config.properties.RedisProtectionProperties;
import com.zading.todoapi.exception.BusinessException;
import com.zading.todoapi.exception.ErrorCode;
import com.zading.todoapi.redis.IdempotencyClaim;
import com.zading.todoapi.redis.IdempotencyStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {
    @Mock
    private TodoRepository todoRepository;

    @Mock
    private TodoActionLogRepository todoActionLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TodoEventPublisher todoEventPublisher;

    @Mock
    private IdempotencyStore idempotencyStore;

    private final RedisProtectionProperties redisProperties = new RedisProtectionProperties(
            Duration.ofMinutes(5),
            Duration.ofMinutes(10),
            Duration.ofMinutes(1),
            5,
            30
    );

    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = new TodoService(
                todoRepository,
                todoActionLogRepository,
                userRepository,
                todoEventPublisher,
                idempotencyStore,
                redisProperties
        );
    }

    @Test
    void shouldUseCombinedFilterWhenCompletedAndKeywordAreProvided() {
        Long userId = 1L;
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Todo> expected = new PageImpl<>(List.of(todo(10L, userId, "学习 Java")));
        when(todoRepository.findByUserIdAndCompletedAndTitleContainingIgnoreCaseAndDeletedFalse(
                userId, false, "java", pageable
        )).thenReturn(expected);

        Page<Todo> actual = todoService.getTodos(userId, false, "  java  ", pageable);

        assertSame(expected, actual);
        verify(todoRepository).findByUserIdAndCompletedAndTitleContainingIgnoreCaseAndDeletedFalse(
                userId, false, "java", pageable
        );
        verify(todoRepository, never()).findByUserIdAndDeletedFalse(userId, pageable);
    }

    @Test
    void shouldReturnUserTodoWhenTodoExists() {
        Todo expected = todo(10L, 1L, "学习 Java");
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(expected));

        Todo actual = todoService.getTodo(1L, 10L);

        assertSame(expected, actual);
        verify(todoRepository).findByIdAndUserIdAndDeletedFalse(10L, 1L);
    }

    @Test
    void shouldThrowWhenTodoDoesNotBelongToUser() {
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 2L)).thenReturn(Optional.empty());

        TodoNotFoundException exception = assertThrows(
                TodoNotFoundException.class,
                () -> todoService.getTodo(2L, 10L)
        );

        assertTrue(exception.getMessage().contains("10"));
    }

    @Test
    void shouldCreateTodoWithNormalizedTitleAndDefaultPriority() {
        AppUser user = user(1L);
        Todo savedTodo = todo(10L, 1L, "学习 Java");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(todoRepository.save(any(Todo.class))).thenReturn(savedTodo);

        Todo result = todoService.addTodo(1L, "  学习 Java  ", null, LocalDate.of(2026, 9, 1));

        assertSame(savedTodo, result);
        verify(todoRepository).save(any(Todo.class));
        verify(todoEventPublisher).publishActionLog(savedTodo, user, TodoAction.CREATED, "创建 Todo");
    }

    @Test
    void shouldRejectCreateWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> todoService.addTodo(99L, "任务", TodoPriority.MEDIUM, null)
        );

        assertEquals("当前用户不存在", exception.getMessage());
        verify(todoRepository, never()).save(any(Todo.class));
        verify(todoEventPublisher, never()).publishActionLog(any(), any(), any(), any());
    }

    @Test
    void shouldReplayTodoWhenIdempotencyKeyWasCompleted() {
        Todo savedTodo = todo(10L, 1L, "只创建一次");
        when(idempotencyStore.tryClaim(any(), any())).thenReturn(IdempotencyClaim.completed("10"));
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(savedTodo));

        Todo result = todoService.addTodo(1L, "不会再次创建", TodoPriority.HIGH, null, "request-001");

        assertSame(savedTodo, result);
        verify(userRepository, never()).findById(any());
        verify(todoRepository, never()).save(any(Todo.class));
    }

    @Test
    void shouldRejectSameIdempotencyKeyWhileRequestIsProcessing() {
        when(idempotencyStore.tryClaim(any(), any())).thenReturn(IdempotencyClaim.processing());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> todoService.addTodo(1L, "任务", TodoPriority.MEDIUM, null, "request-002")
        );

        assertEquals(ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS, exception.getErrorCode());
        verify(todoRepository, never()).save(any(Todo.class));
    }

    @Test
    void shouldStoreTodoIdAfterFirstIdempotentCreate() {
        AppUser user = user(1L);
        Todo savedTodo = todo(10L, 1L, "只创建一次");
        when(idempotencyStore.tryClaim(any(), any())).thenReturn(IdempotencyClaim.claimed("owner-001"));
        when(idempotencyStore.complete(any(), any(), any(), any())).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(todoRepository.save(any(Todo.class))).thenReturn(savedTodo);

        Todo result = todoService.addTodo(1L, "只创建一次", TodoPriority.MEDIUM, null, "request-003");

        assertSame(savedTodo, result);
        verify(idempotencyStore).complete(any(), eq("owner-001"), eq("10"), eq(Duration.ofMinutes(10)));
    }

    @Test
    void shouldUpdateFieldsAndPublishTwoActionEvents() {
        AppUser user = user(1L);
        Todo todo = todo(10L, 1L, "旧标题");
        todo.setUser(user);
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(todo));
        when(todoRepository.save(todo)).thenReturn(todo);

        Todo result = todoService.updateTodo(
                1L,
                10L,
                " 新标题 ",
                true,
                TodoPriority.HIGH,
                LocalDate.of(2026, 9, 2)
        );

        assertSame(todo, result);
        assertEquals("新标题", todo.getTitle());
        assertTrue(todo.isCompleted());
        assertEquals(TodoPriority.HIGH, todo.getPriority());
        assertEquals(LocalDate.of(2026, 9, 2), todo.getDueDate());
        assertNotNull(todo.getCompletedAt());

        ArgumentCaptor<TodoAction> actionCaptor = ArgumentCaptor.forClass(TodoAction.class);
        verify(todoEventPublisher, times(2)).publishActionLog(
                eq(todo), eq(user), actionCaptor.capture(), any(String.class)
        );
        assertEquals(List.of(TodoAction.UPDATED, TodoAction.COMPLETED), actionCaptor.getAllValues());
    }

    @Test
    void shouldToggleIncompleteTodoToCompleted() {
        AppUser user = user(1L);
        Todo todo = todo(10L, 1L, "待完成任务");
        todo.setUser(user);
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(todo));
        when(todoRepository.save(todo)).thenReturn(todo);

        todoService.toggleTodo(1L, 10L);

        assertTrue(todo.isCompleted());
        assertNotNull(todo.getCompletedAt());
        verify(todoEventPublisher).publishActionLog(todo, user, TodoAction.COMPLETED, "完成 Todo");
    }

    @Test
    void shouldSoftDeleteTodoAndSetDeletedAt() {
        AppUser user = user(1L);
        Todo todo = todo(10L, 1L, "待删除任务");
        todo.setUser(user);
        when(todoRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(todo));
        when(todoRepository.save(todo)).thenReturn(todo);

        todoService.deleteTodo(1L, 10L);

        assertTrue(todo.isDeleted());
        assertNotNull(todo.getDeletedAt());
        verify(todoEventPublisher).publishActionLog(todo, user, TodoAction.DELETED, "删除 Todo");
    }

    @Test
    void shouldRestoreSoftDeletedTodo() {
        AppUser user = user(1L);
        Todo todo = todo(10L, 1L, "已删除任务");
        todo.setUser(user);
        todo.setDeleted(true);
        todo.setDeletedAt(java.time.LocalDateTime.now());
        when(todoRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(todo));
        when(todoRepository.save(todo)).thenReturn(todo);

        todoService.restoreTodo(1L, 10L);

        assertFalse(todo.isDeleted());
        assertNull(todo.getDeletedAt());
        verify(todoEventPublisher).publishActionLog(todo, user, TodoAction.RESTORED, "恢复 Todo");
    }

    @Test
    void shouldReadTodoLogsOnlyForOwner() {
        Todo todo = todo(10L, 1L, "有日志的任务");
        List<com.zading.todoapi.model.TodoActionLog> expected = List.of();
        when(todoRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(todo));
        when(todoActionLogRepository.findByTodoIdAndUserIdOrderByCreatedAtAscIdAsc(10L, 1L)).thenReturn(expected);

        List<com.zading.todoapi.model.TodoActionLog> actual = todoService.getTodoLogs(1L, 10L);

        assertSame(expected, actual);
        verify(todoActionLogRepository).findByTodoIdAndUserIdOrderByCreatedAtAscIdAsc(10L, 1L);
    }

    private AppUser user(Long id) {
        AppUser user = new AppUser("user-" + id, "hashed-password");
        user.setId(id);
        return user;
    }

    private Todo todo(Long id, Long userId, String title) {
        Todo todo = new Todo(id, title, false);
        todo.setUser(user(userId));
        todo.setPriority(TodoPriority.MEDIUM);
        return todo;
    }
}
