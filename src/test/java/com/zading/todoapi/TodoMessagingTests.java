package com.zading.todoapi;

import com.zading.todoapi.config.properties.RedisProtectionProperties;
import com.zading.todoapi.messaging.TodoActionLogMessage;
import com.zading.todoapi.messaging.TodoActionLogMessageHandler;
import com.zading.todoapi.model.TodoAction;
import com.zading.todoapi.redis.IdempotencyClaim;
import com.zading.todoapi.redis.IdempotencyStore;
import com.zading.todoapi.service.TodoActionLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoMessagingTests {
    @Mock
    private TodoActionLogService todoActionLogService;

    @Mock
    private IdempotencyStore idempotencyStore;

    private final RedisProtectionProperties redisProperties = new RedisProtectionProperties(
            Duration.ofMinutes(5),
            Duration.ofMinutes(10),
            Duration.ofMinutes(1),
            5,
            30
    );

    private TodoActionLogMessageHandler messageHandler;

    @BeforeEach
    void setUp() {
        messageHandler = new TodoActionLogMessageHandler(
                todoActionLogService,
                idempotencyStore,
                redisProperties
        );
    }

    @Test
    void shouldProcessMessageAndMarkItAsAcked() {
        TodoActionLogMessage message = message();
        when(idempotencyStore.tryClaim(any(), eq(Duration.ofMinutes(10))))
                .thenReturn(IdempotencyClaim.claimed("consumer-001"));
        when(idempotencyStore.complete(any(), eq("consumer-001"), eq("ACKED"), eq(Duration.ofMinutes(10))))
                .thenReturn(true);

        messageHandler.handle(message);

        verify(todoActionLogService).record(message.toEvent());
        verify(idempotencyStore).complete(any(), eq("consumer-001"), eq("ACKED"), eq(Duration.ofMinutes(10)));
    }

    @Test
    void shouldIgnoreMessageWhenItWasAlreadyCompleted() {
        TodoActionLogMessage message = message();
        when(idempotencyStore.tryClaim(any(), any()))
                .thenReturn(IdempotencyClaim.completed("ACKED"));

        messageHandler.handle(message);

        verify(todoActionLogService, never()).record(any());
        verify(idempotencyStore, never()).complete(any(), any(), any(), any());
    }

    @Test
    void shouldReleaseMessageClaimWhenBusinessHandlingFails() {
        TodoActionLogMessage message = message();
        when(idempotencyStore.tryClaim(any(), any()))
                .thenReturn(IdempotencyClaim.claimed("consumer-002"));
        doThrow(new IllegalStateException("模拟消费者失败"))
                .when(todoActionLogService)
                .record(any());

        assertThrows(IllegalStateException.class, () -> messageHandler.handle(message));

        verify(idempotencyStore).release(any(), eq("consumer-002"));
        verify(idempotencyStore, never()).complete(any(), any(), any(), any());
    }

    private TodoActionLogMessage message() {
        return new TodoActionLogMessage(
                UUID.randomUUID(),
                10L,
                1L,
                TodoAction.CREATED,
                "创建 Todo",
                Instant.parse("2026-08-27T00:00:00Z")
        );
    }
}
