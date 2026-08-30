package com.chatweb.auth.exception;

import com.chatweb.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends AppException {

    public EmailAlreadyExistsException(String email) {
        super(
            HttpStatus.CONFLICT,
            "EMAIL_ALREADY_EXISTS",
            String.format("Email '%s' is already registered", email)
        );
    }
}
