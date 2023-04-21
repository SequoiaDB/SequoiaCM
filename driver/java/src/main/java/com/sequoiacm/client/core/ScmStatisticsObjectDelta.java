package com.sequoiacm.client.core;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

import java.util.Date;

public class ScmStatisticsObjectDelta {

    private String bucketName;
    private long countDelta;
    private long sizeDelta;
    private Date recordTime;
    private Date updateTime;

    public ScmStatisticsObjectDelta(BSONObject obj) throws ScmException {
        this.bucketName = BsonUtils.getStringChecked(obj, FieldName.ObjectDelta.FIELD_BUCKET_NAME);
        this.countDelta = BsonUtils.getNumberChecked(obj, FieldName.ObjectDelta.FIELD_COUNT_DELTA)
                .longValue();
        this.sizeDelta = BsonUtils.getNumberChecked(obj, FieldName.ObjectDelta.FIELD_SIZE_DELTA)
                .longValue();
        this.recordTime = new Date(BsonUtils
                .getNumberChecked(obj, FieldName.ObjectDelta.FIELD_RECORD_TIME).longValue());
        this.updateTime = new Date(BsonUtils
                .getNumberChecked(obj, FieldName.ObjectDelta.FIELD_UPDATE_TIME).longValue());

    }

    /**
     * Gets the bucket name of the object delta.
     * 
     * @return The bucket name.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Gets the count delta of the object delta.
     * 
     * @return The count delta.
     */
    public long getCountDelta() {
        return countDelta;
    }

    /**
     * Gets the size delta of the object delta.
     * 
     * @return The size delta.
     */
    public long getSizeDelta() {
        return sizeDelta;
    }

    /**
     * Gets the record time of the object delta.
     * 
     * @return The record time.
     */
    public Date getRecordTime() {
        return recordTime;
    }

    /**
     * Gets the update time of the object delta.
     * 
     * @return The update time.
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    @Override
    public String toString() {
        return "ScmStatisticsObjectDelta{" + "bucketName='" + bucketName + '\'' + ", countDelta="
                + countDelta + ", sizeDelta=" + sizeDelta + ", recordTime=" + recordTime
                + ", updateTime=" + updateTime + '}';
    }
}
