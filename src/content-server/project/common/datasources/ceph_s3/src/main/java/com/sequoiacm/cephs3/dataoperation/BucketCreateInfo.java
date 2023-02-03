package com.sequoiacm.cephs3.dataoperation;

/**
 * record create bucket info
 */
public class BucketCreateInfo {
    private String bucketName;
    // 是否新创建桶
    private boolean isCreate;

    public BucketCreateInfo(String bucketName, boolean isCreate) {
        this.bucketName = bucketName;
        this.isCreate = isCreate;
    }

    public String getBucketName() {
        return bucketName;
    }

    public boolean isCreate() {
        return isCreate;
    }
}
