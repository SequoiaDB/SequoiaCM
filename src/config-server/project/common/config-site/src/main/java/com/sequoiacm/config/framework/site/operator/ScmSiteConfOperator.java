package com.sequoiacm.config.framework.site.operator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.config.framework.site.dao.AmendSiteVersionDao;
import com.sequoiacm.config.framework.site.dao.CreateSiteDao;
import com.sequoiacm.config.framework.site.dao.DeleteSiteDao;
import com.sequoiacm.config.framework.site.dao.GetSiteDao;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.site.SiteConfig;

@Component
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
    public ScmSiteConfOperator(AmendSiteVersionDao siteAmender) throws ScmConfigException {
        // for compatibility, check and insert SITE version.
        siteAmender.amend();
    }

    @Override
    public List<Version> getConfVersion(VersionFilter filter) throws ScmConfigException {
        DefaultVersionFilter versionFilter = (DefaultVersionFilter) filter;
        return versionDao.getVerions(versionFilter);
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
        return siteFinder.get(filter);
    }

    @Override
    public ScmConfOperateResult updateConf(ConfigUpdator updator) throws ScmConfigException {
        throw new ScmConfigException(ScmConfError.UNSUPPORTED_OPTION,
                "unsupport to update site info");
    }

}