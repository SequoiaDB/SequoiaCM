package com.sequoiacm.cephs3.dataservice;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.cephs3.CephS3Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CephS3ConnWrapper {
    private static final Logger logger = LoggerFactory.getLogger(CephS3ConnWrapper.class);
    private final int id;

    private CephS3Conn conn;
    private boolean hasFatalError;

    public CephS3ConnWrapper(int id, CephS3Conn conn) {
        this.conn = conn;
        this.id = id;
    }

    public String getUrl() {
        return conn.getUrl();
    }

    public int getId() {
        return id;
    }

    public boolean hasFatalError() {
        return hasFatalError;
    }

    private void checkFatalError(Exception e) {
        // 1. service could not be contacted for a response,
        // 2. client is unable to parse the response from service.
        if (e.getClass().equals(SdkClientException.class)) {
            hasFatalError = true;
        }
    }

    /**
     * Bucket names should not contain underscores Bucket names should be between 3
     * and 63 characters long Bucket names should not end with a dash Bucket names
     * cannot contain adjacent periods Bucket names cannot contain dashes next to
     * periods (e.g., "my-.bucket.com" and "my.-bucket" are invalid) Bucket names
     * cannot contain uppercase characters
     **/
    public void createBucket(String bucketName) throws CephS3Exception {
        try {
            logger.info("creating bucket:bucketName=" + bucketName);
            conn.getAmzClient().createBucket(bucketName);
        }
        catch (AmazonServiceException e) {
            checkFatalError(e);
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "create bucket failed:siteId=" + conn.getSiteId() + ",bucketName=" + bucketName,
                    e);
        }
        catch (Exception e) {
            checkFatalError(e);
            throw new CephS3Exception(
                    "create bucket failed:siteId=" + conn.getSiteId() + ",bucketName=" + bucketName,
                    e);
        }
    }

    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest req)
            throws CephS3Exception {
        try {
            return conn.getAmzClient().initiateMultipartUpload(req);
        }
        catch (AmazonServiceException e) {
            checkFatalError(e);
            if (e.getStatusCode() == CephS3Exception.STATUS_NOT_FOUND
                    && (e.getErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_BUCKET)
                            // nautilus ceph s3 会抛出这个异常表示 bucket 不存在
                            || e.getErrorCode().equals(CephS3Exception.ERR_CODE_NO_SUCH_KEY))) {
                throw new CephS3Exception(e.getStatusCode(),
                        CephS3Exception.ERR_CODE_NO_SUCH_BUCKET,
                        "bucket is not exist:siteId=" + conn.getSiteId() + ", bucket="
                                + req.getBucketName() + ", key=" + req.getKey(),
                        e);
            }
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "initiate multipart upload failed:siteId=" + conn.getSiteId() + ", bucket="
                            + req.getBucketName() + ", key=" + req.getKey(),
                    e);
        }
        catch (Exception e) {
            checkFatalError(e);
            throw new CephS3Exception("initiate multipart upload failed:siteId=" + conn.getSiteId()
                    + ", bucket=" + req.getBucketName() + ", key=" + req.getKey(), e);
        }
    }

    public UploadPartResult uploadPart(UploadPartRequest req) throws CephS3Exception {
        try {
            return conn.getAmzClient().uploadPart(req);
        }
        catch (AmazonServiceException e) {
            checkFatalError(e);
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "upload part failed:siteId=" + conn.getSiteId() + ", bucket="
                            + req.getBucketName() + ", key=" + req.getKey(),
                    e);
        }
        catch (Exception e) {
            checkFatalError(e);
            throw new CephS3Exception("upload part failed:siteId=" + conn.getSiteId() + ", bucket="
                    + req.getBucketName() + ", key=" + req.getKey(), e);
        }
    }

    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest req)
            throws CephS3Exception {
        try {
            return conn.getAmzClient().completeMultipartUpload(req);
        }
        catch (AmazonServiceException e) {
            checkFatalError(e);
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "complete multipart upload failed:siteId=" + conn.getSiteId() + ", bucket="
                            + req.getBucketName() + ", key=" + req.getKey(),
                    e);
        }
        catch (Exception e) {
            checkFatalError(e);
            throw new CephS3Exception("complete multipart upload failed:siteId=" + conn.getSiteId()
                    + ", bucket=" + req.getBucketName() + ", key=" + req.getKey(), e);
        }
    }

    public S3Object getObject(GetObjectRequest req) throws CephS3Exception {
        S3Object obj;
        try {
            obj = conn.getAmzClient().getObject(req);
        }
        catch (AmazonServiceException e) {
            checkFatalError(e);
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "get object failed:siteId=" + conn.getSiteId() + ", bucket="
                            + req.getBucketName() + ", key=" + req.getKey(),
                    e);
        }
        catch (Exception e) {
            checkFatalError(e);
            throw new CephS3Exception("get object failed:siteId=" + conn.getSiteId() + ", bucket="
                    + req.getBucketName() + ", key=" + req.getKey(), e);
        }

        return obj;
    }

    public void abortMultipartUpload(AbortMultipartUploadRequest req) throws CephS3Exception {
        try {
            conn.getAmzClient().abortMultipartUpload(req);
        }
        catch (AmazonServiceException e) {
            checkFatalError(e);
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "abort multipart upload failed:siteId=" + conn.getSiteId() + ", bucket="
                            + req.getBucketName() + ", key=" + req.getKey(),
                    e);
        }
        catch (Exception e) {
            checkFatalError(e);
            throw new CephS3Exception("abort multipart upload failed:siteId=" + conn.getSiteId()
                    + ", bucket=" + req.getBucketName() + ", key=" + req.getKey(), e);
        }
    }

    public void deleteObject(DeleteObjectRequest req) throws CephS3Exception {
        try {
            conn.getAmzClient().deleteObject(req);
        }
        catch (AmazonServiceException e) {
            checkFatalError(e);
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "delete object failed:siteId=" + conn.getSiteId() + ", bucket="
                            + req.getBucketName() + ", key=" + req.getKey(),
                    e);
        }
        catch (Exception e) {
            checkFatalError(e);
            throw new CephS3Exception("delete object failed:siteId=" + conn.getSiteId()
                    + ", bucket=" + req.getBucketName() + ", key=" + req.getKey(), e);
        }
    }

    public void shutdown() {
        try {
            conn.shutdown();
        }
        catch (Exception e) {
            logger.warn("failed to shutdown connection", e);
        }
    }

    public void closeObj(S3Object obj) {
        if (obj != null) {
            try {
                obj.close();
            }
            catch (IOException e) {
                checkFatalError(e);
                logger.warn("failed to close obj:bucket={}, key={}", obj.getBucketName(),
                        obj.getKey(), e);
            }
        }
    }

    public List<PartSummary> listPart(String bucketName, String key, String uploadId)
            throws CephS3Exception {
        ArrayList<PartSummary> ret = new ArrayList<>();
        ListPartsRequest req = new ListPartsRequest(bucketName, key, uploadId);
        PartListing c;
        try {
            do {
                c = conn.getAmzClient().listParts(req);
                ret.addAll(c.getParts());
            }
            while (c.isTruncated());
        }
        catch (AmazonServiceException e) {
            checkFatalError(e);
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "failed to get part list:siteId=" + conn.getSiteId() + ", bucket="
                            + req.getBucketName() + ", key=" + req.getKey() + ", uploadId="
                            + req.getUploadId(),
                    e);
        }
        catch (Exception e) {
            checkFatalError(e);
            throw new CephS3Exception("failed to get part list:siteId=" + conn.getSiteId()
                    + ", bucket=" + req.getBucketName() + ", key=" + req.getKey() + ", uploadId="
                    + req.getUploadId(), e);
        }
        return ret;
    }

    public void putObject(String bucketName, String key, InputStream data, int dataLen)
            throws CephS3Exception {
        ObjectMetadata objMeta = new ObjectMetadata();
        objMeta.setContentLength(dataLen);
        try {
            conn.getAmzClient().putObject(bucketName, key, data, objMeta);
        }
        catch (AmazonServiceException e) {
            checkFatalError(e);
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "failed to put object:siteId=" + conn.getSiteId() + ", bucket=" + bucketName
                            + ", key=" + key + ", dataLen=" + dataLen,
                    e);
        }
        catch (Exception e) {
            checkFatalError(e);
            throw new CephS3Exception("failed to put object:siteId=" + conn.getSiteId()
                    + ", bucket=" + bucketName + ", key=" + key + ", dataLen=" + dataLen, e);
        }
    }
}
