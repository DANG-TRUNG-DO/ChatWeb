package com.chatweb.message.exception;

import com.chatweb.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class MessageAccessDeniedException extends AppException {

    public MessageAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
