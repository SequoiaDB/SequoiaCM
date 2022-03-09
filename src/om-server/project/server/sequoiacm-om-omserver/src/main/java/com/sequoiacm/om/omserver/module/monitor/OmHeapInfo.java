package com.sequoiacm.om.omserver.module.monitor;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmHeapInfo {

    @JsonProperty("used")
    private long used;

    @JsonProperty("size")
    private long size;

    @JsonProperty("max")
    private long max;

    public long getUsed() {
        return used;
    }

    public void setUsed(long used) {
        this.used = used;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }
}
