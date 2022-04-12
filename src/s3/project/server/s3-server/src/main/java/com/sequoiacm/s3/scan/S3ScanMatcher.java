package com.sequoiacm.s3.scan;

import org.bson.BSONObject;

public interface S3ScanMatcher {
    BSONObject getMatcher();
}
