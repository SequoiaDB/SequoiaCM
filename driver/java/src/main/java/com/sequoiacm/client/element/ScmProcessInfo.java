package com.sequoiacm.client.element;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.PropertiesDefine;

/**
 * The class of Process information.
 *
 * @since 2.2
 */
public class ScmProcessInfo {
    private String version;
    private String revision;
    private Date compileTime;
    private String runningStatus;
    private Date startTime;

    public ScmProcessInfo(BSONObject obj) throws ScmException {
        try {
            version = (String) obj.get(PropertiesDefine.PROPERTY_SCM_VERSION);
            revision = (String) obj.get(PropertiesDefine.PROPERTY_SCM_REVISION);
            runningStatus = (String) obj.get(PropertiesDefine.PROPERTY_SCM_STATUS);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeStr = (String) obj.get(PropertiesDefine.PROPERTY_SCM_COMPILE_TIME);
            if (timeStr != null && timeStr.length() != 0) {
                compileTime = sdf.parse(timeStr);
            }

            timeStr = (String) obj.get(PropertiesDefine.PROPERTY_SCM_START_TIME);
            if (timeStr != null && timeStr.length() != 0) {
                startTime = sdf.parse(timeStr);
            }
        }
        catch (Exception e) {
            throw new ScmSystemException("failed to parse nodeInfo:"
                    + obj.toString(), e);
        }
    }

    /**
     * Get Scm version.
     *
     * @return Version information.
     * @since 2.2
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get Scm revision.
     *
     * @return Revision information
     * @since 2.2
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Get Scm compile time.
     *
     * @return Compile time
     * @since 2.2
     */
    public Date getCompileTime() {
        return compileTime;
    }

    /**
     * Get Scm process status.
     *
     * @return Process status.
     * @since 2.2
     * @see com.sequoiacm.common.CommonDefine.ScmProcessStatus
     */
    public String getRunningStatus() {
        return runningStatus;
    }

    /**
     * Get Scm process start time.
     *
     * @return Start time.
     * @since 2.2
     */
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public String toString() {
        return "version=" + version + ",revision=" + revision + ",compileTime=" + compileTime
                + ",nodeStatus=" + runningStatus + ",startTime=" + startTime;
    }

}
