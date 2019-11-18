package com.sequoiacm.cloud.gateway;

import com.netflix.zuul.exception.ZuulException;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.web.context.request.RequestAttributes;

import java.util.Map;

public class ErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(
            RequestAttributes requestAttributes, boolean includeStackTrace) {
        Map<String, Object> result = super.getErrorAttributes(requestAttributes, includeStackTrace);
        Throwable error = getError(requestAttributes);
        if (error instanceof ZuulException && error.getCause() != null) {
            Throwable cause = error.getCause();
            String original = (String) result.get("message");
            result.put("message", String.format("%s, %s: %s",
                    original, cause.getClass().getName(), cause.getMessage()));
        }
        return result;
    }
}
