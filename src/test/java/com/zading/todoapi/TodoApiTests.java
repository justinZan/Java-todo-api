package com.zading.todoapi;

import com.zading.todoapi.support.AbstractApiTest;
import com.zading.todoapi.config.CacheNames;
import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.model.Todo;
import com.zading.todoapi.model.TodoPriority;
import com.zading.todoapi.service.TodoOverdueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TodoApiTests extends AbstractApiTest {
    @Autowired
    private TodoOverdueService todoOverdueService;

    @Test
    void shouldCreateListUpdateToggleAndDeleteTodo() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");
        String createDueDate = LocalDate.now().plusDays(7).toString();
        String updateDueDate = LocalDate.now().plusDays(14).toString();

        MvcResult createResult = todoClient.create(token, Map.of(
                        "title", "学习 Spring Boot API",
                        "priority", "HIGH",
                        "dueDate", createDueDate
                ))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value("学习 Spring Boot API"))
                .andExpect(jsonPath("$.data.completed").value(false))
                .andExpect(jsonPath("$.data.deleted").value(false))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.dueDate").value(createDueDate))
                .andExpect(jsonPath("$.data.completedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.deletedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andReturn();

        Long todoId = todoClient.readId(createResult);

        mockMvc.perform(get("/api/todos")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));

        mockMvc.perform(patch("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "学习 REST API",
                                "completed", true,
                                "priority", "LOW",
                                "dueDate", updateDueDate
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("学习 REST API"))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.completedAt").value(notNullValue()))
                .andExpect(jsonPath("$.data.priority").value("LOW"))
                .andExpect(jsonPath("$.data.dueDate").value(updateDueDate))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());

        mockMvc.perform(patch("/api/todos/{id}/toggle", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(false))
                .andExpect(jsonPath("$.data.completedAt").value(nullValue()));

        mockMvc.perform(delete("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("删除成功"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        mockMvc.perform(get("/api/todos")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Todo 不存在，id = " + todoId));
    }

    @Test
    void shouldFilterTodosByCompletedAndKeyword() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");
        Long javaTodoId = todoClient.createAndReadId(token, "学习 Java JPA");

        todoClient.createAndReadId(token, "学习前端工程化");

        mockMvc.perform(patch("/api/todos/{id}/toggle", javaTodoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.completedAt").value(notNullValue()));

        mockMvc.perform(get("/api/todos?completed=true")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].title").value("学习 Java JPA"));

        mockMvc.perform(get("/api/todos?keyword=前端")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].title").value("学习前端工程化"));
    }

    @Test
    void shouldPageAndSortTodos() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");
        todoClient.createAndReadId(token, "Alpha");
        todoClient.createAndReadId(token, "Charlie");
        todoClient.createAndReadId(token, "Bravo");

        mockMvc.perform(get("/api/todos?page=0&size=2&sort=title,desc")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].title").value("Charlie"))
                .andExpect(jsonPath("$.data.items[1].title").value("Bravo"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false));

        mockMvc.perform(get("/api/todos?page=1&size=2&sort=title,desc")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].title").value("Alpha"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void shouldReturnBadRequestWhenPageParametersAreInvalid() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");

        mockMvc.perform(get("/api/todos?page=-1")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("page: page 不能小于 0"));

        mockMvc.perform(get("/api/todos?size=101")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("size: size 不能大于 100"));
    }

    @Test
    void shouldReturnBadRequestWhenSortIsInvalid() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");

        mockMvc.perform(get("/api/todos?sort=unknown,desc")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("不支持的排序字段: unknown"));

        mockMvc.perform(get("/api/todos?sort=createdAt,sideways")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("不支持的排序方向: sideways"));
    }

    @Test
    void shouldReturnDefaultPriorityWhenPriorityIsNotProvided() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");

        MvcResult result = todoClient.create(token, Map.of("title", "默认优先级"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.data.dueDate").value(nullValue()))
                .andReturn();

        Long todoId = todoClient.readId(result);

        mockMvc.perform(get("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.data.deleted").value(false))
                .andExpect(jsonPath("$.data.dueDate").value(nullValue()));
    }

    @Test
    void shouldRestoreSoftDeletedTodo() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");
        Long todoId = todoClient.createAndReadId(token, "可恢复的任务");

        mockMvc.perform(delete("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/todos")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)));

        mockMvc.perform(patch("/api/todos/{id}/restore", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("恢复成功"))
                .andExpect(jsonPath("$.data.title").value("可恢复的任务"))
                .andExpect(jsonPath("$.data.deleted").value(false))
                .andExpect(jsonPath("$.data.deletedAt").value(nullValue()));

        mockMvc.perform(get("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("可恢复的任务"))
                .andExpect(jsonPath("$.data.deleted").value(false));
    }

    @Test
    void shouldRecordTodoActionLogs() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");
        Long userId = userRepository.findByUsername("zading").orElseThrow().getId();
        Long todoId = todoClient.createAndReadId(token, "需要记录日志的任务");

        mockMvc.perform(patch("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "已经修改标题的任务"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/todos/{id}/toggle", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk());

        waitUntilActionLogCount(todoId, userId, 4);

        mockMvc.perform(get("/api/todos/{id}/logs", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].action").value("CREATED"))
                .andExpect(jsonPath("$.data[0].description").value("创建 Todo"))
                .andExpect(jsonPath("$.data[0].createdAt").exists())
                .andExpect(jsonPath("$.data[1].action").value("UPDATED"))
                .andExpect(jsonPath("$.data[1].description").value("修改 Todo"))
                .andExpect(jsonPath("$.data[2].action").value("COMPLETED"))
                .andExpect(jsonPath("$.data[2].description").value("完成 Todo"))
                .andExpect(jsonPath("$.data[3].action").value("DELETED"))
                .andExpect(jsonPath("$.data[3].description").value("删除 Todo"));

        mockMvc.perform(patch("/api/todos/{id}/restore", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk());

        waitUntilActionLogCount(todoId, userId, 5);

        mockMvc.perform(get("/api/todos/{id}/logs", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[4].action").value("RESTORED"))
                .andExpect(jsonPath("$.data[4].description").value("恢复 Todo"));
    }

    @Test
    void shouldCacheTodoDetailAndEvictWhenUpdated() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");
        Long userId = userRepository.findByUsername("zading").orElseThrow().getId();
        Long todoId = todoClient.createAndReadId(token, "缓存详情任务");
        String cacheKey = userId + ":" + todoId;
        Cache todoDetailCache = cacheManager.getCache(CacheNames.TODO_DETAIL);

        assertNotNull(todoDetailCache);
        assertNull(todoDetailCache.get(cacheKey));

        mockMvc.perform(get("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("缓存详情任务"));

        assertNotNull(todoDetailCache.get(cacheKey));

        mockMvc.perform(patch("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "缓存已失效的新标题"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("缓存已失效的新标题"));

        assertNull(todoDetailCache.get(cacheKey));
    }

    @Test
    void shouldCacheTodoLogsAndEvictWhenActionChanges() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");
        Long userId = userRepository.findByUsername("zading").orElseThrow().getId();
        Long todoId = todoClient.createAndReadId(token, "缓存日志任务");
        String cacheKey = userId + ":" + todoId;
        Cache todoLogsCache = cacheManager.getCache(CacheNames.TODO_LOGS);

        assertNotNull(todoLogsCache);
        assertNull(todoLogsCache.get(cacheKey));

        waitUntilActionLogCount(todoId, userId, 1);

        mockMvc.perform(get("/api/todos/{id}/logs", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].action").value("CREATED"));

        assertNotNull(todoLogsCache.get(cacheKey));

        mockMvc.perform(patch("/api/todos/{id}/toggle", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk());

        assertNull(todoLogsCache.get(cacheKey));
    }

    @Test
    void shouldRecordOverdueTodoActionLogByBatchJob() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");
        AppUser user = userRepository.findByUsername("zading").orElseThrow();

        Todo overdueTodo = saveTodoDirectly(user, "已经过期的任务", false, false, LocalDate.now().minusDays(1));
        saveTodoDirectly(user, "今天截止的任务", false, false, LocalDate.now());
        saveTodoDirectly(user, "未来截止的任务", false, false, LocalDate.now().plusDays(1));
        saveTodoDirectly(user, "已完成的过期任务", true, false, LocalDate.now().minusDays(1));
        saveTodoDirectly(user, "已删除的过期任务", false, true, LocalDate.now().minusDays(1));

        int recordedCount = todoOverdueService.recordOverdueTodos(LocalDate.now(), 2);

        assertEquals(1, recordedCount);
        waitUntilActionLogCount(overdueTodo.getId(), user.getId(), 1);

        mockMvc.perform(get("/api/todos/{id}/logs", overdueTodo.getId())
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].action").value("OVERDUE"))
                .andExpect(jsonPath("$.data[0].description").value("Todo 已过期"));

        mockMvc.perform(get("/api/internal/jobs/todo-overdue")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobName").value("todo-overdue"))
                .andExpect(jsonPath("$.data.lastRunDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.data.lastRunAt").exists())
                .andExpect(jsonPath("$.data.lastSuccess").value(true))
                .andExpect(jsonPath("$.data.lastProcessedCount").value(1))
                .andExpect(jsonPath("$.data.lastDurationMs").exists())
                .andExpect(jsonPath("$.data.lastErrorMessage").value(nullValue()));

        int repeatedRecordedCount = todoOverdueService.recordOverdueTodos(LocalDate.now(), 2);

        assertEquals(0, repeatedRecordedCount);
    }

    @Test
    void shouldNotReturnOtherUsersTodoActionLogs() throws Exception {
        String userAToken = authClient.registerAndLogin("user-a", "123456");
        String userBToken = authClient.registerAndLogin("user-b", "123456");
        Long userATodoId = todoClient.createAndReadId(userAToken, "用户 A 的日志任务");

        mockMvc.perform(get("/api/todos/{id}/logs", userATodoId)
                        .header("Authorization", authClient.bearer(userBToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Todo 不存在，id = " + userATodoId));
    }

    @Test
    void shouldClearCompletedAtWhenCompletedIsUpdatedToFalse() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");
        Long todoId = todoClient.createAndReadId(token, "完成状态一致性");

        mockMvc.perform(patch("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("completed", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.completedAt").value(notNullValue()));

        mockMvc.perform(patch("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("completed", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(false))
                .andExpect(jsonPath("$.data.completedAt").value(nullValue()));
    }

    @Test
    void shouldReturnBadRequestWhenTitleIsBlank() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");

        todoClient.create(token, Map.of("title", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("title: 任务标题不能为空"));
    }

    @Test
    void shouldReturnBadRequestWhenDueDateIsPast() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");

        todoClient.create(token, Map.of(
                        "title", "过期任务",
                        "dueDate", LocalDate.now().minusDays(1).toString()
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("dueDate: 截止日期不能早于今天"));
    }

    @Test
    void shouldReturnBadRequestWhenUpdatedTitleIsBlank() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");
        Long todoId = todoClient.createAndReadId(token, "原始标题");

        mockMvc.perform(patch("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("title: 任务标题不能为空"));
    }

    @Test
    void shouldReturnNotFoundWhenTodoDoesNotExist() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");

        mockMvc.perform(get("/api/todos/999")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Todo 不存在，id = 999"));
    }

    @Test
    void shouldRequireLoginForTodoApis() throws Exception {
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void shouldOnlyReturnCurrentUsersTodos() throws Exception {
        String userAToken = authClient.registerAndLogin("user-a", "123456");
        String userBToken = authClient.registerAndLogin("user-b", "123456");

        Long userATodoId = todoClient.createAndReadId(userAToken, "用户 A 的任务");
        todoClient.createAndReadId(userBToken, "用户 B 的任务");

        mockMvc.perform(get("/api/todos")
                        .header("Authorization", authClient.bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].title").value("用户 A 的任务"));

        mockMvc.perform(get("/api/todos")
                        .header("Authorization", authClient.bearer(userBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].title").value("用户 B 的任务"));

        mockMvc.perform(get("/api/todos/{id}", userATodoId)
                        .header("Authorization", authClient.bearer(userBToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Todo 不存在，id = " + userATodoId));
    }

    @Test
    void shouldNotRestoreOtherUsersTodo() throws Exception {
        String userAToken = authClient.registerAndLogin("user-a", "123456");
        String userBToken = authClient.registerAndLogin("user-b", "123456");
        Long userATodoId = todoClient.createAndReadId(userAToken, "用户 A 删除的任务");

        mockMvc.perform(delete("/api/todos/{id}", userATodoId)
                        .header("Authorization", authClient.bearer(userAToken)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/todos/{id}/restore", userATodoId)
                        .header("Authorization", authClient.bearer(userBToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Todo 不存在，id = " + userATodoId));
    }

    private Todo saveTodoDirectly(AppUser user, String title, boolean completed, boolean deleted, LocalDate dueDate) {
        Todo todo = new Todo(null, title, completed);
        todo.setUser(user);
        todo.setPriority(TodoPriority.MEDIUM);
        todo.setDueDate(dueDate);
        todo.setDeleted(deleted);

        return todoRepository.save(todo);
    }
}
