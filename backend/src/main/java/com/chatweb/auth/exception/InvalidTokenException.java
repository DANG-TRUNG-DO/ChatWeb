package com.chatweb.auth.exception;

import com.chatweb.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class InvalidTokenException extends AppException {

    public InvalidTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", message);
    }

    public InvalidTokenException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "The provided token is invalid or has been revoked");
    }
}
