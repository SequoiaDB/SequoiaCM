package com.sequoiacm.config.framework.bucket.operator;

import com.sequoiacm.config.framework.bucket.dao.BucketDao;
import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.config.framework.workspace.operator.ScmWorkspaceConfOperator;
import com.sequoiacm.config.framework.workspace.operator.ScmWorkspaceListener;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfig;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfigUpdater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScmBucketConfOperator implements ScmConfOperator {

    private BucketDao bucketDao;
    @Autowired
    private DefaultVersionDao versionDao;

    private List<ScmBucketListener> bucketListeners = new ArrayList<>();

    private ScmBucketConfOperator(ScmWorkspaceConfOperator wsOp, final BucketDao bucketDao) {
        wsOp.registerWorkspaceListener(new ScmWorkspaceListener() {
            @Override
            public void afterWorkspaceDelete(String wsName) {
                bucketDao.deleteBucketMetaSilence(wsName);
            }
        });
        this.bucketDao = bucketDao;
    }

    @Override
    public List<Version> getConfVersion(VersionFilter filter) throws ScmConfigException {
        DefaultVersionFilter versionFilter = (DefaultVersionFilter) filter;
        return versionDao.getVerions(versionFilter);
    }

    @Override
    public ScmConfOperateResult createConf(Config config) throws ScmConfigException {
        return bucketDao.createBucket((BucketConfig) config);
    }

    @Override
    public ScmConfOperateResult deleteConf(ConfigFilter filter) throws ScmConfigException {
        BucketConfigFilter bucketConfigFilter = (BucketConfigFilter) filter;
        ScmConfOperateResult operateResult = bucketDao.deleteBucket(bucketConfigFilter);
        for (ScmBucketListener bucketListener : bucketListeners) {
            bucketListener.afterBucketDelete(bucketConfigFilter.getBucketName());
        }
        return operateResult;
    }

    @Override
    public List<Config> getConf(ConfigFilter filter) throws ScmConfigException {
        return bucketDao.getBuckets((BucketConfigFilter) filter);
    }

    @Override
    public MetaCursor listConf(ConfigFilter filter) throws ScmConfigException {
        return bucketDao.listBuckets((BucketConfigFilter) filter);
    }

    @Override
    public long countConf(ConfigFilter filter) throws ScmConfigException {
        return bucketDao.countBucket((BucketConfigFilter) filter);
    }

    @Override
    public ScmConfOperateResult updateConf(ConfigUpdator updator) throws ScmConfigException {
        return  bucketDao.updateBucket((BucketConfigUpdater) updator);
    }

    public void registerBucketListener(ScmBucketListener listener) {
        bucketListeners.add(listener);
    }

}