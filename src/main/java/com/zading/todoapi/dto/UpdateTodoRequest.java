package com.zading.todoapi.dto;

import com.zading.todoapi.model.TodoPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateTodoRequest {
    @Pattern(regexp = ".*\\S.*", message = "任务标题不能为空")
    @Size(max = 100, message = "任务标题最多 100 个字符")
    private String title;
    private Boolean completed;
    private TodoPriority priority;

    @FutureOrPresent(message = "截止日期不能早于今天")
    private LocalDate dueDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
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
