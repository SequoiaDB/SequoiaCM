package com.sequoiacm.s3.service.impl;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.service.IScmBucketService;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.common.ScmObjectCursor;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.config.BucketConfig;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.ListBucketResult;
import com.sequoiacm.s3.model.LocationConstraint;
import com.sequoiacm.s3.model.Owner;
import com.sequoiacm.s3.model.VersioningConfiguration;
import com.sequoiacm.s3.model.VersioningConfigurationBase;
import com.sequoiacm.s3.service.BucketService;
import com.sequoiacm.s3.service.RegionService;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BucketServiceImpl implements BucketService {
    private static final Logger logger = LoggerFactory.getLogger(BucketServiceImpl.class);
    @Autowired
    private IScmBucketService scmBucketService;
    @Autowired
    private RegionService regionService;

    @Autowired
    private ScmAudit audit;

    @Autowired
    private BucketConfig config;

    @Override
    public void createBucket(ScmSession session, String bucketName, String region)
            throws S3ServerException {
        if (region == null) {
            region = regionService.getDefaultRegionForS3();
        }
        try {
            scmBucketService.createBucket(session.getUser(), region, bucketName);
            audit.info(ScmAuditType.CREATE_S3_BUCKET, session.getUser(), region, 0,
                    "create s3 bucket: bucketName=" + bucketName);
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.BUCKET_EXISTS) {
                try {
                    ScmBucket scmBucket = scmBucketService.getBucket(bucketName);
                    if (scmBucket.getCreateUser().equals(session.getUser().getUsername())) {
                        if (config.isAllowReput()) {
                            return;
                        }
                        throw new S3ServerException(S3Error.BUCKET_ALREADY_OWNED_BY_YOU,
                                "Bucket already owned you. bucket name=" + bucketName, e);
                    }
                    throw new S3ServerException(S3Error.BUCKET_ALREADY_EXIST,
                            "Bucket already exist. bucket name=" + bucketName, e);
                }
                catch (ScmServerException ex) {
                    logger.warn(
                            "failed to create bucket, because of bucket already exist, failed to check the owner of exists bucket: {}",
                            bucketName, ex);
                    throw new S3ServerException(S3Error.BUCKET_ALREADY_EXIST,
                            "Bucket already exist. bucket name=" + bucketName, e);
                }
            }
            if (e.getError() == ScmError.WORKSPACE_NOT_EXIST) {
                throw new S3ServerException(S3Error.REGION_NO_SUCH_REGION,
                        "failed to create bucket, no such region. regionName=" + region
                                + ", bucket=" + bucketName, e);
            }
            if (e.getError() == ScmError.OPERATION_UNAUTHORIZED) {
                throw new S3ServerException(S3Error.ACCESS_DENIED,
                        "You can not create the bucket. bucket name = " + bucketName, e);
            }
            throw new S3ServerException(S3Error.BUCKET_CREATE_FAILED,
                    "failed to create bucket: bucketName=" + bucketName + ", region=" + region, e);
        }
    }

    @Override
    public void deleteBucket(ScmSession session, String bucketName) throws S3ServerException {
        try {
            ScmBucket bucket = scmBucketService.deleteBucket(session.getUser(), bucketName);
            audit.info(ScmAuditType.DELETE_S3_BUCKET, session.getUser(), bucket.getWorkspace(), 0,
                    "delete s3 bucket: bucketName=" + bucketName);
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.BUCKET_NOT_EXISTS) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                        "The specified bucket does not exist. bucket name = " + bucketName, e);
            }
            if (e.getError() == ScmError.BUCKET_NOT_EMPTY) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EMPTY,
                        "The bucket you tried to delete is not empty. bucket name = " + bucketName, e);
            }
            if (e.getError() == ScmError.OPERATION_UNAUTHORIZED) {
                throw new S3ServerException(S3Error.ACCESS_DENIED,
                        "You can not access the specified bucket. bucket name = " + bucketName, e);
            }
            throw new S3ServerException(S3Error.BUCKET_DELETE_FAILED,
                    "Failed to delete bucket. bucket name = " + bucketName, e);
        }
    }

    @Override
    public ListBucketResult listBucket(ScmSession session) throws S3ServerException {
        ListBucketResult ret = new ListBucketResult();
        List<Bucket> bucketList = new ArrayList<>();
        BasicBSONObject matcher = new BasicBSONObject(FieldName.Bucket.CREATE_USER,
                session.getUser().getUsername());
        BasicBSONObject orderBy = new BasicBSONObject(FieldName.Bucket.NAME, 1);
        ScmObjectCursor<ScmBucket> cursor = null;
        try {
            cursor = scmBucketService.listBucket(session.getUser(), matcher, orderBy, 0, -1);
            while (cursor.hasNext()) {
                ScmBucket scmBucket = cursor.getNext();
                bucketList.add(new Bucket(scmBucket.getName(), scmBucket.getCreateTime(),
                        scmBucket.getCreateUser(), scmBucket.getWorkspace(),
                        scmBucket.getVersionStatus().name()));
            }
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.SYSTEM_ERROR,
                    "Failed to get bucket list: user=" + session.getUser().getUsername(), e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        ret.setBuckets(bucketList);
        ret.setOwner(new Owner(session.getUser().getUsername(), session.getUser().getUsername()));
        return ret;
    }

    @Override
    public Bucket getBucket(ScmSession session, String bucketName) throws S3ServerException {
        try {
            ScmBucket scmBucket = scmBucketService.getBucket(session.getUser(), bucketName);
            return new Bucket(scmBucket.getName(), scmBucket.getCreateTime(),
                    scmBucket.getCreateUser(), scmBucket.getWorkspace(),
                    scmBucket.getVersionStatus().name());
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.BUCKET_NOT_EXISTS) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                        "The specified bucket does not exist. bucket name = " + bucketName, e);
            }
            if (e.getError() == ScmError.OPERATION_UNAUTHORIZED) {
                throw new S3ServerException(S3Error.ACCESS_DENIED,
                        "You can not access the specified bucket. bucket name = " + bucketName, e);
            }
            throw new S3ServerException(S3Error.SYSTEM_ERROR, "failed to get bucket:" + bucketName,
                    e);
        }
    }

    @Override
    public LocationConstraint getBucketLocation(ScmSession session, String bucketName)
            throws S3ServerException {
        Bucket bucket = getBucket(session, bucketName);
        return new LocationConstraint(bucket.getRegion());
    }

    @Override
    public void setBucketVersionStatus(ScmSession session, String bucketName, String status)
            throws S3ServerException {
        try {
            ScmBucketVersionStatus versionStatus = ScmBucketVersionStatus.parse(status);
            if (versionStatus == null) {
                throw new S3ServerException(S3Error.BUCKET_INVALID_VERSIONING_STATUS,
                        "invalid status=" + status);
            }
            scmBucketService.updateBucketVersionStatus(session.getUser(), bucketName,
                    versionStatus);
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.BUCKET_NOT_EXISTS) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                        "The specified bucket does not exist. bucket name = " + bucketName, e);
            }
            if (e.getError() == ScmError.OPERATION_UNAUTHORIZED) {
                throw new S3ServerException(S3Error.ACCESS_DENIED,
                        "You can not access the specified bucket. bucket name = " + bucketName, e);
            }
            throw new S3ServerException(S3Error.BUCKET_VERSIONING_SET_FAILED,
                    "put bucket versioning failed. bucketname=" + bucketName + ",status=" + status,
                    e);
        }
        catch (S3ServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.BUCKET_VERSIONING_SET_FAILED,
                    "put bucket versioning failed. bucketname=" + bucketName + ",status=" + status,
                    e);
        }
    }

    @Override
    public VersioningConfigurationBase getBucketVersionStatus(ScmSession session, String bucketName)
            throws S3ServerException {
        try {
            Bucket s3Bucket = getBucket(session, bucketName);
            if (!s3Bucket.getVersionStatus().equals(ScmBucketVersionStatus.Disabled.name())) {
                VersioningConfiguration versioningCfg = new VersioningConfiguration();
                versioningCfg.setStatus(s3Bucket.getVersionStatus());
                return versioningCfg;
            }
            else {
                return new VersioningConfigurationBase();
            }
        }
        catch (S3ServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.BUCKET_VERSIONING_GET_FAILED,
                    "get bucket versioning failed. bucketname=" + bucketName, e);
        }

    }
}
