package com.chatweb.conversation.exception;

import com.chatweb.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class ConversationAccessDeniedException extends AppException {

    public ConversationAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
