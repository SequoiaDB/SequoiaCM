package com.sequoiacm.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.s3.authorization")
public class AuthorizationConfig {
    private boolean check = true;
    // 15分钟
    private int maxTimeOffset = -1;

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public boolean getCheck() {
        return check;
    }

    public boolean isCheck() {
        return check;
    }

    public int getMaxTimeOffset() {
        return maxTimeOffset;
    }

    public void setMaxTimeOffset(int maxTimeOffset) {
        this.maxTimeOffset = maxTimeOffset;
    }
}
