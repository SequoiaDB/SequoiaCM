package com.sequoiacm.cloud.gateway;

import com.netflix.zuul.exception.ZuulException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;

import java.util.Map;
import java.util.Objects;

public class ErrorAttributes extends DefaultErrorAttributes {

    private static final Logger logger = LoggerFactory.getLogger(ErrorAttributes.class);

    private final DiscoveryClient discoveryClient;

    public ErrorAttributes(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

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
            // 后面会修改消息内容，先打印原始消息
            logger.warn("404 error: {}:", result);
            String serviceName = getServiceName(result);
            if (serviceName != null && serviceNotExist(serviceName)) {
                // 请求的服务不存在
                result.put("message", "Service does not exist, serviceName: " + serviceName);
            }
            else {
                // 其他类型的 404 错误，一般是路径含有非法参数
                result.put("message", "invalid request path:" + result.get("path"));
            }
        }
        return result;
    }

    private boolean serviceNotExist(String serviceName) {
        try {
            return discoveryClient.getInstances(serviceName).size() <= 0;
        }
        catch (Exception e) {
            return true;
        }
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
        String serviceName = null;
        int i = pathStr.indexOf("/");
        if (i != -1) {
            serviceName = pathStr.substring(0, i);
        }
        else {
            serviceName = pathStr;
        }
        if ("auth".equals(serviceName)) {
            // 认证服务映射的路径为 /auth/**
            serviceName = "auth-server";
        }
        return serviceName;
    }

    private boolean is404Error(Map<String, Object> result) {
        Object status = result.get("status");
        return Objects.equals(status, HttpStatus.NOT_FOUND.value());
    }

}
