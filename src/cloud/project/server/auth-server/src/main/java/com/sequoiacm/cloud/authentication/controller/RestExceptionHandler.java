package com.sequoiacm.cloud.authentication.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sequoiacm.cloud.authentication.exception.RestException;
import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;

@RestControllerAdvice
public class RestExceptionHandler extends RestExceptionHandlerBase {

    @Override
    protected ExceptionBody convertToExceptionBody(Exception srcException) {
        if (!(srcException instanceof RestException)) {
            return null;
        }
        RestException e = (RestException) srcException;
        return new ExceptionBody(HttpStatus.valueOf(e.getStatus()), e.getStatus(), e.getError(),
                e.getMessage());
    }
}
