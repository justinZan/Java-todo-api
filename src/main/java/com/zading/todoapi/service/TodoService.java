package com.zading.todoapi.service;

import com.zading.todoapi.exception.TodoNotFoundException;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.repository.TodoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TodoService {
    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    public List<Todo> getTodos(Boolean completed, String keyword) {
        Sort sortById = Sort.by(Sort.Direction.ASC, "id");
        String normalizedKeyword = normalizeKeyword(keyword);

        if (completed != null && normalizedKeyword != null) {
            return todoRepository.findByCompletedAndTitleContainingIgnoreCase(completed, normalizedKeyword, sortById);
        }

        if (completed != null) {
            return todoRepository.findByCompleted(completed, sortById);
        }

        if (normalizedKeyword != null) {
            return todoRepository.findByTitleContainingIgnoreCase(normalizedKeyword, sortById);
        }

        return todoRepository.findAll(sortById);
    }

    public Todo getTodo(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    public Todo addTodo(String title) {
        String normalizedTitle = normalizeTitle(title);
        Todo todo = new Todo(null, normalizedTitle, false);
        return todoRepository.save(todo);
    }

    public Todo updateTodo(Long id, String title, Boolean completed) {
        Todo todo = getTodo(id);

        if (title != null) {
            todo.setTitle(normalizeTitle(title));
        }

        if (completed != null) {
            todo.setCompleted(completed);
        }

        return todoRepository.save(todo);
    }

    public Todo toggleTodo(Long id) {
        Todo todo = getTodo(id);
        todo.setCompleted(!todo.isCompleted());
        return todoRepository.save(todo);
    }

    public void deleteTodo(Long id) {
        if (!todoRepository.existsById(id)) {
            throw new TodoNotFoundException(id);
        }

        todoRepository.deleteById(id);
    }

    private String normalizeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("任务标题不能为空");
        }

        return title.trim();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        return keyword.trim();
    }
}
