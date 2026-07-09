package com.zading.todoapi.repository;

import com.zading.todoapi.model.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    Page<Todo> findByCompleted(boolean completed, Pageable pageable);

    Page<Todo> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Todo> findByCompletedAndTitleContainingIgnoreCase(boolean completed, String keyword, Pageable pageable);
}
