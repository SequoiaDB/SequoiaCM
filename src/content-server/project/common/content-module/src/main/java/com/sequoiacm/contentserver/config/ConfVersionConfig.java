package com.sequoiacm.contentserver.config;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.conf.version")
public class ConfVersionConfig {

    private Logger logger = LoggerFactory.getLogger(ConfVersionConfig.class);

    @ScmRewritableConfMarker
    private long workspaceHeartbeat = 3 * 60 * 1000;

    @ScmRewritableConfMarker
    private long siteHeartbeat = 3 * 60 * 1000;

    @ScmRewritableConfMarker
    private long nodeHeartbeat = 3 * 60 * 1000;

    @ScmRewritableConfMarker
    private long metaDataHeartbeat = 3 * 60 * 1000;

    public long getMetaDataHeartbeat() {
        return metaDataHeartbeat;
    }

    public void setMetaDataHeartbeat(long metaDataHeartbeat) {
        if (metaDataHeartbeat <= 0) {
            logger.warn("Invalid interval value of metadata heartbeat: " + metaDataHeartbeat
                    + ", set to default value: " + this.metaDataHeartbeat);
            return;
        }
        this.metaDataHeartbeat = metaDataHeartbeat;
    }

    public long getWorkspaceHeartbeat() {
        return workspaceHeartbeat;
    }

    public void setWorkspaceHeartbeat(long workspaceHeartbeat) {
        if (workspaceHeartbeat <= 0) {
            logger.warn("Invalid interval value of workspace heartbeat: " + workspaceHeartbeat
                    + ", set to default value: " + this.workspaceHeartbeat);
            return;
        }
        this.workspaceHeartbeat = workspaceHeartbeat;
    }

    public long getSiteHeartbeat() {
        return siteHeartbeat;
    }

    public void setSiteHeartbeat(long siteHeartbeat) {
        if (siteHeartbeat <= 0) {
            logger.warn("Invalid interval value of site heartbeat: " + siteHeartbeat
                    + ", set to default value: " + this.siteHeartbeat);
            return;
        }
        this.siteHeartbeat = siteHeartbeat;
    }

    public long getNodeHeartbeat() {
        return nodeHeartbeat;
    }

    public void setNodeHeartbeat(long nodeHeartbeat) {
        if (nodeHeartbeat <= 0) {
            logger.warn("Invalid interval value of node heartbeat: " + nodeHeartbeat
                    + ", set to default value: " + this.nodeHeartbeat);
            return;
        }
        this.nodeHeartbeat = nodeHeartbeat;
    }

}
