package com.sequoiacm.cloud.servicecenter.controller;

import com.sequoiacm.cloud.servicecenter.exception.ScmServiceCenterException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;

@ControllerAdvice
public class ExceptionHandler extends RestExceptionHandlerBase {

    @Override
    protected ExceptionBody convertToExceptionBody(Exception srcException) {
        if (!(srcException instanceof ScmServiceCenterException)) {
            return null;
        }
        ScmServiceCenterException e = (ScmServiceCenterException) srcException;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return new ExceptionBody(status, status.value(), e.getCode(), e.getMessage());
    }

}
