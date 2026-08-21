package com.zading.todoapi;

import com.zading.todoapi.support.AbstractApiTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActuatorTests extends AbstractApiTest {
    @Test
    void shouldExposeHealthInfoAndMetricsWithoutLogin() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("java-todo-api"))
                .andExpect(jsonPath("$.app.version").value("1.0.0"))
                .andExpect(jsonPath("$.app.java-version").value("21"));

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItem("jvm.memory.used")));
    }
}
