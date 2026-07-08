package com.zading.todoapi.repository;

import com.zading.todoapi.model.Todo;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByCompleted(boolean completed, Sort sort);

    List<Todo> findByTitleContainingIgnoreCase(String keyword, Sort sort);

    List<Todo> findByCompletedAndTitleContainingIgnoreCase(boolean completed, String keyword, Sort sort);
}
