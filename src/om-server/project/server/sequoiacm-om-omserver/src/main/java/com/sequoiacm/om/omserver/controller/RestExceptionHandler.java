package com.sequoiacm.om.omserver.controller;

import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;

import java.util.List;

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

        if (srcException instanceof BindException) {
            BindException bindException = (BindException) srcException;
            BindingResult bindingResult = bindException.getBindingResult();
            if (bindingResult.hasErrors()) {
                List<ObjectError> allErrors = bindingResult.getAllErrors();
                if (!allErrors.isEmpty()) {
                    ObjectError fieldError = allErrors.get(0);
                    return new ExceptionBody(ScmOmServerError.INVALID_ARGUMENT.getErrorCode(),
                            ScmOmServerError.INVALID_ARGUMENT.getErrorDescription(),
                            fieldError.getDefaultMessage());
                }
            }
        }
        return null;
    }
}
