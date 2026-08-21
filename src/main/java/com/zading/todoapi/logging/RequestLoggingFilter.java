package com.zading.todoapi.logging;

import com.zading.todoapi.config.properties.RequestLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final RequestLoggingProperties properties;

    public RequestLoggingFilter(RequestLoggingProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || request.getRequestURI().startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String requestId = resolveRequestId(request);
        MDC.put("requestId", requestId);
        response.setHeader(properties.requestIdHeader(), requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info(
                    "requestId={} HTTP {} {} -> {} ({} ms)",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration
            );
            MDC.remove("requestId");
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(properties.requestIdHeader());

        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }

        return requestId;
    }
}
