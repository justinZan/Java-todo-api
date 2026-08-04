package com.zading.todoapi;

import com.zading.todoapi.support.AbstractApiTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationSmokeTests extends AbstractApiTest {
    @Test
    void shouldReturnHelloMessage() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Hello Spring Boot"));
    }
}
