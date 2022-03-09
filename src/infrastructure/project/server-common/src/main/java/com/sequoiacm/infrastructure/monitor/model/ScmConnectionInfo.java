package com.sequoiacm.infrastructure.monitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScmConnectionInfo {

    @JsonProperty("connection_count")
    private Long connectionCount;

    public Long getConnectionCount() {
        return connectionCount;
    }

    public void setConnectionCount(Long connectionCount) {
        this.connectionCount = connectionCount;
    }

    @Override
    public String toString() {
        return "ScmConnectionInfo{" + "connectionCount=" + connectionCount + '}';
    }
}
