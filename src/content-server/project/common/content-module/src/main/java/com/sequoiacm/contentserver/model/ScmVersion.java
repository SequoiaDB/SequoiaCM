package com.sequoiacm.contentserver.model;

import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.exception.ScmError;

import java.util.Objects;

public class ScmVersion implements Comparable<ScmVersion> {
    private int majorVersion = -1;
    private int minorVersion = -1;

    public ScmVersion(Integer majorVersion, Integer minorVersion) throws ScmServerException {
        if (majorVersion != null && minorVersion != null) {
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
        }
        else if (majorVersion == null && minorVersion == null) {
            this.majorVersion = -1;
            this.minorVersion = -1;
        }
        else {
            throw new ScmInvalidArgumentException(
                    "invlid version:majorVersion=" + majorVersion + ",minorVersion="
                            + minorVersion);
        }
    }

    public ScmVersion(int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public ScmVersion() {
    }

    public ScmVersion(String versionSerial) throws ScmServerException {
        String[] versionArr = versionSerial.split("\\.");
        if (versionArr.length != 2) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "invalid version serail:" + versionSerial);
        }
        majorVersion = Integer.parseInt(versionArr[0]);
        minorVersion = Integer.parseInt(versionArr[1]);
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public boolean isAssigned() {
        if (majorVersion == -1 || minorVersion == -1) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScmVersion that = (ScmVersion) o;
        return majorVersion == that.majorVersion &&
                minorVersion == that.minorVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(majorVersion, minorVersion);
    }

    @Override
    public String toString() {
        return majorVersion + "." + minorVersion;
    }

    @Override
    public int compareTo(ScmVersion o) {
        if (majorVersion > o.majorVersion) {
            return 1;
        }
        if (majorVersion < o.majorVersion) {
            return -1;
        }

        if (minorVersion > o.minorVersion) {
            return 1;
        }
        if (minorVersion < o.minorVersion) {
            return -1;
        }

        return 0;
    }
}
