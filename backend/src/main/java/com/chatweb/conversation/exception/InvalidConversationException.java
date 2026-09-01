package com.chatweb.conversation.exception;

import com.chatweb.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class InvalidConversationException extends AppException {

    public InvalidConversationException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_CONVERSATION", message);
    }
}
