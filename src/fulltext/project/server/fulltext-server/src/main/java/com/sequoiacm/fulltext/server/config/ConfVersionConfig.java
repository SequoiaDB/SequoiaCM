package com.sequoiacm.fulltext.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.conf.version")
public class ConfVersionConfig {
    private long workspaceHeartbeat = 3 * 60 * 1000;
    private long siteHeartbeat = 3 * 60 * 1000;

    public long getWorkspaceHeartbeat() {
        return workspaceHeartbeat;
    }

    public void setWorkspaceHeartbeat(long workspaceHeartbeat) {
        this.workspaceHeartbeat = workspaceHeartbeat;
    }

    public long getSiteHeartbeat() {
        return siteHeartbeat;
    }

    public void setSiteHeartbeat(long siteHeartbeat) {
        this.siteHeartbeat = siteHeartbeat;
    }

}
