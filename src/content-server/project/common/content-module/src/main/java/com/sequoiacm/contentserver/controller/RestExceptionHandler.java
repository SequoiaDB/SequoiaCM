package com.sequoiacm.contentserver.controller;

import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.sequoiacm.contentserver.contentmodule.ContentModuleExcludeMarker;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.exception_handler.ExceptionInfo;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.server.controller.RestMapExceptionHandler;

@ContentModuleExcludeMarker
@ControllerAdvice
public class RestExceptionHandler extends RestExceptionHandlerBase {
    public static String EXTRA_INFO_HEADER = "X-SCM-EXTRA-ERROR-INFO";

    @Override
    protected ExceptionInfo covertToExceptionInfo(Exception srcException) {
        ExceptionInfo ret = new ExceptionInfo();

        ScmServerException e;
        if (!(srcException instanceof ScmServerException)) {
            if (!(srcException instanceof ScmMapServerException)) {
                return ret;
            }
            ScmMapServerException mapException = (ScmMapServerException) srcException;
            ret.setExceptionBody(RestMapExceptionHandler.convertToExceptionBody(mapException));
            return ret;
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
        ret.setExceptionBody(new ExceptionBody(status, e.getError().getErrorCode(),
                e.getError().getErrorDescription(), e.getMessage()));
        if (e.getExtraInfo() != null) {
            ret.setExtraExceptionHeader(
                    Collections.singletonMap(EXTRA_INFO_HEADER, e.getExtraInfo().toString()));
        }
        return ret;
    }
}
