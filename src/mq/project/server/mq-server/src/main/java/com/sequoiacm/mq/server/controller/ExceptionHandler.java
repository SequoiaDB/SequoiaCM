package com.sequoiacm.mq.server.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;

import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;
import com.sequoiacm.mq.core.exception.MqException;

@ControllerAdvice
public class ExceptionHandler extends RestExceptionHandlerBase {

    @Override
    protected ExceptionBody convertToExceptionBody(Exception srcException) {
        if (!(srcException instanceof MqException)) {
            return null;
        }
        MqException e = (MqException) srcException;
        return new ExceptionBody(e.getError().getErrorCode(), e.getError().toString(),
                e.getMessage());
    }

}
