package com.zading.todoapi.exception;

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(String message) {
        super(ErrorCode.INVALID_CREDENTIALS, message);
    }
}
