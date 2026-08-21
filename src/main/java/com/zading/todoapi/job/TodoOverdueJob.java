package com.zading.todoapi.job;

import com.zading.todoapi.config.properties.TodoOverdueJobProperties;
import com.zading.todoapi.service.TodoOverdueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@ConditionalOnProperty(prefix = "app.todo.overdue-job", name = "enabled", havingValue = "true")
public class TodoOverdueJob {
    private static final Logger log = LoggerFactory.getLogger(TodoOverdueJob.class);

    private final TodoOverdueService todoOverdueService;
    private final TodoOverdueJobProperties properties;

    public TodoOverdueJob(
            TodoOverdueService todoOverdueService,
            TodoOverdueJobProperties properties
    ) {
        this.todoOverdueService = todoOverdueService;
        this.properties = properties;
    }

    @Scheduled(cron = "${app.todo.overdue-job.cron}", zone = "${app.todo.overdue-job.zone:Asia/Shanghai}")
    public void scanOverdueTodos() {
        LocalDate today = LocalDate.now(ZoneId.of(properties.zone()));
        int recordedCount = todoOverdueService.recordOverdueTodos(today, properties.pageSize());

        log.info("Todo 过期扫描完成，today={}, recordedCount={}", today, recordedCount);
    }
}
