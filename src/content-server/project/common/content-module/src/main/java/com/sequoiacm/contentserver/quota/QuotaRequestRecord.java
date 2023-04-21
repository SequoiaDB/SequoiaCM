package com.sequoiacm.contentserver.quota;

import java.util.concurrent.atomic.AtomicLong;

public class QuotaRequestRecord {
    private String bucketName;
    private AtomicLong objects;
    private AtomicLong size;
    private long seconds;

    public QuotaRequestRecord(String bucketName, long objects, long size, long seconds) {
        this.bucketName = bucketName;
        this.objects = new AtomicLong(objects);
        this.size = new AtomicLong(size);
        this.seconds = seconds;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public long getObjects() {
        return objects.longValue();
    }

    public long getSize() {
        return size.longValue();
    }

    public long getSeconds() {
        return seconds;
    }

    public void setSeconds(long seconds) {
        this.seconds = seconds;
    }

    public void addUsedObjects(long objects) {
        this.objects.addAndGet(objects);
    }

    public void addUsedSize(long size) {
        this.size.addAndGet(size);
    }
}
