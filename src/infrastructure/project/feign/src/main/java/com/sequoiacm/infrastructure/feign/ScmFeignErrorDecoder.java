package com.sequoiacm.infrastructure.feign;

import static java.lang.String.format;

import java.io.IOException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Response;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.ErrorDecoder;

public class ScmFeignErrorDecoder implements ErrorDecoder {
    private static final String ERROR_RESPONSE_HOST_INFO = "SCM-ERROR-HOST";
    private static final String ERROR_ATTRIBUTE = "X-SCM-ERROR";
    private static final Logger logger = LoggerFactory.getLogger(ScmFeignErrorDecoder.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private ScmFeignExceptionConverter exceptionConverter;

    public ScmFeignErrorDecoder() {
    }

    public ScmFeignErrorDecoder(ScmFeignExceptionConverter exceptionConverter) {
        this.exceptionConverter = exceptionConverter;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        String error;
        try {
            error = getErrorResponse(response);
        } catch (IOException e) {
            return e;
        }

        String errorHost = "unknown host";
        Collection<String> errorHostHeaders = response.headers().get(ERROR_RESPONSE_HOST_INFO);
        if (errorHostHeaders != null && errorHostHeaders.size() > 0) {
            errorHost = errorHostHeaders.iterator().next();
        }

        ScmFeignException scmFeignException;
        if (error == null) {
            String message = format("status %s reading %s, remoteHost: %s", response.status(),
                    methodKey, errorHost);
            scmFeignException = new ScmFeignException(
                    HttpStatus.valueOf(response.status()), message);
        } else {
            logger.error("methodKey={}, method={}, remoteHost={}, error={}", methodKey,
                    response.request().method(), errorHost, error);

            try {
                scmFeignException = mapper.readValue(error, ScmFeignException.class);
            } catch (Exception e) {
                String message = format("status %s reading %s, content:\n%s",
                        response.status(), methodKey, error);
                logger.warn("Failed to decode error response: " + message, e);
                return new DecodeException(message, e);
            }
        }

        if (exceptionConverter != null) {
            return exceptionConverter.convert(scmFeignException);
        } else {
            return scmFeignException;
        }
    }

    private String getErrorResponse(Response response) throws IOException {
        String error = null;

        if (response.body() != null) {
            error = Util.toString(response.body().asReader());
        } else if (response.headers() != null) {
            Collection<String> errors = response.headers().get(ERROR_ATTRIBUTE);
            if (errors != null && !errors.isEmpty()) {
                error = errors.iterator().next();
            }
        }

        return error;
    }
}
