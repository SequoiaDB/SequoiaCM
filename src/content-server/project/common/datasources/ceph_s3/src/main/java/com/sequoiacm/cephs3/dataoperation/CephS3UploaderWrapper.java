package com.sequoiacm.cephs3.dataoperation;

import java.io.ByteArrayInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3BreakpointFileContext;
import com.sequoiacm.cephs3.dataservice.CephS3ConnWrapper;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.common.memorypool.ScmPoolWrapper;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.metadata.cephs3.CephS3DataLocation;

public class CephS3UploaderWrapper implements CephS3DataUploader {
    private static final int BUFFER_SIZE = 5 * 1024 * 1024;
    private static final Logger logger = LoggerFactory.getLogger(CephS3UploaderWrapper.class);
    private final ScmPoolWrapper poolWrapper;
    private final CephS3DataLocation location;
    private byte[] buffer;
    private final CephS3DataService dataService;
    private final BucketNameOption bucketNameOption;
    private final String key;
    private int bufferDataOffset;
    private CephS3MultipartUploader multipartUploader;
    private String workspaceName;

    private int siteId;

    private ScmDataWriterContext context;

    private final CephS3BucketManager bucketManager;

    public CephS3UploaderWrapper(CephS3DataService dataService, BucketNameOption bucketNameOption,
            String key, String wsName, CephS3DataLocation location, int siteId,
            ScmDataWriterContext context) throws CephS3Exception {
        this.bufferDataOffset = 0;
        this.dataService = dataService;
        this.bucketNameOption = bucketNameOption;
        this.key = key;
        this.location = location;
        this.workspaceName = wsName;
        this.siteId = siteId;
        this.context = context;
        this.bucketManager = CephS3BucketManager.getInstance();
        try {
            this.poolWrapper = ScmPoolWrapper.getInstance();
            this.buffer = poolWrapper.getBytes(BUFFER_SIZE);
        }
        catch (Exception e) {
            throw new CephS3Exception("failed to acquire buffer: bucket="
                    + bucketNameOption + ", key=" + key,
                    e);
        }

    }

    private boolean tryWriteBuffer(byte[] data, int off, int len) {
        if (!canWriteBuffer(len)) {
            return false;
        }
        System.arraycopy(data, off, buffer, bufferDataOffset, len);
        bufferDataOffset += len;
        return true;
    }

    private boolean canWriteBuffer(int dataLen) {
        return dataLen + bufferDataOffset <= buffer.length;
    }

    @Override
    public long getFileSize() {
        if (multipartUploader != null) {
            return multipartUploader.getFileSize();
        }
        return bufferDataOffset;
    }

    private void sendBufferDataAsObject() throws CephS3Exception {
        CephS3ConnWrapper con = dataService.getConn(location.getPrimaryUserInfo(),
                location.getStandbyUserInfo());
        if (con == null) {
            throw new CephS3Exception("create object failed failed, cephs3 is down:bucketName="
                    + bucketNameOption + ",key=" + key);
        }
        try {
            sendBufferDataAsObject(con);
        }
        catch (Exception e) {
            con = dataService.releaseAndTryGetAnotherConn(con, location.getPrimaryUserInfo(),
                    location.getStandbyUserInfo());
            if (con == null) {
                throw e;
            }
            logger.warn(
                    "write data failed, get another ceph conn to try again: bucketName={}, key={}, conn={}",
                    bucketNameOption, key, con.getUrl(), e);
            sendBufferDataAsObject(con);
        }
        finally {
            dataService.releaseConn(con);
        }

    }

    private void sendBufferDataAsObject(CephS3ConnWrapper con) throws CephS3Exception {

        String targetBucketName = bucketNameOption.getTargetBucketName();
        try {
            con.putObject(targetBucketName, key,
                    new ByteArrayInputStream(buffer, 0, bufferDataOffset),
                    bufferDataOffset);
        }
        catch (CephS3Exception e) {
            if (bucketNameOption.shouldHandleBucketNotExistException()
                    && e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_BUCKET)) {
                logger.info(
                        "failed to create object cause by NO_SUCH_BUCKET, try create bucket and create object again: bucket={}, key={}, uploadId={}",
                        targetBucketName, key, e);
                bucketManager.createSpecifiedBucket(con, targetBucketName);
                con.putObject(targetBucketName, key,
                        new ByteArrayInputStream(buffer, 0, bufferDataOffset),
                        bufferDataOffset);
            }
            else if (bucketNameOption.shouldHandleQuotaExceedException()
                    && e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_QUOTA_EXCEEDED)) {
                logger.info(
                        "failed to create object cause by QUOTA_EXCEEDED, try create new bucket and create object again: bucket={}, key={}, uploadId={}",
                        targetBucketName, key, e);
                targetBucketName = bucketManager.createNewActiveBucket(con, targetBucketName,
                        bucketNameOption.getOriginBucketName(), workspaceName, siteId, dataService);
                con.putObject(targetBucketName, key,
                        new ByteArrayInputStream(buffer, 0, bufferDataOffset), bufferDataOffset);
            }
            else {
                throw e;
            }
        }

        if (!targetBucketName.equals(bucketNameOption.getOriginBucketName())) {
            context.recordTableName(targetBucketName);
        }
    }

    @Override
    public void write(byte[] data, int off, int len) throws CephS3Exception {
        if (multipartUploader != null) {
            multipartUploader.write(data, off, len);
            return;
        }
        if (tryWriteBuffer(data, off, len)) {
            return;
        }

        multipartUploader = new CephS3MultipartUploader(dataService, bucketNameOption, key,
                new CephS3BreakpointFileContext(), 0,
                buffer, bufferDataOffset, workspaceName, context, location);
        multipartUploader.write(data, off, len);
    }

    @Override
    public void cancel() {
        if (multipartUploader != null) {
            multipartUploader.cancel();
        }
    }

    @Override
    public void complete() throws CephS3Exception {
        if (multipartUploader != null) {
            multipartUploader.complete();
        }
        else {
            sendBufferDataAsObject();
        }

    }

    @Override
    public void close() throws CephS3Exception {
        try {
            if (multipartUploader != null) {
                multipartUploader.close();
            }
        }
        finally {
            releaseBuffer();
        }
    }

    private void releaseBuffer() {
        if (buffer != null) {
            poolWrapper.releaseBytes(buffer);
            buffer = null;
        }
    }
}
