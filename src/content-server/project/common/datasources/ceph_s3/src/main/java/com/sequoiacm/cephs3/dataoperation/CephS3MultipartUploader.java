package com.sequoiacm.cephs3.dataoperation;

import com.amazonaws.services.s3.model.*;
import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3ConnWrapper;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.common.memorypool.ScmPoolWrapper;
import com.sequoiacm.datasource.dataservice.ScmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class CephS3MultipartUploader {
    private static final Logger logger = LoggerFactory.getLogger(CephS3MultipartUploader.class);
    private static final int PART_SIZE = 5 * 1024 * 1024;
    private final long writeOffset;
    private byte[] buffer = null;
    private int bufferOff = 0;
    private final String bucketName;
    private final String key;
    private String uploadId;
    private int writingPartNum = -1;
    private final CephS3DataService dataService;
    private CephS3ConnWrapper conn;
    private int fileSize;
    private List<PartETag> eTags = new ArrayList<>();
    private ScmPoolWrapper poolWrapper;
    private boolean createBucketIfNotExist = true;

    public CephS3MultipartUploader(ScmService service, String bucketName, String key,
            boolean createBucketIfNotExist) throws CephS3Exception {
        this(service, bucketName, key, null, 0, createBucketIfNotExist);
    }

    public CephS3MultipartUploader(ScmService service, String bucketName, String key,
            String uploadId, long writeOffset, boolean createBucketIfNotExist)
            throws CephS3Exception {
        this.bucketName = bucketName;
        this.key = key;
        this.uploadId = uploadId;
        this.writeOffset = writeOffset;
        this.dataService = (CephS3DataService) service;
        this.conn = dataService.getConn();
        this.createBucketIfNotExist = createBucketIfNotExist;
        if (conn == null) {
            throw new CephS3Exception(
                    "construct CephS3MultipartUploader failed, cephs3 is down:bucketName="
                            + bucketName + ",key=" + key);
        }
        try {
            this.poolWrapper = ScmPoolWrapper.getInstance();
            this.buffer = poolWrapper.getBytes(PART_SIZE);
        }
        catch (Exception e) {
            throw new CephS3Exception("construct CephS3MultipartUploader failed:bucketName="
                    + bucketName + ",key=" + key, e);
        }
        try {
            init(bucketName, key, uploadId);
        }
        catch (Exception e) {
            if (e instanceof CephS3Exception) {
                if (((CephS3Exception) e).getS3ErrorCode()
                        .equals(CephS3Exception.ERR_CODE_NO_SUCH_BUCKET)) {
                    throw e;
                }
            }
            conn = dataService.releaseAndTryGetAnotherConn(conn);
            if (conn == null) {
                releaseResource();
                throw e;
            }
            logger.warn(
                    "write data failed, get another ceph conn to try again: bucketName={}, key={}, conn={}",
                    bucketName, key, conn.getUrl(), e);
            init(bucketName, key, uploadId);
        }
    }

    private void init(String bucketName, String key, String uploadId) throws CephS3Exception {
        List<PartSummary> parts;
        if (uploadId != null) {
            parts = conn.listPart(bucketName, key, uploadId);
        }
        else {
            try {
                InitiateMultipartUploadRequest req = new InitiateMultipartUploadRequest(bucketName,
                        key);
                InitiateMultipartUploadResult resp = conn.initiateMultipartUpload(req);
                this.uploadId = resp.getUploadId();
                parts = new ArrayList<>();
            }
            catch (CephS3Exception e) {
                if (e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_BUCKET)
                        && createBucketIfNotExist) {
                    conn.createBucket(bucketName);
                    init(bucketName, key, null);
                    return;
                }
                else {
                    throw e;
                }
            }
        }
        for (PartSummary ps : parts) {
            eTags.add(new PartETag(ps.getPartNumber(), ps.getETag()));
        }
    }

    public void write(byte[] content, int offset, int len) throws CephS3Exception {
        try {
            fileSize += len;
            while (len >= buffer.length - bufferOff) {
                int writeSize = buffer.length - bufferOff;
                System.arraycopy(content, offset, buffer, bufferOff, writeSize);

                bufferOff = buffer.length;
                sendAndClearBuffer();

                len -= writeSize;
                offset += writeSize;
            }

            System.arraycopy(content, offset, buffer, bufferOff, len);
            bufferOff += len;
        }
        catch (CephS3Exception e) {
            throw e;
        }
        catch (Exception e) {
            throw new CephS3Exception("write data failed:bucketName=" + bucketName + ",key=" + key,
                    e);
        }
    }

    // 检查 writeOffset 必须是5m的倍数，这意味着如果上一片数据不足5M，本次writeOffset必然不会是5m的倍数，此时进行报错
    private void initPartNum() throws CephS3Exception {
        if (writeOffset % PART_SIZE != 0) {
            throw new CephS3Exception("write offset must be a multiple of " + PART_SIZE);
        }
        this.writingPartNum = (int) (writeOffset / PART_SIZE + 1);
        if (writingPartNum < 1 || writingPartNum > eTags.size() + 1) {
            throw new CephS3Exception("unexpected start partNum: bucketName=" + bucketName
                    + ", key=" + key + ", uploadId=" + uploadId + ", uploadedParts=" + eTags.size()
                    + ", requestStartPartNum=" + writingPartNum);
        }
    }

    private void sendAndClearBuffer() throws CephS3Exception {
        if (writingPartNum == -1) {
            initPartNum();
        }
        try {
            UploadPartResult resp = conn.uploadPart(new UploadPartRequest()
                    .withBucketName(bucketName)
                    .withInputStream(new ByteArrayInputStream(buffer, 0, bufferOff)).withKey(key)
                    .withPartNumber(writingPartNum).withPartSize(bufferOff).withUploadId(uploadId));
            eTags.add(resp.getPartETag());
        }
        catch (CephS3Exception e) {
            logger.error("upload part failed:bucketName=" + bucketName + ",key=" + key + ",part="
                    + writingPartNum);
            throw e;
        }
        writingPartNum++;
        bufferOff = 0;
    }

    public void flush() throws CephS3Exception {
        if (bufferOff <= 0) {
            return;
        }
        sendAndClearBuffer();
    }

    public void cancel() {
        try {
            conn.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, key, uploadId));
        }
        catch (Exception e) {
            logger.warn("cancel writer failed:bucketName=" + bucketName + ",key=" + key, e);
        }
        bufferOff = 0;
    }

    public void close() throws CephS3Exception {
        if (conn == null) {
            return;
        }
        try {
            flush();
        }
        finally {
            releaseResource();
        }
    }

    public void complete() throws CephS3Exception {
        flush();
        if (eTags.size() <= 0) {
            // 空文件也需要有一个空段
            sendAndClearBuffer();
        }
        conn.completeMultipartUpload(
                new CompleteMultipartUploadRequest(bucketName, key, uploadId, eTags));
    }

    private void releaseResource() {
        if (buffer != null) {
            poolWrapper.releaseBytes(buffer);
            buffer = null;
        }
        eTags = null;
        if (conn != null) {
            dataService.releaseConn(conn);
            conn = null;
        }
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getUploadId() {
        return uploadId;
    }
}
