package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmBatchOpResult {

    @JsonProperty("name")
    private String name;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    public OmBatchOpResult(String name, boolean success, String message) {
        this.name = name;
        this.success = success;
        this.message = message;
    }

    public OmBatchOpResult(String name, boolean success) {
        this.name = name;
        this.success = success;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "OmBatchOpResult{" + "name='" + name + '\'' + ", success=" + success + ", message='"
                + message + '\'' + '}';
    }
}
