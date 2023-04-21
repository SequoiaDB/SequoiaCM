package com.sequoiacm.contentserver.model;

public class ObjectDeltaInfo {
    private String bucketName;
    private int count;
    private long sumSize;

    public ObjectDeltaInfo(String bucketName, long count, long size) {
        this.bucketName = bucketName;
        this.count = (int) count;
        this.sumSize = size;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getSumSize() {
        return sumSize;
    }

    public void setSumSize(long sumSize) {
        this.sumSize = sumSize;
    }
}
