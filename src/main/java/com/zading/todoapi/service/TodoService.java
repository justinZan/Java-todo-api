package com.zading.todoapi.service;

import com.zading.todoapi.config.CacheNames;
import com.zading.todoapi.event.TodoEventPublisher;
import com.zading.todoapi.exception.TodoNotFoundException;
import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoAction;
import com.zading.todoapi.model.TodoActionLog;
import com.zading.todoapi.model.TodoPriority;
import com.zading.todoapi.repository.TodoActionLogRepository;
import com.zading.todoapi.repository.TodoRepository;
import com.zading.todoapi.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TodoService {
    private final TodoRepository todoRepository;
    private final TodoActionLogRepository todoActionLogRepository;
    private final UserRepository userRepository;
    private final TodoEventPublisher todoEventPublisher;

    public TodoService(
            TodoRepository todoRepository,
            TodoActionLogRepository todoActionLogRepository,
            UserRepository userRepository,
            TodoEventPublisher todoEventPublisher
    ) {
        this.todoRepository = todoRepository;
        this.todoActionLogRepository = todoActionLogRepository;
        this.userRepository = userRepository;
        this.todoEventPublisher = todoEventPublisher;
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.TODO_DETAIL, key = "#userId + ':' + #id")
    public Todo getTodo(Long userId, Long id) {
        return todoRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    @Transactional
    public Todo addTodo(Long userId, String title, TodoPriority priority, LocalDate dueDate) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
        String normalizedTitle = normalizeTitle(title);
        Todo todo = new Todo(null, normalizedTitle, false);
        todo.setUser(user);
        todo.setPriority(normalizePriority(priority));
        todo.setDueDate(dueDate);
        Todo savedTodo = todoRepository.save(todo);

        addActionLog(savedTodo, user, TodoAction.CREATED, "创建 Todo");

        return savedTodo;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.TODO_DETAIL, key = "#userId + ':' + #id"),
            @CacheEvict(cacheNames = CacheNames.TODO_LOGS, key = "#userId + ':' + #id")
    })
    public Todo updateTodo(Long userId, Long id, String title, Boolean completed, TodoPriority priority, LocalDate dueDate) {
        Todo todo = getTodo(userId, id);
        AppUser user = todo.getUser();
        boolean updatedFields = false;
        TodoAction completedAction = null;

        if (title != null) {
            todo.setTitle(normalizeTitle(title));
            updatedFields = true;
        }

        if (completed != null) {
            boolean oldCompleted = todo.isCompleted();
            applyCompleted(todo, completed);

            if (oldCompleted != completed) {
                completedAction = completed ? TodoAction.COMPLETED : TodoAction.UNCOMPLETED;
            }
        }

        if (priority != null) {
            todo.setPriority(priority);
            updatedFields = true;
        }

        if (dueDate != null) {
            todo.setDueDate(dueDate);
            updatedFields = true;
        }

        Todo savedTodo = todoRepository.save(todo);

        if (updatedFields) {
            addActionLog(savedTodo, user, TodoAction.UPDATED, "修改 Todo");
        }

        if (completedAction != null) {
            addActionLog(savedTodo, user, completedAction, getCompletedDescription(completedAction));
        }

        return savedTodo;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.TODO_DETAIL, key = "#userId + ':' + #id"),
            @CacheEvict(cacheNames = CacheNames.TODO_LOGS, key = "#userId + ':' + #id")
    })
    public Todo toggleTodo(Long userId, Long id) {
        Todo todo = getTodo(userId, id);
        AppUser user = todo.getUser();
        boolean nextCompleted = !todo.isCompleted();

        applyCompleted(todo, nextCompleted);
        Todo savedTodo = todoRepository.save(todo);
        TodoAction action = nextCompleted ? TodoAction.COMPLETED : TodoAction.UNCOMPLETED;

        addActionLog(savedTodo, user, action, getCompletedDescription(action));

        return savedTodo;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.TODO_DETAIL, key = "#userId + ':' + #id"),
            @CacheEvict(cacheNames = CacheNames.TODO_LOGS, key = "#userId + ':' + #id")
    })
    public void deleteTodo(Long userId, Long id) {
        Todo todo = getTodo(userId, id);
        AppUser user = todo.getUser();
        todo.setDeleted(true);
        todo.setDeletedAt(LocalDateTime.now());

        Todo savedTodo = todoRepository.save(todo);
        addActionLog(savedTodo, user, TodoAction.DELETED, "删除 Todo");
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.TODO_DETAIL, key = "#userId + ':' + #id"),
            @CacheEvict(cacheNames = CacheNames.TODO_LOGS, key = "#userId + ':' + #id")
    })
    public Todo restoreTodo(Long userId, Long id) {
        Todo todo = todoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TodoNotFoundException(id));
        AppUser user = todo.getUser();

        todo.setDeleted(false);
        todo.setDeletedAt(null);

        Todo savedTodo = todoRepository.save(todo);

        addActionLog(savedTodo, user, TodoAction.RESTORED, "恢复 Todo");

        return savedTodo;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.TODO_LOGS, key = "#userId + ':' + #id")
    public List<TodoActionLog> getTodoLogs(Long userId, Long id) {
        todoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TodoNotFoundException(id));

        return todoActionLogRepository.findByTodoIdAndUserIdOrderByCreatedAtAscIdAsc(id, userId);
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

    private void addActionLog(Todo todo, AppUser user, TodoAction action, String description) {
        todoEventPublisher.publishActionLog(todo, user, action, description);
    }

    private String getCompletedDescription(TodoAction action) {
        if (action == TodoAction.COMPLETED) {
            return "完成 Todo";
        }

        return "取消完成 Todo";
    }
}
