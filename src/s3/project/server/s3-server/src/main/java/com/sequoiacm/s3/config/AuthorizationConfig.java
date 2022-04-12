package com.sequoiacm.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.s3.authorization")
public class AuthorizationConfig {

    private int maxTimeOffset = -1;

    public int getMaxTimeOffset() {
        return maxTimeOffset;
    }

    public void setMaxTimeOffset(int maxTimeOffset) {
        this.maxTimeOffset = maxTimeOffset;
    }
}
