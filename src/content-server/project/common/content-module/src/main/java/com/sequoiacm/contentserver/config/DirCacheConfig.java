package com.sequoiacm.contentserver.config;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scm.dir.cache")
public class DirCacheConfig {

    @ScmRewritableConfMarker
    private int maxSize = 10000;
    private boolean enable = true;

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        if (maxSize >= 100 && maxSize <= 100000) {
            this.maxSize = maxSize;
        }
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

}
