package com.sequoiacm.s3.processor;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.lock.exception.ScmLockException;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.core.Part;
import com.sequoiacm.s3.core.UploadMeta;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.CompleteMultipartUploadResult;
import com.sequoiacm.s3.model.CompletePart;
import com.sequoiacm.s3.processor.impl.MultipartUploadProcessorSeekable;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface MultipartUploadProcessor {
    void initMultipartUpload(String wsName, long uploadId, UploadMeta meta)
            throws S3ServerException, ScmServerException, ScmMetasourceException;

    Part uploadPart(String wsName, long uploadId, int partNumber, String contentMD5,
            InputStream inputStream, long contentLength, int wsVersion, String tableName)
            throws S3ServerException, ScmMetasourceException, ScmLockException, ScmServerException,
            ScmDatasourceException, IOException, NoSuchAlgorithmException;

    CompleteMultipartUploadResult completeUpload(String wsName, ScmSession session,
            String bucketName, UploadMeta upload, List<CompletePart> reqPartList,
            ServletOutputStream outputStream) throws S3ServerException, ScmServerException,
            ScmMetasourceException, IOException, ScmDatasourceException;

    void cleanInvalidUpload(String wsName, MetaSource ms, UploadMeta uploadMeta)
            throws S3ServerException, ScmMetasourceException, ScmServerException;
}
