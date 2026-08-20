package com.zading.todoapi.event;

import com.zading.todoapi.config.AsyncConfig;
import com.zading.todoapi.service.TodoActionLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TodoActionLogEventListener {
    private final TodoActionLogService todoActionLogService;

    public TodoActionLogEventListener(TodoActionLogService todoActionLogService) {
        this.todoActionLogService = todoActionLogService;
    }

    @Async(AsyncConfig.TODO_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTodoActionLog(TodoActionLogEvent event) {
        todoActionLogService.record(event);
    }
}
