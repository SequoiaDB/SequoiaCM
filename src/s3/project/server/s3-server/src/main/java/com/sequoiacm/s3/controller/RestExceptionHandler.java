package com.sequoiacm.s3.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.exception_handler.RestExceptionHandlerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.exception_handler.ExceptionBody;
import com.sequoiacm.infrastructure.security.auth.RestField;
import com.sequoiacm.s3.core.Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.utils.RestUtils;
import org.springframework.web.method.HandlerMethod;

@ControllerAdvice
public class RestExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);
    private static final String ERROR_ATTRIBUTE = "X-S3-ERROR";
    @Autowired
    RestUtils restUtils;

    private RestExceptionHandlerBase scmProxyExceptionHandler = new RestExceptionHandlerBase() {
        @Override
        protected ExceptionBody convertToExceptionBody(Exception srcException) {
            if (srcException instanceof ScmServerException) {
                ScmServerException scmServerException = (ScmServerException) srcException;
                ExceptionBody body = new ExceptionBody(HttpStatus.INTERNAL_SERVER_ERROR,
                        scmServerException.getError().getErrorCode(),
                        scmServerException.getError().getErrorDescription(),
                        scmServerException.getMessage());
                return body;
            }
            return null;
        }
    };

    public ResponseEntity s3ServerException(S3ServerException e, HttpServletRequest request) {
        String msg = String.format("request=%s, errcode=%s, message=%s", request.getRequestURI(),
                e.getError().getCode(), e.getMessage());
        logger.error(msg, e);

        HttpStatus status = restUtils.convertStatus(e);

        Error exceptionBody = new Error(e, request.getRequestURI());
        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            return ResponseEntity.status(status).build();
        }
        else {
            return ResponseEntity.status(status).body(exceptionBody);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity exceptionHandle(Exception e, HttpServletRequest request,
            HttpServletResponse response, HandlerMethod m) throws Exception {
        S3Controller res = AnnotationUtils.findAnnotation(m.getBeanType(), S3Controller.class);
        if (res == null) {
            // 按 SCM 错误格式进行响应
            return scmProxyExceptionHandler.serverExceptionHandler(e, request, response);
        }
        // 按 S3 错误格式进行响应
        if (e instanceof S3ServerException) {
            return s3ServerException((S3ServerException) e, request);
        }

        String msg = String.format("request=%s", request.getRequestURI());
        logger.error(msg, e);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Error exceptionBody = new Error(e, request.getRequestURI());
        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            String error = exceptionBody.toString();
            response.setHeader(ERROR_ATTRIBUTE, error);
            return ResponseEntity.status(status).build();
        }
        else {
            return ResponseEntity.status(status).body(exceptionBody);
        }
    }
}