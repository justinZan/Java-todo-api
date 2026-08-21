package com.zading.todoapi.controller;

import com.zading.todoapi.dto.ApiResponse;
import com.zading.todoapi.job.TodoOverdueJobStatus;
import com.zading.todoapi.service.TodoOverdueJobStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/jobs")
public class InternalJobController {
    private final TodoOverdueJobStatusService todoOverdueJobStatusService;

    public InternalJobController(TodoOverdueJobStatusService todoOverdueJobStatusService) {
        this.todoOverdueJobStatusService = todoOverdueJobStatusService;
    }

    @GetMapping("/todo-overdue")
    public ApiResponse<TodoOverdueJobStatus> getTodoOverdueJobStatus() {
        return ApiResponse.success(todoOverdueJobStatusService.getStatus());
    }
}
