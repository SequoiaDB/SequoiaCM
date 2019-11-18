package com.sequoiacm.cephs3.dataservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.sequoiacm.cephs3.CephS3Exception;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;

public class CephS3DataService extends ScmService {
    private final String SIGNER_OVERRIDE = "S3SignerType";
    private AmazonS3 conn;
    private static final Logger logger = LoggerFactory.getLogger(CephS3DataService.class);

    public CephS3DataService(int siteId, ScmSiteUrl siteUrl) throws CephS3Exception {
        super(siteId, siteUrl);
        try {
            AuthInfo auth = ScmFilePasswordParser.parserFile(siteUrl.getPassword());
            AWSCredentials creden = new BasicAWSCredentials(siteUrl.getUser(), auth.getPassword());
            ClientConfiguration conf = new ClientConfiguration();
            conf.setProtocol(Protocol.HTTP);
            conf.setSignerOverride(SIGNER_OVERRIDE);
            conn = new AmazonS3Client(creden, conf);
            S3ClientOptions op = new S3ClientOptions();
            op.setPathStyleAccess(true);
            conn.setS3ClientOptions(op);
            conn.setEndpoint(siteUrl.getUrls().get(0));
            // try connect to s3.
            conn.getS3AccountOwner();
        }
        catch (Exception e) {
            throw new CephS3Exception(
                    "create CephS3DataService failed:siteId=" + siteId + ",siteUrl=" + siteUrl, e);
        }

    }

    @Override
    public void clear() {
        conn = null;
    }

    /**
     * Bucket names should not contain underscores Bucket names should be
     * between 3 and 63 characters long Bucket names should not end with a dash
     * Bucket names cannot contain adjacent periods Bucket names cannot contain
     * dashes next to periods (e.g., "my-.bucket.com" and "my.-bucket" are
     * invalid) Bucket names cannot contain uppercase characters
     **/
    public void createBucket(String bucketName) throws CephS3Exception {
        try {
            logger.info("creating bucket:bucketName=" + bucketName);
            conn.createBucket(bucketName);
        }
        catch (AmazonServiceException e) {
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "create bucket failed:siteId=" + siteId + ",bucketName=" + bucketName, e);
        }
        catch (Exception e) {
            throw new CephS3Exception(
                    "create bucket failed:siteId=" + siteId + ",bucketName=" + bucketName, e);
        }
    }

    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest req)
            throws CephS3Exception {
        try {
            return conn.initiateMultipartUpload(req);
        }
        catch (AmazonServiceException e) {
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "initiate multipart upload failed:siteId=" + siteId, e);
        }
        catch (Exception e) {
            throw new CephS3Exception("initiate multipart upload failed:siteId=" + siteId, e);
        }
    }

    public UploadPartResult uploadPart(UploadPartRequest req) throws CephS3Exception {
        try {
            return conn.uploadPart(req);
        }
        catch (AmazonServiceException e) {
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "upload part failed:siteId=" + siteId, e);
        }
        catch (Exception e) {
            throw new CephS3Exception("upload part failed:siteId=" + siteId, e);
        }
    }

    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest req)
            throws CephS3Exception {
        try {
            return conn.completeMultipartUpload(req);
        }
        catch (AmazonServiceException e) {
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "complete multipart upload failed:siteId=" + siteId, e);
        }
        catch (Exception e) {
            throw new CephS3Exception("complete multipart upload failed:siteId=" + siteId, e);
        }
    }

    public S3Object getObject(GetObjectRequest req) throws CephS3Exception {
        S3Object obj;
        try {
            obj = conn.getObject(req);
        }
        catch (AmazonServiceException e) {
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "get object failed:siteId=" + siteId, e);
        }
        catch (Exception e) {
            throw new CephS3Exception("get object failed:siteId=" + siteId, e);
        }

        if (obj == null) {
            throw new CephS3Exception(CephS3Exception.STATUS_NOT_FOUND,
                    CephS3Exception.ERR_CODE_NO_SUCH_KEY, "object not exist:siteId=" + siteId);
        }
        return obj;
    }

    public void abortMultipartUpload(AbortMultipartUploadRequest req) throws CephS3Exception {
        try {
            conn.abortMultipartUpload(req);
        }
        catch (AmazonServiceException e) {
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "abort multipart upload failed:siteId=" + siteId, e);
        }
        catch (Exception e) {
            throw new CephS3Exception("abort multipart upload failed:siteId=" + siteId, e);
        }
    }

    public void deleteObject(DeleteObjectRequest req) throws CephS3Exception {
        try {
            conn.deleteObject(req);
        }
        catch (AmazonServiceException e) {
            throw new CephS3Exception(e.getStatusCode(), e.getErrorCode(),
                    "delete object failed:siteId=" + siteId, e);
        }
        catch (Exception e) {
            throw new CephS3Exception("delete object failed:siteId=" + siteId, e);
        }
    }

    public void closeObject(S3Object obj) {
        if (obj != null) {
            try {
                obj.close();
            }
            catch (Exception e) {
                logger.warn("close S3Object failed:objectKey=" + obj.getKey(), e);
            }
        }
    }

    @Override
    public String getType() {
        return "ceph_s3";
    }

}
