package com.sequoiacm.s3.authoriztion;

import java.io.IOException;

import javax.servlet.ServletException;

import com.sequoiacm.s3.utils.RestUtils;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sequoiacm.s3.core.Error;
import com.sequoiacm.s3.exception.S3ServerException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class S3ContextValveBase extends ValveBase {
    private static final Logger logger = LoggerFactory.getLogger(S3ContextValveBase.class);

    @Autowired
    RestUtils restUtils;

    protected void handleS3Error(Request request, Response response, S3ServerException e)
            throws IOException {
        String msg = String.format("request=%s, errcode=%s, message=%s", request.getRequestURI(),
                e.getError().getCode(), e.getMessage());
        logger.error(msg, e);
        response.setStatus(e.getError().getHttpStatus());
        response.setStatus(restUtils.convertStatus(e).value());
        ObjectMapper objectMapper = new XmlMapper();
        Error exceptionBody = new Error(e, request.getRequestURI());
        response.getResponse().getOutputStream().write(
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(exceptionBody));
    }

    protected void invokeNext(Request request, Response response)
            throws IOException, ServletException {
        Valve next = getNext();
        if (next == null) {
            return;
        }
        next.invoke(request, response);
    }
}
