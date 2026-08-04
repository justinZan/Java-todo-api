package com.zading.todoapi;

import com.zading.todoapi.support.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TodoApiTests extends AbstractApiTest {
    @Test
    void shouldCreateListUpdateToggleAndDeleteTodo() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");

        MvcResult createResult = todoClient.create(token, Map.of(
                        "title", "学习 Spring Boot API",
                        "priority", "HIGH",
                        "dueDate", "2026-07-20"
                ))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("学习 Spring Boot API"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.dueDate").value("2026-07-20"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        Long todoId = todoClient.readId(createResult);

        mockMvc.perform(get("/api/todos")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(patch("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "学习 REST API",
                                "completed", true,
                                "priority", "LOW",
                                "dueDate", "2026-08-01"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("学习 REST API"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.dueDate").value("2026-08-01"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        mockMvc.perform(patch("/api/todos/{id}/toggle", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));

        mockMvc.perform(delete("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/todos")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldFilterTodosByCompletedAndKeyword() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");
        Long javaTodoId = todoClient.createAndReadId(token, "学习 Java JPA");

        todoClient.createAndReadId(token, "学习前端工程化");

        mockMvc.perform(patch("/api/todos/{id}/toggle", javaTodoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(get("/api/todos?completed=true")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("学习 Java JPA"));

        mockMvc.perform(get("/api/todos?keyword=前端")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("学习前端工程化"));
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
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].title").value("Charlie"))
                .andExpect(jsonPath("$.items[1].title").value("Bravo"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/todos?page=1&size=2&sort=title,desc")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Alpha"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void shouldReturnBadRequestWhenPageParametersAreInvalid() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");

        mockMvc.perform(get("/api/todos?page=-1")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("page: page 不能小于 0"));

        mockMvc.perform(get("/api/todos?size=101")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size: size 不能大于 100"));
    }

    @Test
    void shouldReturnBadRequestWhenSortIsInvalid() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");

        mockMvc.perform(get("/api/todos?sort=unknown,desc")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不支持的排序字段: unknown"));

        mockMvc.perform(get("/api/todos?sort=createdAt,sideways")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不支持的排序方向: sideways"));
    }

    @Test
    void shouldReturnDefaultPriorityWhenPriorityIsNotProvided() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");

        MvcResult result = todoClient.create(token, Map.of("title", "默认优先级"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.dueDate").value(nullValue()))
                .andReturn();

        Long todoId = todoClient.readId(result);

        mockMvc.perform(get("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.dueDate").value(nullValue()));
    }

    @Test
    void shouldReturnBadRequestWhenTitleIsBlank() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");

        todoClient.create(token, Map.of("title", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title: 任务标题不能为空"));
    }

    @Test
    void shouldReturnNotFoundWhenTodoDoesNotExist() throws Exception {
        String token = authClient.registerAndLogin("zading", "123456");

        mockMvc.perform(get("/api/todos/999")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Todo 不存在，id = 999"));
    }

    @Test
    void shouldRequireLoginForTodoApis() throws Exception {
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isUnauthorized())
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
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("用户 A 的任务"));

        mockMvc.perform(get("/api/todos")
                        .header("Authorization", authClient.bearer(userBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("用户 B 的任务"));

        mockMvc.perform(get("/api/todos/{id}", userATodoId)
                        .header("Authorization", authClient.bearer(userBToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Todo 不存在，id = " + userATodoId));
    }
}
