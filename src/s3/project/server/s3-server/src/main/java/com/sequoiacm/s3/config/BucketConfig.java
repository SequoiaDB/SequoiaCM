package com.sequoiacm.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.s3.bucket")
public class BucketConfig {
    private String bucketDir = "/#S3_BUCKETS#/";

    private boolean allowreput = false;

    public boolean getAllowreput() {
        return allowreput;
    }

    public void setAllowreput(boolean allowreput) {
        this.allowreput = allowreput;
    }

    public String getBucketDir() {
        return bucketDir;
    }

    public void setBucketDir(String bucketDir) {
        this.bucketDir = bucketDir;
    }
}
