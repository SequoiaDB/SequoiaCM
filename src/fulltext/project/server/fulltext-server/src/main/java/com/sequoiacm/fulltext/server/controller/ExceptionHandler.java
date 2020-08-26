package com.sequoiacm.fulltext.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;

@ControllerAdvice
public class ExceptionHandler extends RestExceptionHandlerBase {

    @Override
    protected ExceptionBody convertToExceptionBody(Exception srcException) {
        if (!(srcException instanceof FullTextException)) {
            return null;
        }
        FullTextException e = (FullTextException) srcException;
        return new ExceptionBody(HttpStatus.INTERNAL_SERVER_ERROR, e.getError().getErrorCode(),
                e.getError().getErrorDescription(), e.getMessage());
    }

}
