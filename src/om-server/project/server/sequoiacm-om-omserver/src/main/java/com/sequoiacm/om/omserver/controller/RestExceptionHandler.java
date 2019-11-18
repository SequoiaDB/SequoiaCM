package com.sequoiacm.om.omserver.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;

import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;

@ControllerAdvice
public class RestExceptionHandler extends RestExceptionHandlerBase {

    @Override
    protected ExceptionBody convertToExceptionBody(Exception srcException) {
        if (srcException instanceof ScmInternalException) {
            ScmInternalException scmInternalException = (ScmInternalException) srcException;
            return new ExceptionBody(scmInternalException.getErrorCode(), "none",
                    scmInternalException.getMessage());
        }

        if (srcException instanceof ScmOmServerException) {
            ScmOmServerException scmOmServerException = (ScmOmServerException) srcException;
            return new ExceptionBody(scmOmServerException.getError().getErrorCode(),
                    scmOmServerException.getError().getErrorDescription(),
                    scmOmServerException.getMessage());
        }
        return null;
    }
}
