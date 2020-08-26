package com.sequoiacm.contentserver.model;

import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.exception.ScmError;

public class ScmVersion {
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
    public String toString() {
        if(isAssigned()) {
            return majorVersion + "." + minorVersion;
        }
        return "null";
    }

}
