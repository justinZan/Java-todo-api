package com.zading.todoapi.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.todo.overdue-job")
public record TodoOverdueJobProperties(
        boolean enabled,
        @NotBlank String cron,
        @NotBlank String zone,
        @Min(1) int pageSize
) {
}
