package com.zading.todoapi.service;

import com.zading.todoapi.exception.TodoNotFoundException;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoPriority;
import com.zading.todoapi.repository.TodoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TodoService {
    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    public Page<Todo> getTodos(Boolean completed, String keyword, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);

        if (completed != null && normalizedKeyword != null) {
            return todoRepository.findByCompletedAndTitleContainingIgnoreCase(completed, normalizedKeyword, pageable);
        }

        if (completed != null) {
            return todoRepository.findByCompleted(completed, pageable);
        }

        if (normalizedKeyword != null) {
            return todoRepository.findByTitleContainingIgnoreCase(normalizedKeyword, pageable);
        }

        return todoRepository.findAll(pageable);
    }

    public Todo getTodo(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    public Todo addTodo(String title, TodoPriority priority, LocalDate dueDate) {
        String normalizedTitle = normalizeTitle(title);
        Todo todo = new Todo(null, normalizedTitle, false);
        todo.setPriority(normalizePriority(priority));
        todo.setDueDate(dueDate);
        return todoRepository.save(todo);
    }

    public Todo updateTodo(Long id, String title, Boolean completed, TodoPriority priority, LocalDate dueDate) {
        Todo todo = getTodo(id);

        if (title != null) {
            todo.setTitle(normalizeTitle(title));
        }

        if (completed != null) {
            todo.setCompleted(completed);
        }

        if (priority != null) {
            todo.setPriority(priority);
        }

        if (dueDate != null) {
            todo.setDueDate(dueDate);
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

    private TodoPriority normalizePriority(TodoPriority priority) {
        if (priority == null) {
            return TodoPriority.MEDIUM;
        }

        return priority;
    }
}
