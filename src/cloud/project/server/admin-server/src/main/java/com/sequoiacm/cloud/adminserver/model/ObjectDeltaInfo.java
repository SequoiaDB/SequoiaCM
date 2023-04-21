package com.sequoiacm.cloud.adminserver.model;

public class ObjectDeltaInfo {
    private String bucketName;
    private long countDelta;
    private long sizeDelta;
    private long recordTime;
    private long updateTime;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public long getCountDelta() {
        return countDelta;
    }

    public void setCountDelta(long countDelta) {
        this.countDelta = countDelta;
    }

    public long getSizeDelta() {
        return sizeDelta;
    }

    public void setSizeDelta(long sizeDelta) {
        this.sizeDelta = sizeDelta;
    }

    public long getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(long recordTime) {
        this.recordTime = recordTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
