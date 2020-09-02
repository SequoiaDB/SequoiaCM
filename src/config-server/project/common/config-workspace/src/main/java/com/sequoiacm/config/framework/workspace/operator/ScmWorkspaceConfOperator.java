package com.sequoiacm.config.framework.workspace.operator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.common.DefaultVersionDao;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.config.framework.workspace.dao.CreateWorkspaceDao;
import com.sequoiacm.config.framework.workspace.dao.DeleteWorkspaceDao;
import com.sequoiacm.config.framework.workspace.dao.GetWorkspaceDao;
import com.sequoiacm.config.framework.workspace.dao.UpdateWorkspaceDao;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceFilter;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdator;

@Component
public class ScmWorkspaceConfOperator implements ScmConfOperator {

    @Autowired
    private CreateWorkspaceDao wsCreater;

    @Autowired
    private DeleteWorkspaceDao wsDeleter;

    @Autowired
    private UpdateWorkspaceDao wsUpdator;

    @Autowired
    private GetWorkspaceDao wsFinder;

    @Autowired
    private DefaultVersionDao versionDao;

    @Override
    public List<Config> getConf(ConfigFilter filter) throws ScmConfigException {
        return wsFinder.getWorkspace((WorkspaceFilter) filter);
    }

    @Override
    public List<Version> getConfVersion(VersionFilter filter) throws ScmConfigException {
        DefaultVersionFilter versionFilter = (DefaultVersionFilter) filter;
        return versionDao.getVerions(versionFilter);
    }

    @Override
    public ScmConfOperateResult updateConf(ConfigUpdator updator) throws ScmConfigException {
        return wsUpdator.update((WorkspaceUpdator) updator);
    }

    @Override
    public ScmConfOperateResult deleteConf(ConfigFilter filter) throws ScmConfigException {
        return wsDeleter.delete((WorkspaceFilter) filter);
    }

    @Override
    public ScmConfOperateResult createConf(Config config) throws ScmConfigException {
        return wsCreater.create((WorkspaceConfig) config);
    }

}