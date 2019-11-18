package com.sequoiacm.config.framework.node.operator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.node.dao.AmendNodeVersionDao;
import com.sequoiacm.config.framework.node.dao.CreateNodeDao;
import com.sequoiacm.config.framework.node.dao.DeleteNodeDao;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.node.NodeConfig;
import com.sequoiacm.infrastructure.config.core.msg.node.NodeFilter;

@Component
public class ScmNodeConfOperator implements ScmConfOperator {

    @Autowired
    private CreateNodeDao nodeCreater;

    @Autowired
    private DeleteNodeDao nodeDeleter;

    @Autowired
    private DefaultVersionDao versionDao;

    @Autowired
    public ScmNodeConfOperator(AmendNodeVersionDao nodeAmender) throws ScmConfigException {
        // for compatibility, check and insert NODE version.
        nodeAmender.amend();
    }

    @Override
    public ScmConfOperateResult createConf(Config config) throws ScmConfigException {
        return nodeCreater.create((NodeConfig) config);
    }

    @Override
    public ScmConfOperateResult deleteConf(ConfigFilter filter) throws ScmConfigException {
        return nodeDeleter.delete((NodeFilter) filter);
    }

    @Override
    public List<Version> getConfVersion(VersionFilter filter) throws ScmConfigException {
        DefaultVersionFilter versionFilter = (DefaultVersionFilter) filter;
        return versionDao.getVerions(versionFilter);
    }

    @Override
    public ScmConfOperateResult updateConf(ConfigUpdator config) throws ScmConfigException {
        throw new ScmConfigException(ScmConfError.UNSUPPORTED_OPTION,
                "unsupport to query workspace info");
    }

    @Override
    public List<Config> getConf(ConfigFilter filter) throws ScmConfigException {
        throw new ScmConfigException(ScmConfError.UNSUPPORTED_OPTION,
                "unsupport to query workspace info");
    }

}