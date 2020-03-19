package com.sequoiacm.s3.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.config.BucketConfig;
import com.sequoiacm.s3.config.RegionConfig;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.GetServiceResult;
import com.sequoiacm.s3.model.LocationConstraint;
import com.sequoiacm.s3.model.Owner;
import com.sequoiacm.s3.model.ScmDirPath;
import com.sequoiacm.s3.remote.ScmClientFactory;
import com.sequoiacm.s3.remote.ScmContentServerClient;
import com.sequoiacm.s3.service.BucketService;
import com.sequoiacm.s3.service.ObjectService;
import com.sequoiacm.s3.utils.CommonUtil;
import com.sequoiacm.s3.utils.DataFormatUtils;

@Component
@ConditionalOnProperty(prefix = "scm.s3", name = "mode", havingValue = "standard", matchIfMissing = true)
public class StandardBucketServiceImpl implements BucketService {
    private static final Logger logger = LoggerFactory.getLogger(StandardBucketServiceImpl.class);
    private String defaultRegion;
    private String bucketsDir;
    private boolean allowReput;

    @Autowired
    public StandardBucketServiceImpl(BucketConfig bucketConfig, RegionConfig regionConfig) {
        defaultRegion = regionConfig.getDefaultRegion();
        bucketsDir = bucketConfig.getBucketDir();
        allowReput = bucketConfig.getAllowreput();
    }

    // TODO: map要有隔离的能力，类似CS隔离CL
    private Map<String, Bucket> bucketMap = new HashMap<>();

    @Autowired
    private ScmClientFactory clientFactory;
    @Autowired
    private ObjectService objService;

    @Override
    public void createBucket(ScmSession session, String bucketName, String region)
            throws S3ServerException {
        bucketName = bucketName.toLowerCase();
        if (region == null) {
            region = defaultRegion;
        }

        ScmContentServerClient client = clientFactory.getContentServerClient(session, region);
        String path = CommonUtil.concatPath(bucketsDir, bucketName);
        try {
            client.createDirOrGet(path);
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.DIR_NOT_FOUND.getErrorCode()) {
                createBucketsDir(session, region);
                try {
                    client.createDirOrGet(path);
                }
                catch (ScmFeignException e1) {
                    throw new S3ServerException(S3Error.BUCKET_CREATE_FAILED,
                            "failed to create scm dir:" + path, e1);
                }
            }
            else if (e.getStatus() == ScmError.WORKSPACE_NOT_EXIST.getErrorCode()) {
                throw new S3ServerException(S3Error.REGION_NO_SUCH_REGION,
                        "no such region. regionName:" + region);
            }
            else {
                throw new S3ServerException(S3Error.BUCKET_CREATE_FAILED,
                        "failed to create scm dir:" + path, e);
            }
        }

        Bucket bucket = new Bucket();
        bucket.setBucketDir(path);
        bucket.setBucketName(bucketName);
        bucket.setWorkspace(region);
        bucket.setCreateDate(DataFormatUtils.formatDate(System.currentTimeMillis()));
        bucket.setUserId(session.getUser().getUserId());
        Bucket previous = bucketMap.putIfAbsent(bucketName, bucket);
        if (previous == null) {
            return;
        }
        if (previous.getUserId().equals(bucket.getUserId())) {
            if (allowReput) {
                return;
            }
            throw new S3ServerException(S3Error.BUCKET_ALREADY_OWNED_BY_YOU,
                    "Bucket already owned you. bucket name=" + bucketName);
        }
        throw new S3ServerException(S3Error.BUCKET_ALREADY_EXIST,
                "Bucket already exist. bucket name=" + bucketName);
    }

    private void createBucketsDir(ScmSession session, String region) throws S3ServerException {
        ScmContentServerClient client = clientFactory.getContentServerClient(session, region);
        ScmDirPath path = new ScmDirPath(bucketsDir);
        for (int i = 2; i <= path.getLevel(); i++) {
            try {
                client.createDirOrGet(path.getPathByLevel(i));
            }
            catch (ScmFeignException e) {
                throw new S3ServerException(S3Error.BUCKET_CREATE_FAILED,
                        "failed to create bucket dir:" + bucketsDir, e);
            }
        }
    }

    @Override
    public void deleteBucket(ScmSession session, String bucketName) throws S3ServerException {
        bucketName = bucketName.toLowerCase();
        // get and check bucket
        Bucket bucket = getBucket(session, bucketName);

        // is bucket empty
        if (!objService.isEmptyBucket(session, bucket)) {
            throw new S3ServerException(S3Error.BUCKET_NOT_EMPTY,
                    "The bucket you tried to delete is not empty. bucket name = " + bucketName);
        }
        //TODO:putObject和删除桶存在并发问题
        bucketMap.remove(bucketName);
        ScmContentServerClient client = clientFactory.getContentServerClient(session,
                bucket.getWorkspace());
        try {
            client.deleteDirRecursive(bucket.getBucketDir());
        }
        catch (Exception e) {
            logger.warn("failed to delete bucket dir:{}", bucket.getBucketDir(), e);
        }
    }

    @Override
    public GetServiceResult getService(ScmSession session) throws S3ServerException {
        GetServiceResult ret = new GetServiceResult();
        List<Bucket> buckets = new ArrayList<>();
        for (Bucket bucket : bucketMap.values()) {
            if (bucket.getUserId().equals(session.getUser().getUserId())) {
                buckets.add(bucket);
            }
        }
        ret.setBuckets(buckets);
        Owner o = new Owner();
        o.setUserName(session.getUser().getUsername());
        o.setUserId(session.getUser().getUsername());
        ret.setOwner(o);
        return ret;
    }

    @Override
    public Bucket getBucket(ScmSession session, String bucketName) throws S3ServerException {
        bucketName = bucketName.toLowerCase();
        Bucket bucket = bucketMap.get(bucketName);
        if (bucket == null) {
            throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                    "The specified bucket does not exist. bucket name = " + bucketName);
        }
        if (bucket.getUserId() != session.getUser().getUserId()) {
            throw new S3ServerException(S3Error.ACCESS_DENIED,
                    "You can not access the specified bucket. bucket name = " + bucketName
                            + ", ownerID = " + bucket.getUserId());
        }

        return bucket;
    }

    @Override
    public void deleteBucketForce(ScmSession session, Bucket bucket) throws S3ServerException {
        throw new RuntimeException("unimplement");
    }

    @Override
    public LocationConstraint getBucketLocation(ScmSession session, String bucketName)
            throws S3ServerException {
        Bucket bucket = getBucket(session, bucketName);
        LocationConstraint ret = new LocationConstraint();
        ret.setLocation(bucket.getWorkspace());
        return ret;
    }

}
