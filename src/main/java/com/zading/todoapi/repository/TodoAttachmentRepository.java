package com.zading.todoapi.repository;

import com.zading.todoapi.model.TodoAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TodoAttachmentRepository extends JpaRepository<TodoAttachment, Long> {
    List<TodoAttachment> findByTodoIdAndUserIdOrderByCreatedAtAscIdAsc(Long todoId, Long userId);

    Optional<TodoAttachment> findByIdAndTodoIdAndUserId(Long id, Long todoId, Long userId);
}
