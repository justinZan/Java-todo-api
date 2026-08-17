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
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.username").value("zading"))
                .andExpect(jsonPath("$.data.createdAt").exists());

        authClient.login("zading", "123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void shouldRejectDuplicateUsernameAndWrongPassword() throws Exception {
        authClient.register("zading", "123456")
                .andExpect(status().isCreated());

        authClient.register("zading", "abcdef")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"))
                .andExpect(jsonPath("$.message").value("用户名已存在"));

        authClient.login("zading", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }
}
