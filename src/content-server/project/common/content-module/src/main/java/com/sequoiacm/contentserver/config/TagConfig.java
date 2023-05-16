package com.sequoiacm.contentserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.tag")
public class TagConfig {
    private int maxTagLibCacheCount = 100;
    private int tagLibCacheMaxSize = 1000;

    public int getMaxTagLibCacheCount() {
        return maxTagLibCacheCount;
    }

    public void setMaxTagLibCacheCount(int maxTagLibCacheCount) {
        this.maxTagLibCacheCount = maxTagLibCacheCount;
    }

    public int getTagLibCacheMaxSize() {
        return tagLibCacheMaxSize;
    }

    public void setTagLibCacheMaxSize(int tagLibCacheMaxSize) {
        this.tagLibCacheMaxSize = tagLibCacheMaxSize;
    }
}
