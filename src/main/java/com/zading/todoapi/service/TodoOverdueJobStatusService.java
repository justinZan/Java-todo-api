package com.zading.todoapi.service;

import com.zading.todoapi.job.TodoOverdueJobStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TodoOverdueJobStatusService {
    private final AtomicReference<TodoOverdueJobStatus> status = new AtomicReference<>(TodoOverdueJobStatus.neverRun());

    public TodoOverdueJobStatus getStatus() {
        return status.get();
    }

    public void recordSuccess(LocalDate runDate, int processedCount, long durationMs) {
        status.set(new TodoOverdueJobStatus(
                "todo-overdue",
                runDate,
                LocalDateTime.now(),
                true,
                processedCount,
                durationMs,
                null
        ));
    }

    public void recordFailure(LocalDate runDate, long durationMs, Exception exception) {
        status.set(new TodoOverdueJobStatus(
                "todo-overdue",
                runDate,
                LocalDateTime.now(),
                false,
                0,
                durationMs,
                exception.getMessage()
        ));
    }
}
