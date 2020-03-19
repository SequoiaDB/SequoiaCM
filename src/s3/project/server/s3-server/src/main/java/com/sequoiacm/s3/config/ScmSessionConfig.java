package com.sequoiacm.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.s3.session")
public class ScmSessionConfig {
    
    private int checkInterval = 60 * 1000;

    public ScmSessionConfig() {
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(int refreshInterval) {
        this.checkInterval = refreshInterval;
    }
}
