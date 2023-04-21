package com.sequoiacm.contentserver.quota;

public class QuotaInfo {
    private String bucketName;
    private long objects;
    private long size;
    private long createTime;

    public QuotaInfo(String bucketName, long objects, long size, long createTime) {
        this.bucketName = bucketName;
        this.objects = objects;
        this.size = size;
        this.createTime = createTime;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public long getObjects() {
        return objects;
    }

    public void setObjects(long objects) {
        this.objects = objects;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "QuotaInfo{" + "bucketName='" + bucketName + '\'' + ", objects=" + objects
                + ", size=" + size + ", createTime=" + createTime + '}';
    }
}
