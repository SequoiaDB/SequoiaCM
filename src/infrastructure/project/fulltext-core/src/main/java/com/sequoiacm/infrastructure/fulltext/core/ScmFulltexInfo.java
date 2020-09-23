package com.sequoiacm.infrastructure.fulltext.core;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.common.BsonUtils;

/**
 * Fulltext information for workspace.
 */
public class ScmFulltexInfo {
    private static final String KEY_STATUS = "status";
    private static final String KEY_FILE_MATCHER = "fileMatcher";
    private static final String KEY_MODE = "mode";
    private static final String KEY_JOB_INFO = "jobInfo";
    private static final String KEY_FULL_TEXT_LOCATION = "fulltextLocation";

    private ScmFulltextStatus status;
    private BSONObject fileMatcher;
    private String fulltextLocation;
    private ScmFulltextMode mode = null;

    private ScmFulltextJobInfo jobInfo;

    public ScmFulltexInfo(BSONObject obj) {
        status = ScmFulltextStatus.valueOf(BsonUtils.getStringChecked(obj, KEY_STATUS));
        fileMatcher = BsonUtils.getBSON(obj, KEY_FILE_MATCHER);
        String modeStr = BsonUtils.getString(obj, KEY_MODE);
        if (modeStr != null) {
            mode = ScmFulltextMode.valueOf(modeStr);
        }
        if (status == ScmFulltextStatus.CREATED || status == ScmFulltextStatus.CREATING) {
            jobInfo = new ScmFulltextJobInfo(BsonUtils.getBSON(obj, KEY_JOB_INFO));
        }
        fulltextLocation = BsonUtils.getString(obj, KEY_FULL_TEXT_LOCATION);
    }

    public ScmFulltexInfo() {
    }

    /**
     * Get the fulltext index data location.
     * 
     * @return index location.
     */
    public String getFulltextLocation() {
        return fulltextLocation;
    }

    public void setFulltextLocation(String fulltextLocation) {
        this.fulltextLocation = fulltextLocation;
    }

    public void setJobInfo(ScmFulltextJobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }

    /**
     * Get the fulltext index job information.
     * 
     * @return fulltext job info.
     */
    public ScmFulltextJobInfo getJobInfo() {
        return jobInfo;
    }

    /**
     * Get the fulltext status.
     * 
     * @return status.
     */
    public ScmFulltextStatus getStatus() {
        return status;
    }

    public void setStatus(ScmFulltextStatus status) {
        this.status = status;
    }

    /**
     * Get the fulltext file matcher.
     * 
     * @return mather.
     */
    public BSONObject getFileMatcher() {
        return fileMatcher;
    }

    public void setFileMatcher(BSONObject fileMatcher) {
        this.fileMatcher = fileMatcher;
    }

    /**
     * Get the fulltext mode.
     * 
     * @return mode
     */
    public ScmFulltextMode getMode() {
        return mode;
    }

    public void setMode(ScmFulltextMode mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "ScmWorksapceFulltexIdxInfo [status=" + status + ", fileMatcher=" + fileMatcher
                + ", mode=" + mode + ", jobInfo=" + jobInfo + "]";
    }

}
