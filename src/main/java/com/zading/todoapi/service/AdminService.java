package com.zading.todoapi.service;

import com.zading.todoapi.dto.AdminStatisticsResponse;
import com.zading.todoapi.dto.AdminTodoResponse;
import com.zading.todoapi.dto.AdminUserResponse;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.repository.TodoRepository;
import com.zading.todoapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    public AdminService(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        ));
    }

    @Transactional(readOnly = true)
    public Page<AdminTodoResponse> getTodos(boolean includeDeleted, Pageable pageable) {
        Page<Todo> todos = includeDeleted
                ? todoRepository.findAll(pageable)
                : todoRepository.findByDeletedFalse(pageable);

        return todos.map(todo -> new AdminTodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.isCompleted(),
                todo.isDeleted(),
                todo.getPriority(),
                todo.getDueDate(),
                todo.getCompletedAt(),
                todo.getDeletedAt(),
                todo.getCreatedAt(),
                todo.getUpdatedAt(),
                todo.getUser().getId(),
                todo.getUser().getUsername()
        ));
    }

    @Transactional(readOnly = true)
    public AdminStatisticsResponse getStatistics() {
        return new AdminStatisticsResponse(
                userRepository.count(),
                todoRepository.count(),
                todoRepository.countByDeletedFalse(),
                todoRepository.countByDeletedFalseAndCompletedTrue(),
                todoRepository.countByDeletedFalseAndCompletedFalse(),
                todoRepository.countByDeletedTrue()
        );
    }
}
