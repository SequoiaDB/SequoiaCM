package com.sequoiacm.s3.service;

import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.core.Part;
import com.sequoiacm.s3.core.Range;
import com.sequoiacm.s3.core.UploadMeta;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.*;

import javax.servlet.ServletOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface MultiPartService {
    InitiateMultipartUploadResult initMultipartUpload(ScmSession session, String bucketName,
            UploadMeta meta) throws S3ServerException;

    Part uploadPart(ScmSession session, ScmBucket bucket, String objectName, long uploadId,
            int partnumber, String contentMD5, InputStream inputStream, long contentLength)
            throws S3ServerException;

    CompleteMultipartUploadResult completeUpload(ScmSession session, String bucketName,
            String objectName, long uploadId, List<CompletePart> reqPartList,
            ServletOutputStream outputStream) throws S3ServerException;

    void abortUpload(ScmSession session, String bucketName, String objectName, long uploadId)
            throws S3ServerException;

    ListPartsResult listParts(ScmSession session, String bucketName, String objectName,
            long uploadId, Integer partNumberMarker, Integer maxParts, String encodingType)
            throws S3ServerException;

    ListMultipartUploadsResult listUploadLists(ScmSession session, String bucketName, String prefix,
            String delimiter, String keyMarker, Long uploadIdMarker, Integer maxKeys,
            String encodingType) throws S3ServerException;
}
