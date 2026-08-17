package com.zading.todoapi.exception;

public class TodoNotFoundException extends BusinessException {
    public TodoNotFoundException(Long id) {
        super(ErrorCode.TODO_NOT_FOUND, "Todo 不存在，id = " + id);
    }
}
