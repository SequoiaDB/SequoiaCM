package com.sequoiacm.client.element.quota;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmQuotaSyncStatus;
import org.bson.BSONObject;

import java.util.Date;

public class ScmBucketQuotaInfo {

    private String bucketName;
    private long maxObjects;
    private long maxSizeBytes;
    private boolean enable;

    private ScmQuotaSyncStatus syncStatus;
    private long estimatedEffectiveTime;
    private Date lastUpdateTime;
    private long usedObjects;
    private long usedSizeBytes;
    private String errorMsg;

    public ScmBucketQuotaInfo(BSONObject bsonObject) throws ScmException {
        this.bucketName = BsonUtils.getStringChecked(bsonObject, FieldName.Quota.NAME);
        this.maxObjects = BsonUtils.getNumberChecked(bsonObject, FieldName.Quota.MAX_OBJECTS)
                .longValue();
        this.maxSizeBytes = BsonUtils.getNumberChecked(bsonObject, FieldName.Quota.MAX_SIZE)
                .longValue();
        this.enable = BsonUtils.getBooleanChecked(bsonObject, FieldName.Quota.ENABLE);

        this.syncStatus = ScmQuotaSyncStatus
                .getByName(BsonUtils.getString(bsonObject, FieldName.QuotaSync.STATUS));
        Number temp = BsonUtils.getNumber(bsonObject,
                FieldName.QuotaStatisticsProgress.ESTIMATED_TIME);
        if (temp != null) {
            this.estimatedEffectiveTime = temp.longValue();
        }
        temp = BsonUtils.getNumber(bsonObject, FieldName.Quota.UPDATE_TIME);
        if (temp != null) {
            this.lastUpdateTime = new Date(temp.longValue());
        }
        temp = BsonUtils.getNumber(bsonObject, FieldName.Quota.USED_OBJECTS);
        if (temp != null) {
            this.usedObjects = temp.longValue();
        }
        temp = BsonUtils.getNumber(bsonObject, FieldName.Quota.USED_SIZE);
        if (temp != null) {
            this.usedSizeBytes = temp.longValue();
        }
        this.errorMsg = BsonUtils.getString(bsonObject, FieldName.QuotaSync.ERROR_MSG);
    }

    /**
     * Gets the bucket name.
     * 
     * @return the bucket name.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Gets the maximum object count limit for the bucket. If the value is -1, it
     * means no limit.
     * 
     * @return the maximum object count limit for the bucket.
     */
    public long getMaxObjects() {
        return maxObjects;
    }

    /**
     * Gets the maximum size limit with bytes for the bucket. If the value is -1, it
     * means no limit.
     * 
     * @return The maximum size limit with bytes for the bucket.
     */
    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    /**
     * Is the quota limit enabled.
     * 
     * @return true if the quota limit is enabled, otherwise false.
     */
    public boolean isEnable() {
        return enable;
    }

    /**
     * Gets the synchronization status of the quota info. return null if no
     * synchronization has been performed.
     * 
     * @see ScmQuotaSyncStatus
     * 
     * @return the synchronization status of the quota info.
     */
    public ScmQuotaSyncStatus getSyncStatus() {
        return syncStatus;
    }

    /**
     * Gets the estimated effective time of the quota limit. return 0 if quota limit
     * disabled. return -1 is estimating.
     * 
     * @return the estimated effective time of the quota limit.
     */
    public long getEstimatedEffectiveTime() {
        return estimatedEffectiveTime;
    }

    /**
     * Gets the used object count for the bucket.
     * 
     * @return the used object count for the bucket.
     */
    public long getUsedObjects() {
        return usedObjects;
    }

    /**
     * Gets the used size with bytes for the bucket.
     * 
     * @return the used size with bytes for the bucket.
     */
    public long getUsedSizeBytes() {
        return usedSizeBytes;
    }

    /**
     * Gets the last update time of the quota info.
     * 
     * @return the last update time of the quota info.
     */
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * Gets the error message when synchronization failed.
     * 
     * @return the error message.
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    @Override
    public String toString() {
        return "ScmBucketQuotaInfo{" + "bucketName='" + bucketName + '\'' + ", maxObjects="
                + maxObjects + ", maxSizeBytes=" + maxSizeBytes + ", enable=" + enable
                + ", syncStatus=" + syncStatus + ", estimatedEffectiveTime="
                + estimatedEffectiveTime + ", lastUpdateTime=" + lastUpdateTime + ", usedObjects="
                + usedObjects + ", usedSizeBytes=" + usedSizeBytes + ", errorMsg='" + errorMsg
                + '\'' + '}';
    }
}
