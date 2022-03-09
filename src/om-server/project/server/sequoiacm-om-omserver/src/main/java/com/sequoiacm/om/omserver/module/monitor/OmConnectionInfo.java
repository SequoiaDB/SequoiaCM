package com.sequoiacm.om.omserver.module.monitor;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OmConnectionInfo {

    @JsonProperty("connection_count")
    private Long connectionCount;

    public Long getConnectionCount() {
        return connectionCount;
    }

    public void setConnectionCount(Long connectionCount) {
        this.connectionCount = connectionCount;
    }
}
