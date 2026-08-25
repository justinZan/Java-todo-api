package com.zading.todoapi;

import com.zading.todoapi.dto.TodoStatistics;
import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoPriority;
import com.zading.todoapi.repository.TodoRepository;
import com.zading.todoapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class TodoQueryOptimizationTests {
    private static final Set<String> TODO_QUERY_INDEXES = Set.of(
            "IDX_TODOS_USER_DELETED_ID",
            "IDX_TODOS_USER_DELETED_COMPLETED_ID",
            "IDX_TODOS_DELETED_COMPLETED_DUE_ID",
            "IDX_TODOS_DELETED_ID"
    );

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReadTodoStatisticsWithOneAggregateQuery() {
        AppUser user = userRepository.save(new AppUser("query-user", "encoded-password"));
        Todo activeCompleted = todo(user, "已完成", true, false);
        Todo activeIncomplete = todo(user, "未完成", false, false);
        Todo deleted = todo(user, "已删除", false, true);
        todoRepository.save(activeCompleted);
        todoRepository.save(activeIncomplete);
        todoRepository.save(deleted);

        TodoStatistics statistics = todoRepository.getStatistics();

        assertEquals(3L, statistics.totalCountOrZero());
        assertEquals(2L, statistics.activeCountOrZero());
        assertEquals(1L, statistics.completedCountOrZero());
        assertEquals(1L, statistics.incompleteCountOrZero());
        assertEquals(1L, statistics.deletedCountOrZero());
    }

    @Test
    void shouldCreateIndexesForTodoQueryPatterns() {
        Set<String> actualIndexes = Set.copyOf(jdbcTemplate.queryForList(
                """
                select index_name
                from information_schema.indexes
                where table_name = 'TODOS'
                """,
                String.class
        ));

        assertTrue(actualIndexes.containsAll(TODO_QUERY_INDEXES),
                () -> "缺少 Todo 查询索引，实际索引: " + actualIndexes);
    }

    @Test
    void shouldReturnExplainPlanForUserTodoListQuery() {
        String plan = jdbcTemplate.queryForObject(
                "EXPLAIN SELECT * FROM todos WHERE user_id = 1 AND deleted = FALSE ORDER BY id",
                String.class
        );

        assertTrue(plan != null && plan.contains("TODOS"));
    }

    private Todo todo(AppUser user, String title, boolean completed, boolean deleted) {
        Todo todo = new Todo(null, title, completed);
        todo.setUser(user);
        todo.setPriority(TodoPriority.MEDIUM);
        todo.setDeleted(deleted);
        return todo;
    }
}
