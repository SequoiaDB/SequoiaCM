package com.sequoiacm.s3.dao;

import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.s3.core.Part;
import com.sequoiacm.s3.exception.S3ServerException;

public interface PartDao {
    void insertPart(TransactionContext transaction, Part part) throws S3ServerException;

    void updatePart(TransactionContext transaction, Part part) throws S3ServerException;

    Part queryPart(long uploadId, long partNumber) throws S3ServerException;

    Part queryOnePart(long uploadId, Long size, Integer startNum, Integer endNum)
            throws S3ServerException;

    void deletePart(TransactionContext transaction, long uploadId, Long partNumber)
            throws S3ServerException;

    MetaCursor queryPartList(long uploadId, Integer start, Integer marker, Integer maxSize)
            throws S3ServerException;

    void initPartsTable() throws S3ServerException;
}
