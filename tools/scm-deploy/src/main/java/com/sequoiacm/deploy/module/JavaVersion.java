package com.sequoiacm.deploy.module;

public class JavaVersion {
    private int majorVersion;
    private int minorVersion;

    public JavaVersion(String versionStr) {
        String[] majorAndMinorVersionArr = versionStr.split("\\.");
        this.majorVersion = Integer.valueOf(majorAndMinorVersionArr[0]);
        this.minorVersion = Integer.valueOf(majorAndMinorVersionArr[1]);
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public boolean isGte(JavaVersion other) {
        if (majorVersion > other.majorVersion) {
            return true;
        }

        if (majorVersion == other.majorVersion && minorVersion >= other.minorVersion) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return majorVersion + "." + minorVersion;
    }

}