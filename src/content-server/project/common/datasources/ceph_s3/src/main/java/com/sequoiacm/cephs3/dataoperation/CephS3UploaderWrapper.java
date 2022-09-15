package com.sequoiacm.cephs3.dataoperation;

import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3ConnWrapper;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.common.memorypool.ScmPoolWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

public class CephS3UploaderWrapper implements CephS3DataUploader {
    private static final int BUFFER_SIZE = 5 * 1024 * 1024;
    private static final Logger logger = LoggerFactory.getLogger(CephS3UploaderWrapper.class);
    private final ScmPoolWrapper poolWrapper;
    private byte[] buffer;
    private final CephS3DataService dataService;
    private final String bucket;
    private final String key;
    private final boolean createBucketIfNotExist;
    private int bufferDataOffset;
    private CephS3MultipartUploader multipartUploader;

    public CephS3UploaderWrapper(CephS3DataService dataService, String bucket, String key,
            boolean createBucketIfNotExist) throws CephS3Exception {
        this.bufferDataOffset = 0;
        this.dataService = dataService;
        this.bucket = bucket;
        this.key = key;
        this.createBucketIfNotExist = createBucketIfNotExist;
        try {
            this.poolWrapper = ScmPoolWrapper.getInstance();
            this.buffer = poolWrapper.getBytes(BUFFER_SIZE);
        }
        catch (Exception e) {
            throw new CephS3Exception("failed to acquire buffer: bucket=" + bucket + ", key=" + key,
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
        CephS3ConnWrapper con = dataService.getConn();
        try {
            sendBufferDataAsObject(con);
        }
        catch (Exception e) {
            con = dataService.releaseAndTryGetAnotherConn(con);
            if (con == null) {
                throw e;
            }
            logger.warn(
                    "write data failed, get another ceph conn to try again: bucketName={}, key={}, conn={}",
                    bucket, key, con.getUrl(), e);
            sendBufferDataAsObject(con);
        }
        finally {
            dataService.releaseConn(con);
        }

    }

    private void sendBufferDataAsObject(CephS3ConnWrapper con) throws CephS3Exception {
        try {
            con.putObject(bucket, key, new ByteArrayInputStream(buffer, 0, bufferDataOffset),
                    bufferDataOffset);
        }
        catch (CephS3Exception e) {
            if (e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_BUCKET)
                    && createBucketIfNotExist) {
                con.createBucket(bucket);
                con.putObject(bucket, key, new ByteArrayInputStream(buffer, 0, bufferDataOffset),
                        bufferDataOffset);
            }
            else {
                throw e;
            }
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

        multipartUploader = new CephS3MultipartUploader(dataService, bucket, key, null, 0,
                createBucketIfNotExist, buffer, bufferDataOffset);
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
