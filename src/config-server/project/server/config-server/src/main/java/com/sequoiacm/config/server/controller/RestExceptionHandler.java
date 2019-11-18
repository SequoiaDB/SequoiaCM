package com.sequoiacm.config.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;

@ControllerAdvice
public class RestExceptionHandler extends RestExceptionHandlerBase {

    @Override
    protected com.sequoiacm.infrastructure.exception_handler.ExceptionBody convertToExceptionBody(
            Exception srcException) {
        if (!(srcException instanceof ScmConfigException)) {
            return null;
        }
        ScmConfigException e = (ScmConfigException) srcException;
        HttpStatus status;
        switch (e.getError()) {
            case INVALID_ARG:
            case NO_SUCH_CONFIG:
                status = HttpStatus.BAD_REQUEST;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ExceptionBody(status, e.getError().getErrorCode(),
                e.getError().getErrorDescription(), e.getMessage());
    }
}
