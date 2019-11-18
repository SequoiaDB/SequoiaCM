package com.sequoiacm.cephs3.dataoperation;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.cephs3.dataservice.CephS3DataService;
import com.sequoiacm.datasource.dataoperation.ScmDataWriter;
import com.sequoiacm.datasource.dataservice.ScmService;

public class CephS3DataWriterImpl extends ScmDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataWriterImpl.class);
    private String bucketName;
    private String key;
    private CephS3DataService dataService;
    private String uploadID;

    // NOTE:CephS3Client.uploadPart() require part size must gte 5M
    private final int PART_SIZE = 5 * 1024 * 1024;

    private byte[] buffer = new byte[PART_SIZE];

    private int bufferOff = 0;
    private int fileSize;
    private int partNum = 1;

    private List<PartETag> tags = new ArrayList<PartETag>();

    public CephS3DataWriterImpl(String bucketName, String key, ScmService service) throws CephS3Exception {
        try {
            this.bucketName = bucketName;
            this.key = key;
            this.dataService = (CephS3DataService)service;
            initUpload();
        }
        catch (CephS3Exception e) {
            logger.error("construct CephS3DataWriterImpl failed:bucketName=" + bucketName + ",key="
                    + key);
            releaseResource();
            throw e;
        }
        catch (Exception e) {
            logger.error("construct CephS3DataWriterImpl failed:bucketName=" + bucketName + ",key="
                    + key);
            releaseResource();
            throw new CephS3Exception(
                    "construct CephS3DataWriterImpl failed:bucketName=" + bucketName + ",key="
                            + key,
                            e);
        }
    }

    private void initUpload() throws CephS3Exception {
        // NOTE:can not prevent multiple threads create the same object
        S3Object obj = null;
        try {
            obj = dataService.getObject(new GetObjectRequest(bucketName, key));
            throw new CephS3Exception(CephS3Exception.ERR_CODE_OBJECT_EXIST,
                    "file data exist:bucketName=" + bucketName + ",key=" + key);
        }
        catch (CephS3Exception e) {
            if (e.getS3StatusCode() != CephS3Exception.STATUS_NOT_FOUND) {
                throw e;
            }
            if (!e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_BUCKET)
                    && !e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_KEY)) {
                throw e;
            }
        }
        finally {
            dataService.closeObject(obj);
        }

        try {
            InitiateMultipartUploadResult initResp = dataService
                    .initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, key));
            this.uploadID = initResp.getUploadId();
        }
        catch (CephS3Exception e) {
            if (e.getS3StatusCode() == CephS3Exception.STATUS_NOT_FOUND
                    && e.getS3ErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_BUCKET)) {
                dataService.createBucket(bucketName);
                initUpload();
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
            throw new CephS3Exception(
                    "write data failed:bucketName=" + bucketName + ",key=" + key, e);
        }
    }

    private void sendAndClearBuffer() throws CephS3Exception {
        try {
            UploadPartResult resp = dataService.uploadPart(new UploadPartRequest()
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
            dataService.abortMultipartUpload(
                    new AbortMultipartUploadRequest(bucketName, key, uploadID));
        }
        catch (Exception e) {
            logger.warn("cancel writer failed:bucketName=" + bucketName + ",key=" + key, e);
        }
        releaseResource();
    }

    private void releaseResource() {
        buffer = null;
        tags = null;
    }

    @Override
    public void close() throws CephS3Exception {
        try {
            if (bufferOff > 0 || tags.size() <=0) {
                UploadPartResult resp = dataService
                        .uploadPart(
                                new UploadPartRequest().withBucketName(bucketName).withKey(key)
                                .withInputStream(
                                        new ByteArrayInputStream(buffer, 0, bufferOff))
                                .withPartSize(bufferOff).withPartNumber(partNum).withUploadId(uploadID));
                tags.add(resp.getPartETag());
            }

            dataService.completeMultipartUpload(
                    new CompleteMultipartUploadRequest(bucketName, key, uploadID, tags));
        }
        catch (CephS3Exception e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("close data writer failed:bucketName" + bucketName + ",key=" + key);
            releaseResource();
            throw new CephS3Exception(
                    "close data writer failed:bucketName" + bucketName + ",key=" + key, e);
        }
    }

    @Override
    public long getSize() {
        return fileSize;
    }

    @Override
    public String getCreatedTableName() {
        //TODO:no record now!
        return null;
    }

}
