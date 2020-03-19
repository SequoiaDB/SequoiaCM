package com.sequoiacm.s3.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.core.Bucket;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.model.GetServiceResult;
import com.sequoiacm.s3.model.LocationConstraint;
import com.sequoiacm.s3.model.Owner;
import com.sequoiacm.s3.remote.ScmClientFactory;
import com.sequoiacm.s3.remote.ScmContentServerClient;
import com.sequoiacm.s3.remote.ScmDirInfo;
import com.sequoiacm.s3.remote.ScmWsInfo;
import com.sequoiacm.s3.service.BucketService;
import com.sequoiacm.s3.service.ObjectService;
import com.sequoiacm.s3.utils.DataFormatUtils;

@Service
@ConditionalOnProperty(prefix = "scm.s3", name = "mode", havingValue = "compatibility", matchIfMissing = false)
public class CompatibilityBucketServiceImpl implements BucketService {
    private static final String CompatibilityBucketNamePrefix = "ws-";
    @Autowired
    private ScmClientFactory clientFactory;
    @Autowired
    private ObjectService objService;

    // /
    // ws-id   

    // /bucket1
    // ws-id-bucket1 

    @Override
    public void createBucket(ScmSession session, String bucketName, String region)
            throws S3ServerException {
        ScmContentServerClient client = clientFactory.getContentServerClient(session, null);
        Bucket bucket = parseBucketName(bucketName, client);
        if (region != null && !region.equals(bucket.getWorkspace())) {
            throw new S3ServerException(S3Error.BUCKET_INVALID_BUCKETNAME,
                    "region name parse from bucket is " + bucket.getWorkspace()
                            + ", specified region name is " + region);

        }
        client = clientFactory.getContentServerClient(session, bucket.getWorkspace());
        if (!bucket.getBucketDir().equals(S3CommonDefine.SCM_DIR_SEP)) {
            try {
                client.createDir(bucket.getBucketDir());
            }
            catch (ScmFeignException e) {
                if (e.getStatus() == ScmError.DIR_EXIST.getErrorCode()) {
                    throw new S3ServerException(S3Error.BUCKET_ALREADY_EXIST,
                            "bucket already exist: dir=" + bucket.getBucketDir());
                }
                throw new S3ServerException(S3Error.BUCKET_CREATE_FAILED,
                        "create scm dir failed:bucket=" + bucketName + ", dir="
                                + bucket.getBucketDir(),
                        e);
            }
        }
        else {
            throw new S3ServerException(S3Error.BUCKET_ALREADY_EXIST,
                    "bucket already exist: dir=" + bucket.getBucketName());
        }
    }

    private Bucket parseBucketName(String bucketName, ScmContentServerClient client)
            throws S3ServerException {
        // bucket name: ws-wsId-dirName
        Bucket bucket = new Bucket();
        bucket.setBucketName(bucketName);

        if (!bucketName.startsWith(CompatibilityBucketNamePrefix)) {
            throw new S3ServerException(S3Error.BUCKET_INVALID_BUCKETNAME,
                    "bucket name must be start with 'ws-wsId-XXX'");
        }
        bucketName = bucketName.substring(CompatibilityBucketNamePrefix.length());
        try {
            int wsEndIdx = bucketName.indexOf("-");
            if (wsEndIdx == -1) {
                int wsId = Integer.valueOf(bucketName);
                ScmWsInfo ws = client.getWorkspaceById(wsId);
                if (ws == null) {
                    throw new S3ServerException(S3Error.BUCKET_INVALID_BUCKETNAME,
                            "workspace not exist:wsId=" + wsId + ", bucket="
                                    + bucket.getBucketName());
                }
                bucket.setWorkspace(ws.getName());
                bucket.setBucketDir(S3CommonDefine.SCM_DIR_SEP);
                return bucket;
            }
            if (wsEndIdx >= bucketName.length() - 1) {
                throw new S3ServerException(S3Error.BUCKET_INVALID_BUCKETNAME,
                        "bucket name must be start with 'ws-wsId-XXX':bucketName="
                                + bucket.getBucketName());
            }
            String wsIdStr = bucketName.substring(0, wsEndIdx);
            if (wsIdStr.isEmpty()) {
                throw new S3ServerException(S3Error.BUCKET_INVALID_BUCKETNAME,
                        "bucket name must be start with 'ws-wsId-XXX':bucketName="
                                + bucket.getBucketName());
            }
            String dirName = bucketName.substring(wsEndIdx + 1, bucketName.length());
            if (!ScmArgChecker.Directory.checkDirectoryName(dirName)) {
                throw new S3ServerException(S3Error.BUCKET_INVALID_BUCKETNAME,
                        "bucket name can not contain invalid char:" + bucket.getBucketName());
            }
            int wsId = Integer.valueOf(wsIdStr);
            ScmWsInfo ws = client.getWorkspaceById(wsId);
            if (ws == null) {
                throw new S3ServerException(S3Error.REGION_NO_SUCH_REGION,
                        "no such region, workspace not exist:id=" + wsId);
            }
            bucket.setBucketDir(S3CommonDefine.SCM_DIR_SEP + dirName);
            bucket.setWorkspace(ws.getName());
            return bucket;
        }
        catch (ScmFeignException e) {
            throw new S3ServerException(S3Error.REGION_GET_FAILED,
                    "get workspace from scm failed: bucket=" + bucketName, e);
        }
        catch (NumberFormatException e) {
            throw new S3ServerException(S3Error.BUCKET_INVALID_BUCKETNAME,
                    "bucket name must be start with 'workspaceIntId-XXX' :" + bucketName, e);
        }
    }

