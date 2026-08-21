package com.zading.todoapi.repository;

import com.zading.todoapi.model.TodoAction;
import com.zading.todoapi.model.TodoActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoActionLogRepository extends JpaRepository<TodoActionLog, Long> {
    List<TodoActionLog> findByTodoIdAndUserIdOrderByCreatedAtAscIdAsc(Long todoId, Long userId);

    long countByTodoIdAndUserId(Long todoId, Long userId);

    boolean existsByTodoIdAndAction(Long todoId, TodoAction action);
}
