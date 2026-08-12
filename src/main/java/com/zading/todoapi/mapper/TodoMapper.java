package com.zading.todoapi.mapper;

import com.zading.todoapi.dto.TodoResponse;
import com.zading.todoapi.model.Todo;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TodoMapper {
    public TodoResponse toResponse(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.isCompleted(),
                todo.isDeleted(),
                todo.getPriority(),
                todo.getDueDate(),
                todo.getCompletedAt(),
                todo.getDeletedAt(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }

    public List<TodoResponse> toResponseList(List<Todo> todos) {
        return todos.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TodoResponse> toResponseList(Page<Todo> todos) {
        return todos.getContent()
                .stream()
                .map(this::toResponse)
                .toList();
    }
}
