package com.sequoiacm.contentserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.conf.version")
public class ConfVersionConfig {
    private long workspaceHeartbeat = 3 * 60 * 1000;
    private long siteHeartbeat = 3 * 60 * 1000;
    private long nodeHeartbeat = 3 * 60 * 1000;
    private long metaDataHeartbeat = 3 * 60 * 1000;

    public long getMetaDataHeartbeat() {
        return metaDataHeartbeat;
    }

    public void setMetaDataHeartbeat(long metaDataHeartbeat) {
        this.metaDataHeartbeat = metaDataHeartbeat;
    }

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

    public long getNodeHeartbeat() {
        return nodeHeartbeat;
    }

    public void setNodeHeartbeat(long nodeHeartbeat) {
        this.nodeHeartbeat = nodeHeartbeat;
    }
    
    
}
