package com.zading.todoapi.job;

import com.zading.todoapi.config.properties.TodoOverdueJobProperties;
import com.zading.todoapi.config.properties.RedisProtectionProperties;
import com.zading.todoapi.redis.DistributedLock;
import com.zading.todoapi.redis.LockHandle;
import com.zading.todoapi.service.TodoOverdueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "app.todo.overdue-job", name = "enabled", havingValue = "true")
public class TodoOverdueJob {
    private static final String LOCK_KEY = "lock:todo-overdue-job";

    private static final Logger log = LoggerFactory.getLogger(TodoOverdueJob.class);

    private final TodoOverdueService todoOverdueService;
    private final TodoOverdueJobProperties properties;
    private final DistributedLock distributedLock;
    private final RedisProtectionProperties redisProperties;

    public TodoOverdueJob(
            TodoOverdueService todoOverdueService,
            TodoOverdueJobProperties properties,
            DistributedLock distributedLock,
            RedisProtectionProperties redisProperties
    ) {
        this.todoOverdueService = todoOverdueService;
        this.properties = properties;
        this.distributedLock = distributedLock;
        this.redisProperties = redisProperties;
    }

    @Scheduled(cron = "${app.todo.overdue-job.cron}", zone = "${app.todo.overdue-job.zone:Asia/Shanghai}")
    public void scanOverdueTodos() {
        Optional<LockHandle> lock = distributedLock.tryLock(
                LOCK_KEY,
                redisProperties.lockLease()
        );

        if (lock.isEmpty()) {
            log.info("跳过 Todo 过期扫描：其他实例正在执行");
            return;
        }

        try {
            scanWithLock();
        } finally {
            distributedLock.unlock(lock.get());
        }
    }

    private void scanWithLock() {
        LocalDate today = LocalDate.now(ZoneId.of(properties.zone()));
        int recordedCount = todoOverdueService.recordOverdueTodos(today, properties.pageSize());

        log.info("Todo 过期扫描完成，today={}, recordedCount={}", today, recordedCount);
    }
}
