package com.sequoiacm.infrastructure.sdbversion;

import java.util.List;

public class Version implements Comparable<Version> {
    private int version;
    private int subVersion;
    private int fixVersion;

    public Version(int version, int subVersion, int fixVersion) {
        this.version = version;
        this.subVersion = subVersion;
        this.fixVersion = fixVersion;
    }

    public Version(String version) {
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("version is null or empty");
        }

        version = version.replace(" ", "");

        try {
            String[] versionParts = version.split("\\.");
            this.version = Integer.parseInt(versionParts[0]);
            if (versionParts.length > 1) {
                this.subVersion = Integer.parseInt(versionParts[1]);
            }
            if (versionParts.length > 2) {
                this.fixVersion = Integer.parseInt(versionParts[2]);
            }
        }
        catch (Exception e) {
            throw new IllegalArgumentException("version is invalid: " + version, e);
        }
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        return strBuilder.append(version).append(".").append(subVersion).append(".")
                .append(fixVersion).toString();
    }

    @Override
    public int compareTo(Version dbVersion) {
        int versionDiff = this.version - dbVersion.version;
        if (versionDiff != 0)
            return versionDiff;

        int subVersionDiff = this.subVersion - dbVersion.subVersion;
        if (subVersionDiff != 0)
            return subVersionDiff;

        return this.fixVersion - dbVersion.fixVersion;
    }

    public boolean inRange(List<VersionRange> rangeList) {
        for (VersionRange range : rangeList) {
            if (range.isInRange(this)) {
                return true;
            }
        }
        return false;
    }
}
