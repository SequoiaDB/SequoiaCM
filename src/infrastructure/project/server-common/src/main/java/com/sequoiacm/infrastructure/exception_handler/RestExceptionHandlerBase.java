package com.sequoiacm.infrastructure.exception_handler;

import java.net.URLEncoder;
import java.nio.charset.UnsupportedCharsetException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequoiacm.infrastructure.security.auth.RestField;

public abstract class RestExceptionHandlerBase {
    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandlerBase.class);
    private static final String ERROR_ATTRIBUTE = "X-SCM-ERROR";
    private static final String ERROR_RESPONSE_HOST_INFO = "SCM-ERROR-HOST";
    private static final String ERROR_ATTRIBUTE_CHARSET = "X-SCM-ERROR-CHARSET";
    private static final String CHARSET_UTF8 = "UTF-8";

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ExceptionBody> serverExceptionHandler(Exception e,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setHeader(ERROR_RESPONSE_HOST_INFO,
                request.getLocalName() + ":" + request.getLocalPort());
        String sessionId = request.getHeader(RestField.SESSION_ATTRIBUTE);

        ExceptionBody exceptionBody = convertToExceptionBody(e);
        if (exceptionBody == null) {
            // unexpected exception
            String log = String.format("sessionId=%s, request=%s", sessionId,
                    request.getRequestURI());
            logger.error(log, e);
            throw e;
        }

        if (exceptionBody.isNeedSessionId() && sessionId != null && sessionId.length() > 0) {
            exceptionBody.setMessage("sessionId=" + sessionId + "," + exceptionBody.getMessage());
        }
        exceptionBody.setException(e.getClass().getName());
        exceptionBody.setPath(request.getRequestURI());

        String log = String.format("sessionId=%s, request=%s, errcode=%d, errcodeDesc=%s", sessionId,
                request.getRequestURI(), exceptionBody.getStatus(), exceptionBody.getError());
        logger.error(log, e);
        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            String exceptionMsg = exceptionBody.toJson();
            String charsetStr = request.getHeader(ERROR_ATTRIBUTE_CHARSET);
            if (charsetStr != null) {
                try {
                    exceptionMsg = URLEncoder.encode(exceptionMsg, charsetStr);
                }
                catch (UnsupportedCharsetException e1) {
                    charsetStr = CHARSET_UTF8;
                    exceptionMsg = URLEncoder.encode(exceptionMsg, charsetStr);
                }
                response.setHeader(ERROR_ATTRIBUTE_CHARSET, charsetStr);
            }
            response.setHeader(ERROR_ATTRIBUTE, exceptionMsg);
            return ResponseEntity.status(exceptionBody.getHttpStatus()).build();
        }
        else {
            return ResponseEntity.status(exceptionBody.getHttpStatus()).body(exceptionBody);
        }
    }

    // return null if the type of srcException is not recognized
    protected abstract ExceptionBody convertToExceptionBody(Exception srcException);
}
