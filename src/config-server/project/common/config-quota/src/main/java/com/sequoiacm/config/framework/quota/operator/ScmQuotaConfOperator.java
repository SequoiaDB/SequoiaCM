package com.sequoiacm.config.framework.quota.operator;

import com.sequoiacm.config.framework.bucket.operator.ScmBucketConfOperator;
import com.sequoiacm.config.framework.bucket.operator.ScmBucketListener;
import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.config.framework.quota.dao.QuotaDao;
import com.sequoiacm.config.framework.workspace.operator.ScmWorkspaceConfOperator;
import com.sequoiacm.config.framework.workspace.operator.ScmWorkspaceListener;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaConfig;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaFilter;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaUpdator;
import com.sequoiacm.infrastructure.config.core.msg.quota.QuotaVersionFilter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScmQuotaConfOperator implements ScmConfOperator {

    private QuotaDao quotaDao;

    private DefaultVersionDao versionDao;

    public ScmQuotaConfOperator(ScmBucketConfOperator bucketConfOperator,
            ScmWorkspaceConfOperator workspaceConfOperator, final QuotaDao quotaDao,
            DefaultVersionDao versionDao) {
        this.quotaDao = quotaDao;
        this.versionDao = versionDao;
        bucketConfOperator.registerBucketListener(new ScmBucketListener() {
            @Override
            public void afterBucketDelete(String bucketName) {
                quotaDao.deleteBucketQuotaSilence(bucketName);
            }
        });
        workspaceConfOperator.registerWorkspaceListener(new ScmWorkspaceListener() {
            @Override
            public void afterWorkspaceDelete(String wsName) {
                quotaDao.deleteWsBucketQuotaSilence(wsName);
            }
        });
    }

    @Override
    public List<Config> getConf(ConfigFilter filter) throws ScmConfigException {
        return quotaDao.getQuotas((QuotaFilter) filter);
    }

    @Override
    public List<Version> getConfVersion(VersionFilter filter) throws ScmConfigException {
        QuotaVersionFilter versionFilter = (QuotaVersionFilter) filter;
        return quotaDao.getVersions(versionFilter);
    }

    @Override
    public ScmConfOperateResult updateConf(ConfigUpdator config) throws ScmConfigException {
        return quotaDao.updateQuota((QuotaUpdator)config);
    }

    @Override
    public ScmConfOperateResult deleteConf(ConfigFilter filter) throws ScmConfigException {
        return quotaDao.deleteQuota((QuotaFilter)filter);
    }

    @Override
    public ScmConfOperateResult createConf(Config config) throws ScmConfigException {
        return quotaDao.crateQuota((QuotaConfig)config);
    }
}
