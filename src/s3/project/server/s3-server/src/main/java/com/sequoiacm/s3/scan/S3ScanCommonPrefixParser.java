package com.sequoiacm.s3.scan;

import org.bson.BSONObject;

import com.sequoiacm.s3.exception.S3ServerException;

public interface S3ScanCommonPrefixParser {

    // 返回空表示不可分割
    String getCommonPrefix(BSONObject record) throws S3ServerException;
}
