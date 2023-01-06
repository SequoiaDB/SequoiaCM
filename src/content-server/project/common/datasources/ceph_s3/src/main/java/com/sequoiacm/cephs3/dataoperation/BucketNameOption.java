package com.sequoiacm.cephs3.dataoperation;

import com.sequoiacm.cephs3.CephS3Exception;

public class BucketNameOption {
    private BucketNameOptionType type;
    private String originBucketName;
    private String ws;

    public BucketNameOptionType getType() {
        return type;
    }

    public String getOriginBucketName() {
        return originBucketName;
    }

    private BucketNameOption() {
    }

    public static BucketNameOption fixedBucketNameOption(String bucketName) {
        BucketNameOption op = new BucketNameOption();
        op.originBucketName = bucketName;
        op.type = BucketNameOptionType.FIXED_BUCKET_NAME;
        return op;
    }

    public static BucketNameOption ruleBucketNameOption(String wsName, String ruleBucketName) {
        BucketNameOption op = new BucketNameOption();
        op.originBucketName = ruleBucketName;
        op.type = BucketNameOptionType.RULE_BUCKET_NAME;
        op.ws = wsName;
        return op;
    }

    public String getTargetBucketName() throws CephS3Exception {
        if (type == BucketNameOptionType.FIXED_BUCKET_NAME) {
            return originBucketName;
        }
        return CephS3BucketManager.getInstance().getActiveBucketName(ws, originBucketName);
    }

    public boolean shouldHandleBucketNotExistException() {
        if (type == BucketNameOptionType.FIXED_BUCKET_NAME) {
            return false;
        }
        return true;
    }

    public boolean shouldHandleQuotaExceedException() {
        if (type == BucketNameOptionType.FIXED_BUCKET_NAME) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "BucketNameOption{" + "type=" + type + ", bucketName='" + originBucketName + '\''
                + '}';
    }
}

enum BucketNameOptionType {
    // 上层指定的是必须使用该桶名，不可变
    FIXED_BUCKET_NAME,
    // 上层指定的是规则桶名，当规则桶超限时可以创建新桶存储
    RULE_BUCKET_NAME
}