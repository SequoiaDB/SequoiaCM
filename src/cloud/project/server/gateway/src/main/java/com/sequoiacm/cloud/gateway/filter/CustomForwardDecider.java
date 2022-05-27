package com.sequoiacm.cloud.gateway.filter;

import javax.servlet.http.HttpServletRequest;

public interface CustomForwardDecider {
    // 决定一个请求要不要走自实现的转发逻辑
    Decision decide(HttpServletRequest req);
}

class Decision {
    private boolean isCustomForward;
    private String serviceName;
    private String targetApi;
    private String defaultContentType;
    private boolean chunkedForward;

    public static Decision shouldForward(String serviceName, String targetApi,
                                         String contentType, boolean chunkedForward) {
        return new Decision(true, serviceName, targetApi, contentType, chunkedForward);
    }

    public static Decision shouldNotForward() {
        return new Decision(false, null, null, null,false);
    }

    private Decision(boolean isCustomForward, String serviceName, String targetApi,
            String contentType, boolean chunkedForward) {
        this.isCustomForward = isCustomForward;
        this.serviceName = serviceName;
        this.targetApi = targetApi;
        this.defaultContentType = contentType;
        this.chunkedForward = chunkedForward;
    }

    public boolean isCustomForward() {
        return isCustomForward;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getTargetApi() {
        return targetApi;
    }

    public String getDefaultContentType() {
        return defaultContentType;
    }

    public boolean isChunkedForward() {
        return chunkedForward;
    }
}