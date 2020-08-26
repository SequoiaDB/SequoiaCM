package com.sequoiacm.schedule.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;

@ControllerAdvice
public class RestExceptionHandler extends RestExceptionHandlerBase {

    @Override
    protected ExceptionBody convertToExceptionBody(Exception srcException) {
        if (!(srcException instanceof ScheduleException)) {
            return null;
        }

        ScheduleException e = (ScheduleException) srcException;
        HttpStatus status;
        switch (e.getCode()) {
            case RestCommonDefine.ErrorCode.INVALID_AUTH_INFO:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case RestCommonDefine.ErrorCode.INVALID_ACCOUNT:
                status = HttpStatus.FORBIDDEN;
                break;
            case RestCommonDefine.ErrorCode.PERMISSION_DENIED:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case RestCommonDefine.ErrorCode.MISSING_ARGUMENT:
            case RestCommonDefine.ErrorCode.INVALID_ARGUMENT:
                status = HttpStatus.BAD_REQUEST;
                break;
            case RestCommonDefine.ErrorCode.WORKSPACE_NOT_EXISTS:
            case RestCommonDefine.ErrorCode.FILE_NOT_EXISTS:
            case RestCommonDefine.ErrorCode.RECORD_NOT_EXISTS:
            case RestCommonDefine.ErrorCode.SITE_NOT_EXISTS:
            case RestCommonDefine.ErrorCode.ROOT_SITE_NOT_EXISTS:
            case RestCommonDefine.ErrorCode.TASK_NOT_EXISTS:
                status = HttpStatus.NOT_FOUND;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ExceptionBody(status, status.value(), e.getCode(), e.getMessage());
    }
}
