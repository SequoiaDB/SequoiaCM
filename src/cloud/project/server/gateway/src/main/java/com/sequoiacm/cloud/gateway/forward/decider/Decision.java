package com.sequoiacm.cloud.gateway.forward.decider;

public class Decision {
    private boolean isCustomForward;
    private String serviceName;
    private String targetApi;
    private String defaultContentType;
    private boolean chunkedForward;
    private boolean setFrowardPrefix;

    public static Decision shouldCustomForward(String serviceName, String targetApi,
            String contentType, boolean chunkedForward, boolean setFrowardPrefix) {
        return new Decision(true, serviceName, targetApi, contentType, chunkedForward,
                setFrowardPrefix);
    }

    public static Decision unrecognized() {
        return null;
    }

    public static Decision shouldForward(String serviceName) {
        return new Decision(false, serviceName, null, null, false, false);
    }
    private Decision(boolean isCustomForward, String serviceName, String targetApi,
            String contentType, boolean chunkedForward, boolean setFrowardPrefix) {
        this.isCustomForward = isCustomForward;
        this.serviceName = serviceName;
        this.targetApi = targetApi;
        this.defaultContentType = contentType;
        this.chunkedForward = chunkedForward;
        this.setFrowardPrefix = setFrowardPrefix;
    }

    public boolean isSetFrowardPrefix() {
        return setFrowardPrefix;
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
