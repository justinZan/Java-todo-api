package com.zading.todoapi;

import com.zading.todoapi.model.AppUser;
import com.zading.todoapi.model.UserRole;
import com.zading.todoapi.support.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RbacApiTests extends AbstractApiTest {
    @Test
    void shouldAssignUserRoleToNewlyRegisteredUser() throws Exception {
        authClient.register("normal-user", "123456")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("USER"));

        AppUser user = userRepository.findByUsername("normal-user").orElseThrow();
        assertEquals(UserRole.USER, user.getRole());
    }

    @Test
    void shouldReturnUnauthorizedWhenAdminApiHasNoToken() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/admin/statistics"));
    }

    @Test
    void shouldReturnForbiddenWhenNormalUserCallsAdminApi() throws Exception {
        String token = authClient.registerAndLogin("normal-user", "123456");

        mockMvc.perform(get("/api/admin/statistics")
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("没有权限访问该资源"));
    }

    @Test
    void shouldAllowAdminToReadUsersTodosAndStatistics() throws Exception {
        String normalUserToken = authClient.registerAndLogin("normal-user", "123456");
        todoClient.createAndReadId(normalUserToken, "普通用户的任务");
        String adminToken = registerAdminAndLogin("admin-user", "123456");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", authClient.bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.items[0].username").value("normal-user"))
                .andExpect(jsonPath("$.data.items[0].role").value("USER"))
                .andExpect(jsonPath("$.data.items[1].username").value("admin-user"))
                .andExpect(jsonPath("$.data.items[1].role").value("ADMIN"));

        mockMvc.perform(get("/api/admin/todos")
                        .header("Authorization", authClient.bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("普通用户的任务"))
                .andExpect(jsonPath("$.data.items[0].username").value("normal-user"));

        mockMvc.perform(get("/api/admin/statistics")
                        .header("Authorization", authClient.bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(2))
                .andExpect(jsonPath("$.data.totalTodos").value(1))
                .andExpect(jsonPath("$.data.activeTodos").value(1))
                .andExpect(jsonPath("$.data.completedTodos").value(0))
                .andExpect(jsonPath("$.data.pendingTodos").value(1))
                .andExpect(jsonPath("$.data.deletedTodos").value(0));
    }

    @Test
    void shouldAllowAdminToIncludeSoftDeletedTodos() throws Exception {
        String normalUserToken = authClient.registerAndLogin("normal-user", "123456");
        Long todoId = todoClient.createAndReadId(normalUserToken, "需要删除的任务");

        mockMvc.perform(delete("/api/todos/{id}", todoId)
                        .header("Authorization", authClient.bearer(normalUserToken)))
                .andExpect(status().isOk());

        String adminToken = registerAdminAndLogin("admin-user", "123456");

        mockMvc.perform(get("/api/admin/todos")
                        .param("includeDeleted", "true")
                        .header("Authorization", authClient.bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].deleted").value(true));
    }

    private String registerAdminAndLogin(String username, String password) throws Exception {
        authClient.register(username, password)
                .andExpect(status().isCreated());

        AppUser user = userRepository.findByUsername(username).orElseThrow();
        user.setRole(UserRole.ADMIN);
        userRepository.saveAndFlush(user);

        MvcResult loginResult = authClient.login(username, password)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data")
                .get("token")
                .asText();
    }
}
