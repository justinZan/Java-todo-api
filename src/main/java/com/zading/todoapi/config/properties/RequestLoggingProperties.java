package com.zading.todoapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.request-logging")
public record RequestLoggingProperties(
        boolean enabled,
        String requestIdHeader
) {
    public RequestLoggingProperties {
        if (requestIdHeader == null || requestIdHeader.isBlank()) {
            requestIdHeader = "X-Request-Id";
        }
    }
}
