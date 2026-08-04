package com.zading.todoapi;

import com.zading.todoapi.support.AbstractApiTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTests extends AbstractApiTest {
    @Test
    void shouldRegisterAndLoginUser() throws Exception {
        authClient.register("zading", "123456")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("zading"))
                .andExpect(jsonPath("$.createdAt").exists());

        authClient.login("zading", "123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void shouldRejectDuplicateUsernameAndWrongPassword() throws Exception {
        authClient.register("zading", "123456")
                .andExpect(status().isCreated());

        authClient.register("zading", "abcdef")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名已存在"));

        authClient.login("zading", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }
}
