package com.zading.todoapi.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TodoTestClient {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public TodoTestClient(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public ResultActions create(String token, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/todos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    public Long createAndReadId(String token, String title) throws Exception {
        MvcResult result = create(token, Map.of("title", title))
                .andExpect(status().isCreated())
                .andReturn();

        return readId(result);
    }

    public Long readId(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);
        return json.get("id").asLong();
    }
}
