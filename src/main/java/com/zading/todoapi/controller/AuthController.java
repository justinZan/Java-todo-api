package com.zading.todoapi.controller;

import com.zading.todoapi.dto.ApiResponse;
import com.zading.todoapi.dto.LoginRequest;
import com.zading.todoapi.dto.LoginResponse;
import com.zading.todoapi.dto.RegisterRequest;
import com.zading.todoapi.dto.UserResponse;
import com.zading.todoapi.config.properties.RedisProtectionProperties;
import com.zading.todoapi.exception.BusinessException;
import com.zading.todoapi.exception.ErrorCode;
import com.zading.todoapi.redis.RateLimitDecision;
import com.zading.todoapi.redis.RateLimiter;
import com.zading.todoapi.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final RateLimiter rateLimiter;
    private final RedisProtectionProperties redisProperties;

    public AuthController(
            AuthService authService,
            RateLimiter rateLimiter,
            RedisProtectionProperties redisProperties
    ) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.redisProperties = redisProperties;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.created(authService.register(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String key = "rate-limit:auth-login:" + request.getUsername().trim().toLowerCase(Locale.ROOT);
        RateLimitDecision decision = rateLimiter.tryAcquire(
                key,
                redisProperties.loginLimit(),
                redisProperties.rateLimitWindow()
        );

        if (!decision.allowed()) {
            throw new BusinessException(
                    ErrorCode.RATE_LIMIT_EXCEEDED,
                    "登录请求过于频繁，请在 " + decision.retryAfterSeconds() + " 秒后重试"
            );
        }

        return ApiResponse.success(authService.login(request.getUsername(), request.getPassword()));
    }
}
