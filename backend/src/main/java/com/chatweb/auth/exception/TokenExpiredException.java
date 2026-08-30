package com.chatweb.auth.exception;

import com.chatweb.common.exception.AppException;
import org.springframework.http.HttpStatus;

public class TokenExpiredException extends AppException {

    public TokenExpiredException(String message) {
        super(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", message);
    }

    public TokenExpiredException() {
        super(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "The provided token has expired");
    }
}
