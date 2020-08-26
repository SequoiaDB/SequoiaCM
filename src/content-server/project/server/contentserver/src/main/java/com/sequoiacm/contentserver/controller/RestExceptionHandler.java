package com.sequoiacm.contentserver.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.server.controller.RestMapExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends RestExceptionHandlerBase {

    @Override
    protected ExceptionBody convertToExceptionBody(Exception srcException) {
        ScmServerException e;
        if (!(srcException instanceof ScmServerException)) {
            if (!(srcException instanceof ScmMapServerException)) {
                return null;
            }
            ScmMapServerException mapException = (ScmMapServerException) srcException;
            return RestMapExceptionHandler.convertToExceptionBody(mapException);
        }
        else {
            e = (ScmServerException) srcException;
        }
        HttpStatus status;
        switch (e.getError()) {
            case MISSING_ARGUMENT:
            case INVALID_ARGUMENT:
            case INVALID_ID:
                status = HttpStatus.BAD_REQUEST;
                break;
            case OPERATION_FORBIDDEN:
                status = HttpStatus.FORBIDDEN;
                break;
            case OPERATION_UNAUTHORIZED:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case FILE_NOT_FOUND:
            case DIR_NOT_FOUND:
            case FILE_NOT_EXIST:
            case SITE_NOT_EXIST:
            case TASK_NOT_EXIST:
                status = HttpStatus.NOT_FOUND;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ExceptionBody(status, e.getError().getErrorCode(),
                e.getError().getErrorDescription(), e.getMessage());
    }
}
