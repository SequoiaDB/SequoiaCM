package com.sequoiacm.contentserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.workspace.tagRetrievalUpdater")
public class ScmWorkspaceTagRetrievalUpdaterConfig {
    private int interval = 60000;

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}
