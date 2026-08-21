package com.zading.todoapi.job;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TodoOverdueJobStatus(
        String jobName,
        LocalDate lastRunDate,
        LocalDateTime lastRunAt,
        Boolean lastSuccess,
        Integer lastProcessedCount,
        Long lastDurationMs,
        String lastErrorMessage
) {
    public static TodoOverdueJobStatus neverRun() {
        return new TodoOverdueJobStatus(
                "todo-overdue",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
