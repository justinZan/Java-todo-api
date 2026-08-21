package com.zading.todoapi.service;

import com.zading.todoapi.config.CacheNames;
import com.zading.todoapi.event.TodoEventPublisher;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoAction;
import com.zading.todoapi.repository.TodoActionLogRepository;
import com.zading.todoapi.repository.TodoRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class TodoOverdueService {
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final TodoRepository todoRepository;
    private final TodoActionLogRepository todoActionLogRepository;
    private final TodoEventPublisher todoEventPublisher;
    private final CacheManager cacheManager;
    private final TodoOverdueJobStatusService todoOverdueJobStatusService;

    public TodoOverdueService(
            TodoRepository todoRepository,
            TodoActionLogRepository todoActionLogRepository,
            TodoEventPublisher todoEventPublisher,
            CacheManager cacheManager,
            TodoOverdueJobStatusService todoOverdueJobStatusService
    ) {
        this.todoRepository = todoRepository;
        this.todoActionLogRepository = todoActionLogRepository;
        this.todoEventPublisher = todoEventPublisher;
        this.cacheManager = cacheManager;
        this.todoOverdueJobStatusService = todoOverdueJobStatusService;
    }

    @Transactional
    public int recordOverdueTodos(LocalDate today, int pageSize) {
        long start = System.nanoTime();

        try {
            int recordedCount = doRecordOverdueTodos(today, pageSize);
            todoOverdueJobStatusService.recordSuccess(today, recordedCount, elapsedMs(start));
            return recordedCount;
        } catch (Exception ex) {
            todoOverdueJobStatusService.recordFailure(today, elapsedMs(start), ex);
            throw ex;
        }
    }

    private int doRecordOverdueTodos(LocalDate today, int pageSize) {
        int normalizedPageSize = normalizePageSize(pageSize);
        Pageable pageable = PageRequest.of(0, normalizedPageSize, Sort.by("id").ascending());
        int recordedCount = 0;
        Page<Todo> page;

        do {
            page = todoRepository.findByCompletedFalseAndDeletedFalseAndDueDateBefore(today, pageable);

            for (Todo todo : page.getContent()) {
                if (hasOverdueLog(todo)) {
                    continue;
                }

                todoEventPublisher.publishActionLog(todo, todo.getUser(), TodoAction.OVERDUE, "Todo 已过期");
                evictTodoLogsCache(todo);
                recordedCount++;
            }

            pageable = page.nextPageable();
        } while (page.hasNext());

        return recordedCount;
    }

    private long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return pageSize;
    }

    private boolean hasOverdueLog(Todo todo) {
        return todoActionLogRepository.existsByTodoIdAndAction(todo.getId(), TodoAction.OVERDUE);
    }

    private void evictTodoLogsCache(Todo todo) {
        Cache cache = cacheManager.getCache(CacheNames.TODO_LOGS);

        if (cache != null) {
            cache.evict(todo.getUser().getId() + ":" + todo.getId());
        }
    }
}
