package com.zading.todoapi.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.file-storage")
public record FileStorageProperties(
        @NotBlank String rootLocation,
        @Min(1) long maxFileSizeBytes
) {
}
