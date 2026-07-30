package com.zading.todoapi.service;

import com.zading.todoapi.exception.TodoNotFoundException;
import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoPriority;
import com.zading.todoapi.repository.TodoRepository;
import com.zading.todoapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TodoService {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    public TodoService(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    public Page<Todo> getTodos(Long userId, Boolean completed, String keyword, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);

        if (completed != null && normalizedKeyword != null) {
            return todoRepository.findByUserIdAndCompletedAndTitleContainingIgnoreCase(userId, completed, normalizedKeyword, pageable);
        }

        if (completed != null) {
            return todoRepository.findByUserIdAndCompleted(userId, completed, pageable);
        }

        if (normalizedKeyword != null) {
            return todoRepository.findByUserIdAndTitleContainingIgnoreCase(userId, normalizedKeyword, pageable);
        }

        return todoRepository.findByUserId(userId, pageable);
    }

    public Todo getTodo(Long userId, Long id) {
        return todoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    public Todo addTodo(Long userId, String title, TodoPriority priority, LocalDate dueDate) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
        String normalizedTitle = normalizeTitle(title);
        Todo todo = new Todo(null, normalizedTitle, false);
        todo.setUser(user);
        todo.setPriority(normalizePriority(priority));
        todo.setDueDate(dueDate);
        return todoRepository.save(todo);
    }

    public Todo updateTodo(Long userId, Long id, String title, Boolean completed, TodoPriority priority, LocalDate dueDate) {
        Todo todo = getTodo(userId, id);

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

    public Todo toggleTodo(Long userId, Long id) {
        Todo todo = getTodo(userId, id);
        todo.setCompleted(!todo.isCompleted());
        return todoRepository.save(todo);
    }

    public void deleteTodo(Long userId, Long id) {
        if (!todoRepository.existsByIdAndUserId(id, userId)) {
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
