package com.sequoiacm.cloud.authentication.controller;

import com.sequoiacm.exception.ScmServerException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sequoiacm.cloud.authentication.exception.RestException;
import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;

@RestControllerAdvice
public class RestExceptionHandler extends RestExceptionHandlerBase {

    @Override
    protected ExceptionBody convertToExceptionBody(Exception srcException) {
        ScmServerException serverException;
        if (!(srcException instanceof ScmServerException)) {
            if (!(srcException instanceof RestException)) {
                return null;
            }
            RestException e = (RestException) srcException;
            return new ExceptionBody(HttpStatus.valueOf(e.getStatus()), e.getStatus(), e.getError(),
                    e.getMessage());
        }
        serverException = (ScmServerException) srcException;
        HttpStatus status;
        switch (serverException.getError()) {
            case SALT_NOT_EXIST:
                status = HttpStatus.BAD_REQUEST;
                break;
            case FIND_SALT_FAILED:
                status = HttpStatus.BAD_REQUEST;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ExceptionBody(status, serverException.getError().getErrorCode(),
                serverException.getError().getErrorDescription(), serverException.getMessage());
    }
}
