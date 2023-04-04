package com.sequoiacm.config.framework.user.operator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.config.framework.user.dao.CreateUserDao;
import com.sequoiacm.config.framework.user.dao.DeleteUserDao;
import com.sequoiacm.config.framework.user.dao.GetUserDao;
import com.sequoiacm.config.framework.user.dao.UpdateUserDao;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.user.UserConfig;
import com.sequoiacm.infrastructure.config.core.msg.user.UserFilter;
import com.sequoiacm.infrastructure.config.core.msg.user.UserUpdator;

@Component
public class ScmUserConfOperator implements ScmConfOperator {

    @Autowired
    private CreateUserDao userCreator;

    @Autowired
    private UpdateUserDao userUpdator;

    @Autowired
    private DeleteUserDao userDeleter;

    @Autowired
    private GetUserDao userFinder;

    @Override
    public List<Config> getConf(ConfigFilter filter) throws ScmConfigException {
        throw new ScmConfigException(ScmConfError.UNSUPPORTED_OPTION,
                "unsupport to query user info");
    }

    @Override
    public List<Version> getConfVersion(VersionFilter filter) throws ScmConfigException {
        return userFinder.getVersions((DefaultVersionFilter) filter);
    }

    @Override
    public ScmConfOperateResult updateConf(ConfigUpdator updator) {
        return userUpdator.update((UserUpdator) updator);
    }

    @Override
    public ScmConfOperateResult deleteConf(ConfigFilter filter) {
        return userDeleter.delete((UserFilter) filter);
    }

    @Override
    public ScmConfOperateResult createConf(Config config) {
        return userCreator.create((UserConfig) config);
    }
}