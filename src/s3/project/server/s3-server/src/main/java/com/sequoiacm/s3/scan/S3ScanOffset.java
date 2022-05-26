package com.sequoiacm.s3.scan;

import com.sequoiacm.s3.core.UploadMeta;
import com.sequoiacm.s3.exception.S3ServerException;
import org.bson.BSONObject;

public abstract class S3ScanOffset {
    public abstract boolean isReach(BSONObject record);

   public abstract BSONObject getOrderBy();

    public abstract BSONObject getOptimizedMatcher();

    public abstract S3ScanOffset createOffsetByRecord(BSONObject b, String commonPrefix)
            throws S3ServerException;

    protected boolean isReachCommonPrefix(String key, String commonPrefix) {
        // 本 offset 代表的是一个 commonPrefix，且这个 commonPrefix之前已经给过客户端了，
        // 这里检查记录是否也是这个commonPrefix，如果是就越过它
        if (key.startsWith(commonPrefix)) {
            return false;
        }
        return true;
    }
}
