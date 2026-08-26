package com.zading.todoapi.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "Todo 不存在"),
    TODO_ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Todo 附件不存在"),
    DUPLICATE_USERNAME(HttpStatus.BAD_REQUEST, "用户名已存在"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "用户名或密码错误"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "请先登录"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "没有权限访问该资源"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "请求参数校验失败"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "请求参数不正确"),
    CONCURRENT_UPDATE_CONFLICT(HttpStatus.CONFLICT, "数据已被其他请求修改，请刷新后重试"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
