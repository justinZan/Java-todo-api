package com.zading.todoapi.exception;

public class TodoAttachmentNotFoundException extends BusinessException {
    public TodoAttachmentNotFoundException(Long id) {
        super(ErrorCode.TODO_ATTACHMENT_NOT_FOUND, "Todo 附件不存在，id = " + id);
    }
}
