package com.sequoiacm.s3.scan;

import com.sequoiacm.s3.exception.S3ServerException;
import org.bson.BSONObject;

public interface S3ScanOffset {
    boolean isReach(BSONObject record);

    BSONObject getOrderBy();

    BSONObject getOptimizedMatcher();

    S3ScanOffset createOffsetByRecord(BSONObject b, String commonPrefix) throws S3ServerException;
}
