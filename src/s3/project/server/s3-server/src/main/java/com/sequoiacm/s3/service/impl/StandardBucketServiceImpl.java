package com.sequoiacm.s3.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.S3CommonDefine;
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
import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.client.core.ScmMapFactory;
import com.sequoiadb.infrastructure.map.client.service.MapFeignClient;
import com.sequoiadb.infrastructure.map.client.service.MapFeignClientFactory;

@Component
@ConditionalOnProperty(prefix = "scm.s3", name = "mode", havingValue = "standard", matchIfMissing = true)
public class StandardBucketServiceImpl implements BucketService {
    private static final Logger logger = LoggerFactory.getLogger(StandardBucketServiceImpl.class);
    private String defaultRegion;
    private String bucketsDir;
    private boolean allowReput;
    private Map<String, Bucket> bucketMap;

    @Autowired
    private ScmClientFactory clientFactory;

    @Autowired
    private ObjectService objService;

    @Autowired
    private MapFeignClientFactory mapClientFactory;

    @Autowired
    public StandardBucketServiceImpl(BucketConfig bucketConfig, RegionConfig regionConfig) {
        defaultRegion = regionConfig.getDefaultRegion();
        bucketsDir = bucketConfig.getBucketDir();
        allowReput = bucketConfig.getAllowreput();
    }

    private Map<String, Bucket> getBucketMap() throws S3ServerException {
        initMap();
        return bucketMap;
    }
    
    private void initMap() throws S3ServerException {
        MapFeignClient client = mapClientFactory
                .getFeignClientByServiceName(clientFactory.getRootSite());
        try {
            bucketMap = ScmMapFactory.getGroupMap(client, S3CommonDefine.S3_MAP_GROUP_NAME)
                    .createMap(S3CommonDefine.S3_MAP_BURKET_NAME, String.class, Bucket.class);
        }
        catch (ScmMapServerException e) {
            if (e.getError().equals(ScmMapError.MAP_TABLE_ALREADY_EXIST)) {
                try {
                    bucketMap = ScmMapFactory.getGroupMap(client, S3CommonDefine.S3_MAP_GROUP_NAME)
                            .getMap(S3CommonDefine.S3_MAP_BURKET_NAME);
                    return;
                }
                catch (Exception e1) {
                    throw new S3ServerException(S3Error.SYSTEM_ERROR, "failed to init burket map",
                            e1);
                }
            }
            throw new S3ServerException(S3Error.SYSTEM_ERROR, "failed to init burket map", e);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.SYSTEM_ERROR, "failed to init burket map", e);
        }
    }

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
        Bucket previous = getBucketMap().putIfAbsent(bucketName, bucket);
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
        // TODO:putObject和删除桶存在并发问题
        getBucketMap().remove(bucketName);
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
        Set<String> keySet = getBucketMap().keySet();
        for (String bucketName : keySet) {
            Bucket bucket = getBucketMap().get(bucketName);
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
        Bucket bucket = getBucketMap().get(bucketName);
        if (bucket == null) {
            throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                    "The specified bucket does not exist. bucket name = " + bucketName);
        }
        if (!bucket.getUserId().equals(session.getUser().getUserId())) {
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
