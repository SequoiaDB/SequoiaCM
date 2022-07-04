package com.sequoiacm.infrastructure.config.client.core.workspace;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

@ConfigurationProperties("scm.conf.client.workspace")
public class WorkspaceConfSubscriberConfig {
    private long heartbeatInterval = 180000;

    public WorkspaceConfSubscriberConfig(Environment env) {
        this.heartbeatInterval = Long.parseLong(
                env.getProperty("scm.conf.version.workspaceHeartbeat", heartbeatInterval + ""));
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }
}
