package com.sequoiacm.cloud.adminserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.common.FieldName;

public class QuotaResult {

    @JsonProperty(FieldName.Quota.TYPE)
    private String type;

    @JsonProperty(FieldName.Quota.NAME)
    private String name;

    @JsonProperty(FieldName.Quota.MAX_OBJECTS)
    private long maxObjects = -1;

    @JsonProperty(FieldName.Quota.MAX_SIZE)
    private long maxSize = -1;

    @JsonProperty(FieldName.Quota.ENABLE)
    private boolean enable;


    @JsonProperty(FieldName.QuotaStatisticsProgress.ESTIMATED_TIME)
    private long estimatedEffectiveTime;

    @JsonProperty(FieldName.Quota.UPDATE_TIME)
    private Long lastUpdateTime;

    @JsonProperty(FieldName.Quota.USED_SIZE)
    private long usedSize;

    @JsonProperty(FieldName.Quota.USED_OBJECTS)
    private long usedObjects;

    @JsonProperty(FieldName.QuotaSync.STATUS)
    private String syncStatus;

    @JsonProperty(FieldName.QuotaSync.ERROR_MSG)
    private String errorMsg;

    public QuotaResult() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMaxObjects() {
        return maxObjects;
    }

    public void setMaxObjects(long maxObjects) {
        this.maxObjects = maxObjects;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public long getEstimatedEffectiveTime() {
        return estimatedEffectiveTime;
    }

    public void setEstimatedEffectiveTime(long estimatedEffectiveTime) {
        this.estimatedEffectiveTime = estimatedEffectiveTime;
    }

    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public long getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(long usedSize) {
        this.usedSize = usedSize;
    }

    public long getUsedObjects() {
        return usedObjects;
    }

    public void setUsedObjects(long usedObjects) {
        this.usedObjects = usedObjects;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @Override
    public String toString() {
        return "QuotaInfo{" + "type='" + type + '\'' + ", name='" + name + '\'' + ", maxObjects="
                + maxObjects + ", maxSize=" + maxSize + ", enable=" + enable
                + ", estimatedEffectiveTime=" + estimatedEffectiveTime + ", lastUpdateTime="
                + lastUpdateTime + ", usedSize=" + usedSize + ", usedObjects=" + usedObjects
                + ", syncStatus='" + syncStatus + '\'' + ", errorMsg='" + errorMsg + '\'' + '}';
    }
}
