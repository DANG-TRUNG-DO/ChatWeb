package com.chatweb.message.exception;

import com.chatweb.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class InvalidMessageException extends AppException {

    public InvalidMessageException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_MESSAGE", message);
    }
}
