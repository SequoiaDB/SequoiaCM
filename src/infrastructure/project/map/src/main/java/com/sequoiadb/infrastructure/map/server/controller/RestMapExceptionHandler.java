package com.sequoiadb.infrastructure.map.server.controller;

import org.springframework.http.HttpStatus;

import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiadb.infrastructure.map.ScmMapServerException;

public class RestMapExceptionHandler {
    public static ExceptionBody convertToExceptionBody(ScmMapServerException e) {
        HttpStatus status;
        switch (e.getError()) {
            case INVALID_ARGUMENT:
                status = HttpStatus.BAD_REQUEST;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ExceptionBody(status, e.getError().getErrorCode(),
                e.getError().getErrorDescription(), e.getMessage());
    }
}