    @Override
    public void deleteBucket(ScmSession session, String bucketName) throws S3ServerException {
        Bucket bucket = getBucket(session, bucketName);
        // is bucket empty
        if (!objService.isEmptyBucket(session, bucket)) {
            throw new S3ServerException(S3Error.BUCKET_NOT_EMPTY,
                    "The bucket you tried to delete is not empty. bucket name = " + bucketName);
        }
        ScmContentServerClient client = clientFactory.getContentServerClient(session,
                bucket.getWorkspace());
        try {
            client.deleteDirRecursive(bucket.getBucketDir());
        }
        catch (S3ServerException e) {
            if (e.getError() == S3Error.SCM_DELETE_DIR_NOTEMPTY) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EMPTY,
                        "The bucket you tried to delete is not empty. bucket name = " + bucketName);
            }
            throw e;
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.BUCKET_DELETE_FAILED,
                    "failed to delete bucket:" + bucketName, e);
        }
    }

    @Override
    public GetServiceResult getService(ScmSession session) throws S3ServerException {
        GetServiceResult ret = new GetServiceResult();
        List<Bucket> buckets = new ArrayList<>();
        try {
            ScmContentServerClient client = clientFactory.getContentServerClient(session, null);
            List<ScmWsInfo> wsList = client.getWorkspaceList();
            for (ScmWsInfo ws : wsList) {
                Bucket bucket = new Bucket();
                bucket.setBucketDir(S3CommonDefine.SCM_DIR_SEP);
                bucket.setBucketName(CompatibilityBucketNamePrefix + ws.getId());
                bucket.setCreateDate(DataFormatUtils.formatDate(ws.getCreateTime()));
                bucket.setWorkspace(ws.getName());
                buckets.add(bucket);

                client = clientFactory.getContentServerClient(session, ws.getName());
                List<ScmDirInfo> dirs = client.getDirs(CommonDefine.Directory.SCM_ROOT_DIR_ID, null,
                        null, -1);
                for (ScmDirInfo dir : dirs) {
                    bucket = new Bucket();
                    bucket.setBucketDir(S3CommonDefine.SCM_DIR_SEP + dir.getName());
                    bucket.setBucketName(
                            CompatibilityBucketNamePrefix + ws.getId() + "-" + dir.getName());
                    bucket.setCreateDate(DataFormatUtils.formatDate(dir.getCreateTime()));
                    bucket.setWorkspace(ws.getName());
                    buckets.add(bucket);
                }
            }
        }
        catch (ScmFeignException e) {
            throw new S3ServerException(S3Error.BUCKET_GET_SERVICE_FAILED, "get bucket list failed",
                    e);
        }
        ret.setBuckets(buckets);
        Owner o = new Owner();
        o.setUserName("null");
        o.setUserId("null");
        ret.setOwner(o);
        return ret;
    }

    @Override
    public Bucket getBucket(ScmSession session, String bucketName) throws S3ServerException {
        ScmContentServerClient client = clientFactory.getContentServerClient(session, null);
        Bucket bucket = null;
        try {
            bucket = parseBucketName(bucketName, client);
        }
        catch (S3ServerException e) {
            if (e.getError() == S3Error.REGION_NO_SUCH_REGION
                    || e.getError() == S3Error.BUCKET_INVALID_BUCKETNAME) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                        "bucket not exist:" + bucketName, e);
            }

            throw e;
        }
        client = clientFactory.getContentServerClient(session, bucket.getWorkspace());
        try {
            ScmDirInfo dir = client.getDir(bucket.getBucketDir());
            bucket.setCreateDate(DataFormatUtils.formatDate(dir.getCreateTime()));
            return bucket;
        }
        catch (ScmFeignException e) {
            if (e.getStatus() == ScmError.DIR_NOT_FOUND.getErrorCode()) {
                throw new S3ServerException(S3Error.BUCKET_NOT_EXIST,
                        "bucket not exist:bucket=" + bucketName + ", dir=" + bucket.getBucketDir());
            }
            throw new S3ServerException(S3Error.SCM_GET_DIR_FAILED,
                    "get bucket failed: bucket=" + bucketName + ", dir=" + bucket.getBucketDir());
        }
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
