package com.zading.todoapi.job;

import com.zading.todoapi.config.properties.RedisProtectionProperties;
import com.zading.todoapi.config.properties.TodoOverdueJobProperties;
import com.zading.todoapi.redis.DistributedLock;
import com.zading.todoapi.redis.LockHandle;
import com.zading.todoapi.service.TodoOverdueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoOverdueJobTest {
    @Mock
    private TodoOverdueService todoOverdueService;

    @Mock
    private DistributedLock distributedLock;

    private final TodoOverdueJobProperties jobProperties = new TodoOverdueJobProperties(
            true,
            "0 0 9 * * *",
            "Asia/Shanghai",
            50
    );

    private final RedisProtectionProperties redisProperties = new RedisProtectionProperties(
            Duration.ofMinutes(5),
            Duration.ofMinutes(10),
            Duration.ofMinutes(1),
            5,
            30
    );

    private TodoOverdueJob todoOverdueJob;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        todoOverdueJob = new TodoOverdueJob(
                todoOverdueService,
                jobProperties,
                distributedLock,
                redisProperties
        );
    }

    @Test
    void shouldSkipScanWhenAnotherInstanceOwnsTheLock() {
        when(distributedLock.tryLock("lock:todo-overdue-job", Duration.ofMinutes(5)))
                .thenReturn(Optional.empty());

        todoOverdueJob.scanOverdueTodos();

        verify(todoOverdueService, never()).recordOverdueTodos(any(), anyInt());
        verify(distributedLock, never()).unlock(any());
    }

    @Test
    void shouldReleaseLockAfterScanCompletes() {
        LockHandle handle = new LockHandle("lock:todo-overdue-job", "owner-001");
        Duration lease = Duration.ofMinutes(5);
        when(distributedLock.tryLock("lock:todo-overdue-job", lease)).thenReturn(Optional.of(handle));
        when(todoOverdueService.recordOverdueTodos(any(LocalDate.class), eq(50))).thenReturn(2);

        todoOverdueJob.scanOverdueTodos();

        verify(todoOverdueService).recordOverdueTodos(any(LocalDate.class), eq(50));
        verify(distributedLock).unlock(handle);
    }
}
