package com.sequoiacm.om.omserver.module;

import com.fasterxml.jackson.annotation.JsonProperty;


public class OmBucketQuotaInfo {

    @JsonProperty("bucket_name")
    private String bucketName;

    @JsonProperty("max_objects")
    private Long maxObjects;

    @JsonProperty("max_size")
    private Long maxSizeBytes;

    @JsonProperty("enable")
    private Boolean enable;

    @JsonProperty("sync_status")
    private String syncStatus;

    @JsonProperty("estimated_effective_time")
    private Long estimatedEffectiveTime;

    @JsonProperty("last_update_time")
    private Long lastUpdateTime;

    @JsonProperty("used_objects")
    private Long usedObjects;

    @JsonProperty("used_size")
    private Long usedSizeBytes;

    @JsonProperty("error_msg")
    private String errorMsg;

    @JsonProperty("quota_level")
    private String quotaLevel;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public Long getMaxObjects() {
        return maxObjects;
    }

    public void setMaxObjects(Long maxObjects) {
        this.maxObjects = maxObjects;
    }

    public Long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(Long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Long getEstimatedEffectiveTime() {
        return estimatedEffectiveTime;
    }

    public void setEstimatedEffectiveTime(Long estimatedEffectiveTime) {
        this.estimatedEffectiveTime = estimatedEffectiveTime;
    }

    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public Long getUsedObjects() {
        return usedObjects;
    }

    public void setUsedObjects(Long usedObjects) {
        this.usedObjects = usedObjects;
    }

    public Long getUsedSizeBytes() {
        return usedSizeBytes;
    }

    public void setUsedSizeBytes(Long usedSizeBytes) {
        this.usedSizeBytes = usedSizeBytes;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getQuotaLevel() {
        return quotaLevel;
    }

    public void setQuotaLevel(String quotaLevel) {
        this.quotaLevel = quotaLevel;
    }
}
