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
import java.time.LocalDateTime;

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
            return todoRepository.findByUserIdAndCompletedAndTitleContainingIgnoreCaseAndDeletedFalse(userId, completed, normalizedKeyword, pageable);
        }

        if (completed != null) {
            return todoRepository.findByUserIdAndCompletedAndDeletedFalse(userId, completed, pageable);
        }

        if (normalizedKeyword != null) {
            return todoRepository.findByUserIdAndTitleContainingIgnoreCaseAndDeletedFalse(userId, normalizedKeyword, pageable);
        }

        return todoRepository.findByUserIdAndDeletedFalse(userId, pageable);
    }

    public Todo getTodo(Long userId, Long id) {
        return todoRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
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
            applyCompleted(todo, completed);
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
        applyCompleted(todo, !todo.isCompleted());
        return todoRepository.save(todo);
    }

    public void deleteTodo(Long userId, Long id) {
        Todo todo = getTodo(userId, id);
        todo.setDeleted(true);
        todo.setDeletedAt(LocalDateTime.now());

        todoRepository.save(todo);
    }

    public Todo restoreTodo(Long userId, Long id) {
        Todo todo = todoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TodoNotFoundException(id));

        todo.setDeleted(false);
        todo.setDeletedAt(null);

        return todoRepository.save(todo);
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

    private void applyCompleted(Todo todo, boolean completed) {
        todo.setCompleted(completed);

        if (completed) {
            todo.setCompletedAt(LocalDateTime.now());
        } else {
            todo.setCompletedAt(null);
        }
    }
}
