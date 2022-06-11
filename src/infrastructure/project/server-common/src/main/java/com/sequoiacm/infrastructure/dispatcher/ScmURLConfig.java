package com.sequoiacm.infrastructure.dispatcher;

import java.util.Map;

public class ScmURLConfig {

    private String url;
    private boolean doInput = true;
    private boolean doOutput = true;
    private int chunkedStreamingMode = 4096;
    private boolean useCaches = false;
    private String requestMethod = "GET";
    private int connectTimeout = 10000;
    private int readTimeout = 30000;
    private Map<String, String> requestProperties;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isDoInput() {
        return doInput;
    }

    public void setDoInput(boolean doInput) {
        this.doInput = doInput;
    }

    public boolean isDoOutput() {
        return doOutput;
    }

    public void setDoOutput(boolean doOutput) {
        this.doOutput = doOutput;
    }

    public int getChunkedStreamingMode() {
        return chunkedStreamingMode;
    }

    public void setChunkedStreamingMode(int chunkedStreamingMode) {
        this.chunkedStreamingMode = chunkedStreamingMode;
    }

    public boolean isUseCaches() {
        return useCaches;
    }

    public void setUseCaches(boolean useCaches) {
        this.useCaches = useCaches;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Map<String, String> getRequestProperties() {
        return requestProperties;
    }

    public void setRequestProperties(Map<String, String> requestProperties) {
        this.requestProperties = requestProperties;
    }
}
