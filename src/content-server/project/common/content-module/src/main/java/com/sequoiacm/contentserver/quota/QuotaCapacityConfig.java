package com.sequoiacm.contentserver.quota;

public class QuotaCapacityConfig {

    private String bucketName;

    private long maxSize;

    private int maxObject;

    private boolean enable;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public int getMaxObject() {
        return maxObject;
    }

    public void setMaxObject(int maxObject) {
        this.maxObject = maxObject;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
