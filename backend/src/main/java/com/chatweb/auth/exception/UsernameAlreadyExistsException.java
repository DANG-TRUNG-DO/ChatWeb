package com.chatweb.auth.exception;

import com.chatweb.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends AppException {

    public UsernameAlreadyExistsException(String username) {
        super(
            HttpStatus.CONFLICT,
            "USERNAME_ALREADY_EXISTS",
            String.format("Username '%s' is already taken", username)
        );
    }
}
