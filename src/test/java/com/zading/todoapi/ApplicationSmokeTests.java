package com.zading.todoapi;

import com.zading.todoapi.support.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.request-logging.enabled=true")
class ApplicationSmokeTests extends AbstractApiTest {
    @Test
    void shouldReturnHelloMessage() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Hello Spring Boot"));
    }
    @Test
    void shouldReturnRequestIdHeader() throws Exception {
        mockMvc.perform(get("/hello")
                        .header("X-Request-Id", "test-request-id-001"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-request-id-001"));
    }
}
