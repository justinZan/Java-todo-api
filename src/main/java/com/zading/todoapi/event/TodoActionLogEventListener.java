package com.zading.todoapi.event;

import com.zading.todoapi.config.AsyncConfig;
import com.zading.todoapi.messaging.TodoActionLogMessage;
import com.zading.todoapi.messaging.TodoMessagePublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TodoActionLogEventListener {
    private final TodoMessagePublisher todoMessagePublisher;

    public TodoActionLogEventListener(TodoMessagePublisher todoMessagePublisher) {
        this.todoMessagePublisher = todoMessagePublisher;
    }

    @Async(AsyncConfig.TODO_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTodoActionLog(TodoActionLogEvent event) {
        todoMessagePublisher.publish(TodoActionLogMessage.from(event));
    }
}
