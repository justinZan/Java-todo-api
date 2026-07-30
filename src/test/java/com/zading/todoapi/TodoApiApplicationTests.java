package com.zading.todoapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zading.todoapi.repository.TodoRepository;
import com.zading.todoapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TodoApiApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnHelloMessage() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Hello Spring Boot"));
    }

    @Test
    void shouldCreateListUpdateToggleAndDeleteTodo() throws Exception {
        String token = registerAndLogin("zading", "123456");
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "学习 Spring Boot API",
                "priority", "HIGH",
                "dueDate", "2026-07-20"
        ));

        MvcResult createResult = mockMvc.perform(post("/api/todos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
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

        Long todoId = readId(createResult);

        mockMvc.perform(get("/api/todos")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "title", "学习 REST API",
                "completed", true,
                "priority", "LOW",
                "dueDate", "2026-08-01"
        ));

        mockMvc.perform(patch("/api/todos/{id}", todoId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("学习 REST API"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.dueDate").value("2026-08-01"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        mockMvc.perform(patch("/api/todos/{id}/toggle", todoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));

        mockMvc.perform(delete("/api/todos/{id}", todoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/todos")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldFilterTodosByCompletedAndKeyword() throws Exception {
        String token = registerAndLogin("zading", "123456");
        Long javaTodoId = createTodo(token, "学习 Java JPA");

        createTodo(token, "学习前端工程化");

        mockMvc.perform(patch("/api/todos/{id}/toggle", javaTodoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(get("/api/todos?completed=true")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("学习 Java JPA"));

        mockMvc.perform(get("/api/todos?keyword=前端")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("学习前端工程化"));
    }

    @Test
    void shouldPageAndSortTodos() throws Exception {
        String token = registerAndLogin("zading", "123456");
        createTodo(token, "Alpha");
        createTodo(token, "Charlie");
        createTodo(token, "Bravo");

        mockMvc.perform(get("/api/todos?page=0&size=2&sort=title,desc")
                        .header("Authorization", bearer(token)))
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
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Alpha"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void shouldReturnBadRequestWhenPageParametersAreInvalid() throws Exception {
        String token = registerAndLogin("zading", "123456");

        mockMvc.perform(get("/api/todos?page=-1")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("page: page 不能小于 0"));

        mockMvc.perform(get("/api/todos?size=101")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size: size 不能大于 100"));
    }

    @Test
    void shouldReturnBadRequestWhenSortIsInvalid() throws Exception {
        String token = registerAndLogin("zading", "123456");

        mockMvc.perform(get("/api/todos?sort=unknown,desc")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不支持的排序字段: unknown"));

        mockMvc.perform(get("/api/todos?sort=createdAt,sideways")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不支持的排序方向: sideways"));
    }

    @Test
    void shouldReturnDefaultPriorityWhenPriorityIsNotProvided() throws Exception {
        String token = registerAndLogin("zading", "123456");

        MvcResult result = mockMvc.perform(post("/api/todos")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "默认优先级"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.dueDate").value(nullValue()))
                .andReturn();

        Long todoId = readId(result);

        mockMvc.perform(get("/api/todos/{id}", todoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.dueDate").value(nullValue()));
    }

    @Test
    void shouldReturnBadRequestWhenTitleIsBlank() throws Exception {
        String token = registerAndLogin("zading", "123456");
        String body = objectMapper.writeValueAsString(Map.of("title", " "));

        mockMvc.perform(post("/api/todos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title: 任务标题不能为空"));
    }

    @Test
    void shouldReturnNotFoundWhenTodoDoesNotExist() throws Exception {
        String token = registerAndLogin("zading", "123456");

        mockMvc.perform(get("/api/todos/999")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Todo 不存在，id = 999"));
    }

    @Test
    void shouldRegisterAndLoginUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "zading",
                                "password", "123456"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("zading"))
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "zading",
                                "password", "123456"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void shouldRejectDuplicateUsernameAndWrongPassword() throws Exception {
        register("zading", "123456")
                .andExpect(status().isCreated());

        register("zading", "abcdef")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名已存在"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "zading",
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void shouldRequireLoginForTodoApis() throws Exception {
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void shouldOnlyReturnCurrentUsersTodos() throws Exception {
        String userAToken = registerAndLogin("user-a", "123456");
        String userBToken = registerAndLogin("user-b", "123456");

        Long userATodoId = createTodo(userAToken, "用户 A 的任务");
        createTodo(userBToken, "用户 B 的任务");

        mockMvc.perform(get("/api/todos")
                        .header("Authorization", bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("用户 A 的任务"));

        mockMvc.perform(get("/api/todos")
                        .header("Authorization", bearer(userBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("用户 B 的任务"));

        mockMvc.perform(get("/api/todos/{id}", userATodoId)
                        .header("Authorization", bearer(userBToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Todo 不存在，id = " + userATodoId));
    }

    private Long createTodo(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/todos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", title))))
                .andExpect(status().isCreated())
                .andReturn();

        return readId(result);
    }

    private String registerAndLogin(String username, String password) throws Exception {
        register(username, password)
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token")
                .asText();
    }

    private org.springframework.test.web.servlet.ResultActions register(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", username,
                        "password", password
                ))));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Long readId(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);
        return json.get("id").asLong();
    }
}
