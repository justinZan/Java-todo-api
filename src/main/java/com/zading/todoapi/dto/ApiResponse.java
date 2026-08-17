package com.zading.todoapi.dto;

import com.zading.todoapi.exception.ErrorCode;

public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
    private String path;

    public ApiResponse() {
    }

    private ApiResponse(boolean success, String code, String message, T data, String path) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.path = path;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", "成功", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "OK", message, data, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "CREATED", "创建成功", data, null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message, String path) {
        return new ApiResponse<>(false, errorCode.name(), message, null, path);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getPath() {
        return path;
    }
}
