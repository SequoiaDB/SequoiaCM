package com.sequoiacm.cephs3.dataoperation;

import com.amazonaws.services.s3.model.*;
import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3ConnWrapper;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class CephS3DataWriterImpl extends ScmDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataWriterImpl.class);
    private CephS3ConnWrapper conn;
    private final String bucketName;
    private final String key;
    private final CephS3DataService dataService;
    private String uploadID;

    // NOTE:CephS3Client.uploadPart() require part size must gte 5M
    private final int PART_SIZE = 5 * 1024 * 1024;

    private byte[] buffer = new byte[PART_SIZE];

    private int bufferOff = 0;
    private int fileSize;
    private int partNum = 1;

    private List<PartETag> tags = new ArrayList<>();

    public CephS3DataWriterImpl(String bucketName, String key, ScmService service)
            throws CephS3Exception {
        this.bucketName = bucketName;
        this.key = key;
        this.dataService = (CephS3DataService) service;
        this.conn = dataService.getConn();
        if (conn == null) {
            throw new CephS3Exception(
                    "construct CephS3DataWriterImpl failed, cephs3 is down:bucketName=" + bucketName
                            + ",key=" + key);
        }
        try {
            initUpload(conn);
        }
        catch (Exception e) {
            conn = dataService.releaseAndTryGetAnotherConn(conn);
            if (conn == null) {
                releaseResource();
                throw e;
            }
            logger.warn(
                    "write data failed, get another ceph conn to try again: bucketName={}, key={}, conn={}",
                    bucketName, key, conn.getUrl(), e);
            initUpload(conn);
        }
    }

    private void initUpload(CephS3ConnWrapper cephS3ConnWrapper) throws CephS3Exception {
        try {
            InitiateMultipartUploadResult initResp = cephS3ConnWrapper
                    .initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, key));
            this.uploadID = initResp.getUploadId();
        }
        catch (CephS3Exception e) {
            if (e.getS3StatusCode() == CephS3Exception.STATUS_NOT_FOUND
                    && (e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_BUCKET)
                            // nautilus ceph s3 会抛出这个异常表示 bucket 不存在
                            || e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_KEY))) {
                cephS3ConnWrapper.createBucket(bucketName);
                initUpload(cephS3ConnWrapper);
            }
            else {
                throw e;
            }
        }
    }

    @Override
    public void write(byte[] content) throws CephS3Exception {
        write(content, 0, content.length);
    }

    @Override
    public void write(byte[] content, int offset, int len) throws CephS3Exception {
        try {
            fileSize += len;
            while (len >= buffer.length - bufferOff) {
                int writeSize = buffer.length - bufferOff;
                System.arraycopy(content, offset, buffer, bufferOff, writeSize);

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
            logger.error("write data failed:bucketName=" + bucketName + ",key=" + key);
            throw new CephS3Exception("write data failed:bucketName=" + bucketName + ",key=" + key,
                    e);
        }
    }

    private void sendAndClearBuffer() throws CephS3Exception {
        try {
            UploadPartResult resp = conn.uploadPart(new UploadPartRequest()
                    .withBucketName(bucketName).withInputStream(new ByteArrayInputStream(buffer))
                    .withKey(key).withPartNumber(partNum).withPartSize(PART_SIZE)
                    .withUploadId(uploadID));
            tags.add(resp.getPartETag());
        }
        catch (CephS3Exception e) {
            logger.error("upload part failed:bucketName=" + bucketName + ",key=" + key + ",part="
                    + partNum);
            throw e;
        }
        partNum++;
        bufferOff = 0;
    }

    @Override
    public void cancel() {
        try {
            conn.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, key, uploadID));
        }
        catch (Exception e) {
            logger.warn("cancel writer failed:bucketName=" + bucketName + ",key=" + key, e);
        }
        releaseResource();
    }

    private void releaseResource() {
        buffer = null;
        tags = null;
        if (conn != null) {
            dataService.releaseConn(conn);
        }
    }

    @Override
    public void close() throws CephS3Exception {
        try {
            if (bufferOff > 0 || tags.size() <= 0) {
                UploadPartResult resp = conn.uploadPart(new UploadPartRequest()
                        .withBucketName(bucketName).withKey(key)
                        .withInputStream(new ByteArrayInputStream(buffer, 0, bufferOff))
                        .withPartSize(bufferOff).withPartNumber(partNum).withUploadId(uploadID));
                tags.add(resp.getPartETag());
            }

            conn.completeMultipartUpload(
                    new CompleteMultipartUploadRequest(bucketName, key, uploadID, tags));
        }
        catch (Exception e) {
            logger.error("close data writer failed:bucketName" + bucketName + ",key=" + key);
            throw new CephS3Exception(
                    "close data writer failed:bucketName" + bucketName + ",key=" + key, e);
        }
        finally {
            dataService.releaseConn(conn);
        }
    }

    @Override
    public long getSize() {
        return fileSize;
    }

    @Override
    public String getCreatedTableName() {
        // TODO:no record now!
        return null;
    }

}
