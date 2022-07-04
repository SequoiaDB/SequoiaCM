package com.sequoiacm.cloud.gateway.forward;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sequoiacm.cloud.gateway.forward.decider.S3ForwardDecider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class S3ForwardExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(S3ForwardExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public void handle(Exception e, HttpServletRequest req, HttpServletResponse response)
            throws Exception {
        if (req.getAttribute(S3ForwardDecider.S3_REQUEST_MARKER_ATTRIBUTE) == null) {
            throw e;
        }
        logger.error("failed to forward s3 request, request:{}", req.getRequestURI(), e);
        ObjectMapper objectMapper = new XmlMapper();
        Error error;
        if (e instanceof IllegalArgumentException) {
            error = new Error("InvalidArgument", e.getMessage(), req.getRequestURI());
            response.setStatus(400);
        }
        else {
            response.setStatus(500);
            error = new Error("InternalError", e.getMessage(), req.getRequestURI());
        }
        response.getOutputStream()
                .write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(error));
    }
}

class Error {
    public final static String JSON_CODE = "Code";
    public final static String JSON_MESSAGE = "Message";
    public final static String JSON_RESOURCE = "Resource";
    // public final static String JSON_REQUESTID = "RequestId";

    @JsonProperty(JSON_CODE)
    private String code;
    @JsonProperty(JSON_MESSAGE)
    private String message;
    @JsonProperty(JSON_RESOURCE)
    private String resource;
    // private Long requestId;

    public Error(String code, String message, String resource) {
        this.code = code;
        this.message = message;
        this.resource = resource;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    // public long getRequestId() { return requestId; }

    // public void setRequestId(Long requestId) { this.requestId = requestId; }

    @Override
    public String toString() {
        return String.format("{\"Code\":%s,\"Message\":\"%s\",\"Resource\":\"%s\"}", code, message,
                resource);
    }
}
