package com.sequoiacm.cloud.gateway;

import com.netflix.zuul.exception.ZuulException;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;

import java.util.Map;
import java.util.Objects;

public class ErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(RequestAttributes requestAttributes,
            boolean includeStackTrace) {
        Map<String, Object> result = super.getErrorAttributes(requestAttributes, includeStackTrace);
        Throwable error = getError(requestAttributes);
        if (error instanceof ZuulException && error.getCause() != null) {
            Throwable cause = error.getCause();
            String original = (String) result.get("message");
            result.put("message", String.format("%s, %s: %s", original, cause.getClass().getName(),
                    cause.getMessage()));
        }
        else if (is404Error(result)) {
            String serviceName = getServiceName(result);
            if (serviceName != null) {
                result.put("message", serviceName + " is not exits");
            }
        }
        return result;
    }

    private String getServiceName(Map<String, Object> result) {
        Object path = result.get("path");
        if (path == null) {
            return null;
        }
        String pathStr = String.valueOf(path);
        // pathStr=> 1:/; 2:/xxx; 3:/xxx/xxx
        if (pathStr.isEmpty() || pathStr.equals("/")) {
            return null;
        }
        if (pathStr.startsWith("/")) {
            pathStr = pathStr.substring(1);
        }
        int i = pathStr.indexOf("/");
        if (i != -1) {
            return pathStr.substring(0, i);
        }
        else {
            return pathStr;
        }
    }

    private boolean is404Error(Map<String, Object> result) {
        Object status = result.get("status");
        return Objects.equals(status, HttpStatus.NOT_FOUND.value());
    }

}
