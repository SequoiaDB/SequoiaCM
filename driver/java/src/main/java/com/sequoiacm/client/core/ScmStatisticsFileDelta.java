package com.sequoiacm.client.core;

import java.util.Date;

import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;

/**
 * Scm statistics file delta.
 */
public class ScmStatisticsFileDelta {

    private String workspaceName;
    private long countDelta;
    private long sizeDelta;
    private Date recordTime;

    ScmStatisticsFileDelta(BSONObject bson) {
        Object obj;
        obj = bson.get(FieldName.FileDelta.FIELD_WORKSPACE_NAME);
        if (null != obj) {
            setWorkspaceName((String) obj);
        }
        obj = bson.get(FieldName.FileDelta.FIELD_COUNT_DELTA);
        if (null != obj) {
            setCountDelta(Long.parseLong(String.valueOf(obj)));
        }
        obj = bson.get(FieldName.FileDelta.FIELD_SIZE_DELTA);
        if (null != obj) {
            setSizeDelta(Long.parseLong(String.valueOf(obj)));
        }
        obj = bson.get(FieldName.FileDelta.FIELD_RECORD_TIME);
        if (null != obj) {
            setRecordTime(new Date(Long.parseLong(String.valueOf(obj))));
        }
    }

    void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    void setCountDelta(long countDelta) {
        this.countDelta = countDelta;
    }

    void setSizeDelta(long sizeDelta) {
        this.sizeDelta = sizeDelta;
    }

    void setRecordTime(Date recordTime) {
        this.recordTime = recordTime;
    }

    /**
     * Return the value of the Workspace property name.
     *
     * @return Workspace name
     */
    public String getWorkspaceName() {
        return this.workspaceName;
    }

    /**
     * Returns the file count delta.
     *
     * @return file count delta
     */
    public long getCountDelta() {
        return this.countDelta;
    }

    /**
     * Returns the file size delta.
     *
     * @return file size delta
     */
    public long getSizeDelta() {
        return this.sizeDelta;
    }

    /**
     * Returns the date represented by this statistical record.
     *
     * @return Record Time
     */
    public Date getRecordTime() {
        return this.recordTime;
    }
}
