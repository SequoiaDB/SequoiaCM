package com.sequoiacm.contentserver.bucket;

import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.client.cache.bucket.BucketConfCache;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BucketInfoManager {
    @Autowired
    private BucketConfCache bucketConfCache;

    private ScmBucket convertBucketConf(BucketConfig ret) {
        ScmBucketVersionStatus versionStatus = ScmBucketVersionStatus.parse(ret.getVersionStatus());
        if (versionStatus == null) {
            throw new IllegalArgumentException("invalid version status:" + ret.toString());
        }
        return new ScmBucket(ret.getName(), ret.getId(), ret.getCreateTime(), ret.getCreateUser(),
                ret.getWorkspace(), ret.getFileTable(), versionStatus, ret.getCustomTag(),
                ret.getUpdateUser(), ret.getUpdateTime(), this);
    }

    public ScmBucket getBucketById(long id) throws ScmServerException {
        BucketConfig bucketConf = null;
        try {
            bucketConf = bucketConfCache.getBucketById(id);
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to get bucket, id:" + id, e);
        }
        if (bucketConf == null) {
            return null;
        }
        return convertBucketConf(bucketConf);

    }

    public ScmBucket getBucket(String name) throws ScmServerException {
        BucketConfig b = null;
        try {
            b = bucketConfCache.getBucket(name);
        }
        catch (ScmConfigException e) {
            throw new ScmServerException(ScmError.CONFIG_SERVER_ERROR,
                    "failed to get bucket, name:" + name, e);
        }
        if (b != null) {
            return convertBucketConf(b);
        }

        return null;
    }

    public void invalidateBucketCache(String name) {
        bucketConfCache.invalidateBucketCache(name);
    }


}
