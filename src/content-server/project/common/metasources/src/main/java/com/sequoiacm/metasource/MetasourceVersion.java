package com.sequoiacm.metasource;

public class MetasourceVersion {
    private final int version;
    private final int subVersion;
    private final int fixVersion;

    public MetasourceVersion(int version, int subVersion, int fixVersion) {
        this.version = version;
        this.subVersion = subVersion;
        this.fixVersion = fixVersion;
    }

    public int getVersion() {
        return version;
    }

    public int getSubVersion() {
        return subVersion;
    }

    public int getFixVersion() {
        return fixVersion;
    }
}
