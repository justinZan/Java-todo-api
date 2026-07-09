package com.zading.todoapi.dto;

import com.zading.todoapi.model.TodoPriority;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public class CreateTodoRequest {
    @NotBlank(message = "任务标题不能为空")
    private String title;
    private TodoPriority priority;
    private LocalDate dueDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TodoPriority getPriority() {
        return priority;
    }

    public void setPriority(TodoPriority priority) {
        this.priority = priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
