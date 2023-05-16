package com.sequoiacm.infrastructure.sdbversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class SdbVersionChecker {
    private static final Logger logger = LoggerFactory.getLogger(SdbVersionChecker.class);
    private final VersionFetcher versionFetcher;
    @Value("${scm.sdbVersionChecker.versionCacheTTL:300000}")
    private int versionCacheTTL = 10 * 60 * 1000;
    private long lastRefreshVersionTime = 0;

    private volatile Version version = null;

    public SdbVersionChecker(VersionFetcher versionFetcher) {
        this.versionFetcher = versionFetcher;
    }

    public boolean isCompatible(List<VersionRange> versionRanges) {
        if (System.currentTimeMillis() - lastRefreshVersionTime > versionCacheTTL) {
            version = getSdbVersionSilence();
        }

        if (version == null) {
            throw new IllegalArgumentException("failed to get sequoiadb version");
        }

        return version.inRange(versionRanges);
    }

    public Version getSdbVersionSilence() {
        try {
            return versionFetcher.fetchVersion();
        }
        catch (Exception e) {
            logger.warn("get version failed", e);
            return null;
        }
    }

    public Version getSdbVersion() {
        return version;
    }
}
