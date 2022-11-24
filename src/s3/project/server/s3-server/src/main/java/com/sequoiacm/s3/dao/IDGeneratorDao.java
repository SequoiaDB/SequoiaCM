package com.sequoiacm.s3.dao;

import com.sequoiacm.s3.exception.S3ServerException;

public interface IDGeneratorDao {
    Long getNewId(String type) throws S3ServerException;

    void initIdGeneratorTable() throws S3ServerException;
}
