package com.sequoiacm.infrastructure.config.client.core.workspace;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

@ConfigurationProperties("scm.conf.client.workspace")
public class WorkspaceConfSubscriberConfig {

    private static final Logger logger = LoggerFactory
            .getLogger(WorkspaceConfSubscriberConfig.class);

    @ScmRewritableConfMarker
    private long heartbeatInterval = 3 * 60 * 1000; // 3 min

    public WorkspaceConfSubscriberConfig(Environment env) {
        String heartbeatInterval = env.getProperty("scm.conf.version.workspaceHeartbeat");
        if (heartbeatInterval != null) {
            long interval = Long.parseLong(heartbeatInterval);
            if (interval <= 0) {
                logger.warn("Invalid interval value of workspace heartbeat: " + heartbeatInterval
                        + ", set to default value: " + this.heartbeatInterval);
                return;
            }
            this.heartbeatInterval = interval;
        }
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        if (heartbeatInterval <= 0) {
            logger.warn("Invalid interval value of workspace heartbeat: " + heartbeatInterval
                    + ", set to default value: " + this.heartbeatInterval);
            return;
        }
        this.heartbeatInterval = heartbeatInterval;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }
}
