package com.zading.todoapi;

import com.zading.todoapi.support.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
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

    @Test
    void shouldServeLearningHomepageWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Java Todo Lab")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"today\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"knowledge-list\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"source-code\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"source-note-list\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"config-list\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"specific-study-list\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"lab-step-list\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("id=\"timeline-list\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("id=\"rhythm\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"review-list\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"roadmap\"")));
    }
}
