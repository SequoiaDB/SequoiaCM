package com.sequoiacm.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.s3.multipartupload")
public class MultipartUploadConfig {
    private long incompletelifecycle = 3;
    private long completereservetime = 1440;

    public void setIncompletelifecycle(long incompletelifecycle) {
        this.incompletelifecycle = incompletelifecycle;
    }

    public long getIncompletelifecycle() {
        return incompletelifecycle;
    }

    public void setCompletereservetime(long completereservetime) {
        this.completereservetime = completereservetime;
    }

    public long getCompletereservetime() {
        return completereservetime;
    }
}
