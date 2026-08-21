package com.zading.todoapi.repository;

import com.zading.todoapi.model.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    Page<Todo> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    Page<Todo> findByUserIdAndCompletedAndDeletedFalse(Long userId, boolean completed, Pageable pageable);

    Page<Todo> findByUserIdAndTitleContainingIgnoreCaseAndDeletedFalse(Long userId, String keyword, Pageable pageable);

    Page<Todo> findByUserIdAndCompletedAndTitleContainingIgnoreCaseAndDeletedFalse(Long userId, boolean completed, String keyword, Pageable pageable);

    Optional<Todo> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    Optional<Todo> findByIdAndUserId(Long id, Long userId);

    Page<Todo> findByCompletedFalseAndDeletedFalseAndDueDateBefore(LocalDate dueDate, Pageable pageable);
}
