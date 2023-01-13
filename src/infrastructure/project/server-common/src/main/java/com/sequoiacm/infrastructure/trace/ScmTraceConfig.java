package com.sequoiacm.infrastructure.trace;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.sequoiacm.infrastructure.common.annotation.ScmRewritableConfMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import com.sequoiacm.infrastructure.common.annotation.ScmRefreshableConfigMarker;

@ConfigurationProperties(prefix = "scm.trace")
@RefreshScope
public class ScmTraceConfig {

    private static final Logger logger = LoggerFactory.getLogger(ScmTraceConfig.class);

    @ScmRefreshableConfigMarker
    private boolean enabled;

    @ScmRefreshableConfigMarker
    @ScmRewritableConfMarker
    private int samplePercentage = 10;

    @ScmRefreshableConfigMarker
    private String sampleServices;

    private Set<String> parsedSampleServices;

    @PostConstruct
    public void init() {
        parseSampleServices();
    }

    @PreDestroy
    public void destroy() {
        this.sampleServices = null;
        this.parsedSampleServices = null;
    }

    private void parseSampleServices() {
        if (sampleServices != null) {
            Set<String> res = new HashSet<>();
            for (String service : sampleServices.split(",")) {
                res.add(service.toLowerCase());
            }
            this.parsedSampleServices = res;
        }
        else {
            this.parsedSampleServices = null;
        }
    }

    public boolean isSampledService(String service) {
        if (service == null) {
            return false;
        }
        if (parsedSampleServices == null) {
            return true;
        }
        return parsedSampleServices.contains(service.toLowerCase());
    }

    public int getSamplePercentage() {
        return samplePercentage;
    }

    public void setSamplePercentage(int samplePercentage) {
        if (samplePercentage < 0) {
            this.samplePercentage = 0;
            logger.warn("samplePercentage value:{} is invalid, update to {}", samplePercentage,
                    this.samplePercentage);
        }
        else if (samplePercentage > 100) {
            this.samplePercentage = 100;
            logger.warn("samplePercentage value:{} is invalid, update to {}", samplePercentage,
                    this.samplePercentage);
        }
        else {
            this.samplePercentage = samplePercentage;
        }

    }

    public String getSampleServices() {
        return sampleServices;
    }

    public void setSampleServices(String sampleServices) {
        this.sampleServices = sampleServices;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
