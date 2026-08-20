package com.zading.todoapi.event;

import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoAction;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class TodoEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public TodoEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishActionLog(Todo todo, AppUser user, TodoAction action, String description) {
        applicationEventPublisher.publishEvent(
                new TodoActionLogEvent(todo.getId(), user.getId(), action, description)
        );
    }
}
