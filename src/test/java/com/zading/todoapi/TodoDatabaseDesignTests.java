package com.zading.todoapi;

import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoPriority;
import com.zading.todoapi.repository.TodoRepository;
import com.zading.todoapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 第 25 周数据库设计实践：验证迁移脚本、数据库约束和乐观锁映射。
 */
@DataJpaTest
@ActiveProfiles("test")
class TodoDatabaseDesignTests {
    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateTodoWithInitialVersionAndIncrementVersionAfterUpdate() {
        AppUser user = userRepository.saveAndFlush(new AppUser("version-user", "encoded-password"));

        Todo todo = new Todo(null, "第一版标题", false);
        todo.setUser(user);
        todo.setPriority(TodoPriority.MEDIUM);

        Todo savedTodo = todoRepository.saveAndFlush(todo);

        assertEquals(0L, savedTodo.getVersion());

        savedTodo.setTitle("第二版标题");
        Todo updatedTodo = todoRepository.saveAndFlush(savedTodo);

        assertEquals(1L, updatedTodo.getVersion());
    }

    @Test
    void shouldCreateBusinessCheckConstraintsFromMigration() {
        Set<String> constraints = Set.copyOf(jdbcTemplate.queryForList(
                """
                select constraint_name
                from information_schema.table_constraints
                where constraint_type = 'CHECK'
                  and constraint_name in (
                      'CK_TODOS_TITLE_NOT_BLANK',
                      'CK_TODOS_PRIORITY',
                      'CK_USERS_ROLE',
                      'CK_TODO_ACTION_LOGS_ACTION',
                      'CK_TODO_ATTACHMENTS_FILE_SIZE'
                  )
                """,
                String.class
        ));

        assertEquals(Set.of(
                "CK_TODOS_TITLE_NOT_BLANK",
                "CK_TODOS_PRIORITY",
                "CK_USERS_ROLE",
                "CK_TODO_ACTION_LOGS_ACTION",
                "CK_TODO_ATTACHMENTS_FILE_SIZE"
        ), constraints);
    }

    @Test
    void shouldRejectBlankTitleAtDatabaseLevel() {
        AppUser user = userRepository.saveAndFlush(new AppUser("constraint-user", "encoded-password"));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                insert into todos (
                    title, completed, deleted, user_id, priority,
                    created_at, updated_at, version
                ) values (?, false, false, ?, 'MEDIUM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                """,
                "   ",
                user.getId()
        ));
    }

    @Test
    void shouldKeepTodoVersionColumnInDatabase() {
        Integer versionColumnCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.columns
                where table_name = 'TODOS'
                  and column_name = 'VERSION'
                """,
                Integer.class
        );

        assertTrue(versionColumnCount != null && versionColumnCount == 1);
    }
}
