package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmInvalidArgumentException;

/**
 * version serial of the file with null version .
 */
public class ScmVersionSerial {
    private int majorSerial;
    private int minorSerial;

    public ScmVersionSerial(String versionSerial) throws ScmInvalidArgumentException {
        String[] versionArr = versionSerial.split("\\.");
        if (versionArr.length != 2) {
            throw new ScmInvalidArgumentException(
                    "failed to parse version serial:" + versionSerial);
        }

        majorSerial = Integer.parseInt(versionArr[0]);
        minorSerial = Integer.parseInt(versionArr[1]);
    }

    public ScmVersionSerial(int majorSerial, int minorSerial) {
        this.majorSerial = majorSerial;
        this.minorSerial = minorSerial;
    }

    /**
     * Major version serial.
     * 
     * @return major serial.
     */
    public int getMajorSerial() {
        return majorSerial;
    }

    /**
     * Minor version serial.
     * 
     * @return minor serial.
     */
    public int getMinorSerial() {
        return minorSerial;
    }

    @Override
    public String toString() {
        return "ScmVersionSerial{" + "majorSerial=" + majorSerial + ", minorSerial=" + minorSerial
                + '}';
    }
}
