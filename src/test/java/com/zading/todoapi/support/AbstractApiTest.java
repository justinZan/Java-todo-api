package com.zading.todoapi.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zading.todoapi.repository.TodoActionLogRepository;
import com.zading.todoapi.repository.TodoRepository;
import com.zading.todoapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class AbstractApiTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private TodoActionLogRepository todoActionLogRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    protected AuthTestClient authClient;
    protected TodoTestClient todoClient;

    @BeforeEach
    void setUpApiTest() {
        todoActionLogRepository.deleteAll();
        todoRepository.deleteAll();
        userRepository.deleteAll();
        authClient = new AuthTestClient(mockMvc, objectMapper);
        todoClient = new TodoTestClient(mockMvc, objectMapper);
    }
}
