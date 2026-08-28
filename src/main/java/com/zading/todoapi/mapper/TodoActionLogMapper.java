package com.zading.todoapi.mapper;

import com.zading.todoapi.dto.TodoActionLogResponse;
import com.zading.todoapi.model.TodoActionLog;
import org.springframework.stereotype.Component;

import java.util.List;

/** 将操作日志 Entity 转换为不暴露数据库关系的 API 响应。 */
@Component
public class TodoActionLogMapper {
    public TodoActionLogResponse toResponse(TodoActionLog log) {
        return new TodoActionLogResponse(
                log.getId(),
                log.getAction(),
                log.getDescription(),
                log.getCreatedAt()
        );
    }

    public List<TodoActionLogResponse> toResponseList(List<TodoActionLog> logs) {
        return logs.stream()
                .map(this::toResponse)
                .toList();
    }
}
