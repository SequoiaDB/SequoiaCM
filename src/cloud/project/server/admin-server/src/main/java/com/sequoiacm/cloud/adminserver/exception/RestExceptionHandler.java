package com.sequoiacm.cloud.adminserver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;

@ControllerAdvice
public class RestExceptionHandler extends RestExceptionHandlerBase {
    @Override
    protected ExceptionBody convertToExceptionBody(Exception srcException) {
        if (!(srcException instanceof StatisticsException)) {
            return null;
        }

        StatisticsException e = (StatisticsException) srcException;

        HttpStatus status;
        switch (e.getCode()) {
            case StatisticsError.MISSING_ARGUMENT:
            case StatisticsError.INVALID_ARGUMENT:
                status = HttpStatus.BAD_REQUEST;
                break;
            case StatisticsError.WORKSPACE_NOT_EXISTS:
            case StatisticsError.RECORD_NOT_EXISTS:
                status = HttpStatus.NOT_FOUND;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ExceptionBody(status, status.value(), e.getCode(), e.getMessage());
    }
}
