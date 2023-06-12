package com.sequoiacm.config.framework.config.site.operator;

import java.util.List;

import com.sequoiacm.config.framework.config.site.dao.AmendSiteVersionDao;
import com.sequoiacm.config.framework.config.site.dao.CreateSiteDao;
import com.sequoiacm.config.framework.config.site.dao.DeleteSiteDao;
import com.sequoiacm.config.framework.config.site.dao.GetSiteDao;
import com.sequoiacm.config.framework.config.site.dao.UpdateSiteDao;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdater;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteConfig;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteUpdater;

@Component
@BusinessType(ScmBusinessTypeDefine.SITE)
public class ScmSiteConfOperator implements ScmConfOperator {

    @Autowired
    private CreateSiteDao siteCreater;

    @Autowired
    private DeleteSiteDao siteDeleter;

    @Autowired
    private GetSiteDao siteFinder;
    @Autowired
    private DefaultVersionDao versionDao;

    @Autowired
    private UpdateSiteDao siteUpdator;

    @Autowired
    public ScmSiteConfOperator(AmendSiteVersionDao siteAmender) throws ScmConfigException {
        // for compatibility, check and insert SITE version.
        siteAmender.amend();
    }

    @Override
    public List<Version> getConfVersion(VersionFilter filter) throws ScmConfigException {
        return versionDao.getVerions(filter);
    }

    @Override
    public ScmConfOperateResult createConf(Config config) throws ScmConfigException {
        return siteCreater.create((SiteConfig) config);
    }

    @Override
    public ScmConfOperateResult deleteConf(ConfigFilter filter) throws ScmConfigException {
        return siteDeleter.delete(filter);
    }

    @Override
    public List<Config> getConf(ConfigFilter filter) throws ScmConfigException {
        return siteFinder.get((SiteFilter) filter);
    }

    @Override
    public ScmConfOperateResult updateConf(ConfigUpdater updator) throws ScmConfigException {
        return siteUpdator.update((SiteUpdater) updator);
    }

}