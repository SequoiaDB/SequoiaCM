package com.sequoiacm.cloud.authentication.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends RestException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
