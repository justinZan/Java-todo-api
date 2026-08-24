package com.zading.todoapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.zading.todoapi.support.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TodoAttachmentApiTests extends AbstractApiTest {
    @Test
    void shouldUploadListDownloadAndDeleteAttachment() throws Exception {
        String token = authClient.registerAndLogin("attachment_user", "123456");
        Long todoId = todoClient.createAndReadId(token, "带附件的 Todo");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "hello attachment".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/todos/{todoId}/attachments", todoId)
                        .file(file)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.todoId").value(todoId))
                .andExpect(jsonPath("$.data.originalFilename").value("note.txt"))
                .andExpect(jsonPath("$.data.contentType").value("text/plain"))
                .andExpect(jsonPath("$.data.fileSize").value(16))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andReturn();

        Long attachmentId = readAttachmentId(uploadResult);
        assertEquals(1, todoAttachmentRepository.count());

        mockMvc.perform(get("/api/todos/{todoId}/attachments", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(attachmentId))
                .andExpect(jsonPath("$.data[0].originalFilename").value("note.txt"));

        mockMvc.perform(get("/api/todos/{todoId}/attachments/{attachmentId}/download", todoId, attachmentId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/plain")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("note.txt")))
                .andExpect(content().string("hello attachment"));

        mockMvc.perform(delete("/api/todos/{todoId}/attachments/{attachmentId}", todoId, attachmentId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("删除附件成功"));

        assertEquals(0, todoAttachmentRepository.count());

        mockMvc.perform(get("/api/todos/{todoId}/attachments", todoId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/todos/{todoId}/attachments/{attachmentId}/download", todoId, attachmentId)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TODO_ATTACHMENT_NOT_FOUND"));
    }

    @Test
    void shouldRejectEmptyAttachment() throws Exception {
        String token = authClient.registerAndLogin("empty_file_user", "123456");
        Long todoId = todoClient.createAndReadId(token, "空文件 Todo");
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/todos/{todoId}/attachments", todoId)
                        .file(emptyFile)
                        .header("Authorization", authClient.bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("附件不能为空"));
    }

    @Test
    void shouldPreventUserFromAccessingOthersAttachments() throws Exception {
        String userAToken = authClient.registerAndLogin("attachment_user_a", "123456");
        Long userATodoId = todoClient.createAndReadId(userAToken, "用户 A 的 Todo");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "private.txt",
                "text/plain",
                "private content".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/todos/{todoId}/attachments", userATodoId)
                        .file(file)
                        .header("Authorization", authClient.bearer(userAToken)))
                .andExpect(status().isCreated())
                .andReturn();

        Long attachmentId = readAttachmentId(uploadResult);
        String userBToken = authClient.registerAndLogin("attachment_user_b", "123456");

        mockMvc.perform(get("/api/todos/{todoId}/attachments", userATodoId)
                        .header("Authorization", authClient.bearer(userBToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"));

        mockMvc.perform(get("/api/todos/{todoId}/attachments/{attachmentId}/download", userATodoId, attachmentId)
                        .header("Authorization", authClient.bearer(userBToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"));
    }

    private Long readAttachmentId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("id").asLong();
    }
}
