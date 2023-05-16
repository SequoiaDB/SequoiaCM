package com.sequoiacm.infrastructure.sdbversion;

import java.util.List;

public class SdbVersionIncompitableException extends Exception {

    private final List<VersionRange> requiredVersionRanges;
    private final Version currentVersion;

    public SdbVersionIncompitableException(List<VersionRange> requiredVersionRanges,
            Version currentVersion) {
        super("required sdb version is '" + requiredVersionRanges + "', but current version is "
                + currentVersion);
        this.requiredVersionRanges = requiredVersionRanges;
        this.currentVersion = currentVersion;
    }

    public List<VersionRange> getRequiredVersionRanges() {
        return requiredVersionRanges;
    }

    public Version getCurrentVersion() {
        return currentVersion;
    }
}
