package com.sequoiacm.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.s3.bucket")
public class BucketConfig {
    private boolean allowReput = false;

    public boolean isAllowReput() {
        return allowReput;
    }

    public void setAllowReput(boolean allowReput) {
        this.allowReput = allowReput;
    }
}
