package com.zading.todoapi.repository;

import com.zading.todoapi.dto.TodoStatistics;
import com.zading.todoapi.model.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    Page<Todo> findByDeletedFalse(Pageable pageable);

    /**
     * 用一次聚合查询获取管理员统计，避免为每个数字分别访问数据库。
     */
    @Query("""
            select new com.zading.todoapi.dto.TodoStatistics(
                count(t.id),
                sum(case when t.deleted = false then 1L else 0L end),
                sum(case when t.deleted = false and t.completed = true then 1L else 0L end),
                sum(case when t.deleted = false and t.completed = false then 1L else 0L end),
                sum(case when t.deleted = true then 1L else 0L end)
            )
            from Todo t
            """)
    TodoStatistics getStatistics();

    Page<Todo> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    Page<Todo> findByUserIdAndCompletedAndDeletedFalse(Long userId, boolean completed, Pageable pageable);

    Page<Todo> findByUserIdAndTitleContainingIgnoreCaseAndDeletedFalse(Long userId, String keyword, Pageable pageable);

    Page<Todo> findByUserIdAndCompletedAndTitleContainingIgnoreCaseAndDeletedFalse(Long userId, boolean completed, String keyword, Pageable pageable);

    Optional<Todo> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    Optional<Todo> findByIdAndUserId(Long id, Long userId);

    Page<Todo> findByCompletedFalseAndDeletedFalseAndDueDateBefore(LocalDate dueDate, Pageable pageable);
}
