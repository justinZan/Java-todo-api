package com.zading.todoapi.service;

import com.zading.todoapi.event.TodoActionLogEvent;
import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoActionLog;
import com.zading.todoapi.repository.TodoActionLogRepository;
import com.zading.todoapi.repository.TodoRepository;
import com.zading.todoapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoActionLogService {
    private final TodoActionLogRepository todoActionLogRepository;
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    public TodoActionLogService(
            TodoActionLogRepository todoActionLogRepository,
            TodoRepository todoRepository,
            UserRepository userRepository
    ) {
        this.todoActionLogRepository = todoActionLogRepository;
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(TodoActionLogEvent event) {
        Todo todo = todoRepository.getReferenceById(event.todoId());
        AppUser user = userRepository.getReferenceById(event.userId());

        todoActionLogRepository.save(new TodoActionLog(
                todo,
                user,
                event.action(),
                event.description()
        ));
    }
}
