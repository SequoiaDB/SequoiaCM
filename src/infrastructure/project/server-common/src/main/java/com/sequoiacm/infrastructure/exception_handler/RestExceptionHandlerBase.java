package com.sequoiacm.infrastructure.exception_handler;

import java.net.URLEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sequoiacm.infrastructure.common.SecurityRestField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

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
        String sessionId = request.getHeader(SecurityRestField.SESSION_ATTRIBUTE);

        ExceptionInfo exceptionInfo = covertToExceptionInfo(e);
        ExceptionBody exceptionBody = exceptionInfo.getExceptionBody();
        if (exceptionBody == null) {
            // unexpected exception
            if (response.isCommitted()) {
                // 如果已经向客户端响应，之后触发了异常，为了防止错误码被写为 200，此时自己封装异常消息，不交给框架处理
                exceptionBody = createDefaultExceptionBody(e, response);
            }
            else {
                String log = String.format("sessionId=%s, request=%s", sessionId,
                        request.getRequestURI());
                logger.error(log, e);
                throw e;
            }
        }

        if (exceptionBody.isNeedSessionId() && sessionId != null && sessionId.length() > 0) {
            exceptionBody.setMessage("sessionId=" + sessionId + "," + exceptionBody.getMessage());
        }
        exceptionBody.setException(e.getClass().getName());
        exceptionBody.setPath(request.getRequestURI());

        String log = String.format("sessionId=%s, request=%s, errcode=%d, errcodeDesc=%s",
                sessionId, request.getRequestURI(), exceptionBody.getStatus(),
                exceptionBody.getError());
        logger.error(log, e);

        HttpHeaders exceptionInfoHeader = new HttpHeaders();
        if (exceptionInfo.getExtraExceptionHeader() != null) {
            for (Map.Entry<String, String> entry : exceptionInfo.getExtraExceptionHeader()
                    .entrySet()) {
                exceptionInfoHeader.add(entry.getKey(), entry.getValue());
            }
        }

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
            return ResponseEntity.status(exceptionBody.getHttpStatus()).headers(exceptionInfoHeader)
                    .build();
        }
        else {
            return ResponseEntity.status(exceptionBody.getHttpStatus()).headers(exceptionInfoHeader)
                    .body(exceptionBody);
        }
    }

    private ExceptionBody createDefaultExceptionBody(Exception e, HttpServletResponse response) {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        if (response.getStatus() != HttpStatus.OK.value()) {
            httpStatus = HttpStatus.valueOf(response.getStatus());
        }
        int errorCode = httpStatus.value();
        String errorDesc = httpStatus.getReasonPhrase();
        String message = e.getMessage();
        return new ExceptionBody(httpStatus, errorCode, errorDesc, message);
    }

    // 只有 body 信息的异常转码，重写这个函数即可
    // return null 表示交由父类来编码这个异常
    protected ExceptionBody convertToExceptionBody(Exception srcException) {
        return null;
    }

    // 需要将异常信息同时放到 body、header的转码，重写这个函数
    // 若返回值 ExceptionInfo 中的 body 为 null，则表示交由父类来编码这个异常到 body 中
    protected ExceptionInfo covertToExceptionInfo(Exception srcException) {
        return new ExceptionInfo(convertToExceptionBody(srcException),
                Collections.<String, String> emptyMap());
    }
}
