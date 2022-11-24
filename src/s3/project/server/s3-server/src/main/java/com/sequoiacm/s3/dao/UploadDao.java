package com.sequoiacm.s3.dao;

import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.s3.core.UploadMeta;
import com.sequoiacm.s3.exception.S3ServerException;
import org.bson.BSONObject;

public interface UploadDao {
    void insertUploadMeta(TransactionContext transactionContext, UploadMeta upload)
            throws S3ServerException;

    void updateUploadMeta(TransactionContext transaction, UploadMeta upload)
            throws S3ServerException;

    UploadMeta queryUpload(Long bucketId, String objectName, long uploadId)
            throws S3ServerException;

    void deleteUploadByUploadId(TransactionContext transaction, long bucketId, String objectName,
            long uploadId) throws S3ServerException;

    MetaCursor queryUploads(BSONObject statusMatcher, Long exceedTime) throws S3ServerException;

    void initUploadMetaTable() throws S3ServerException;
}
