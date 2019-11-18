package com.sequoiacm.cloud.authentication.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends RestException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
