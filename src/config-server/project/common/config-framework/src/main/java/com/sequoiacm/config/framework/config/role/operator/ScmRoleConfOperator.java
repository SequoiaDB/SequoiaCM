package com.sequoiacm.config.framework.config.role.operator;

import java.util.List;

import com.sequoiacm.config.framework.config.role.dao.CreateRoleDao;
import com.sequoiacm.config.framework.config.role.dao.DeleteRoleDao;
import com.sequoiacm.config.framework.config.role.dao.GetRoleDao;
import com.sequoiacm.config.framework.config.role.dao.UpdateRoleDao;
import com.sequoiacm.infrastructure.config.core.common.BusinessType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdater;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.role.RoleConfig;
import com.sequoiacm.infrastructure.config.core.msg.role.RoleFilter;
import com.sequoiacm.infrastructure.config.core.msg.role.RoleUpdater;

@Component
@BusinessType(ScmBusinessTypeDefine.ROLE)
public class ScmRoleConfOperator implements ScmConfOperator {

    @Autowired
    private CreateRoleDao roleCreator;

    @Autowired
    private UpdateRoleDao roleUpdator;

    @Autowired
    private DeleteRoleDao roleDeleter;

    @Autowired
    private GetRoleDao roleFinder;

    @Override
    public List<Config> getConf(ConfigFilter filter) throws ScmConfigException {
        throw new ScmConfigException(ScmConfError.UNSUPPORTED_OPTION,
                "unsupport to query role info");
    }

    @Override
    public List<Version> getConfVersion(VersionFilter filter) throws ScmConfigException {
        return roleFinder.getVersions(filter);
    }

    @Override
    public ScmConfOperateResult updateConf(ConfigUpdater updator) {
        return roleUpdator.update((RoleUpdater) updator);
    }

    @Override
    public ScmConfOperateResult deleteConf(ConfigFilter filter) {
        return roleDeleter.delete((RoleFilter) filter);
    }

    @Override
    public ScmConfOperateResult createConf(Config config) {
        return roleCreator.create((RoleConfig) config);
    }
}