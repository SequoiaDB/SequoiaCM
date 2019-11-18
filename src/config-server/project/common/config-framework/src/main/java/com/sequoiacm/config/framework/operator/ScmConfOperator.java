package com.sequoiacm.config.framework.operator;

import java.util.List;

import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;

public interface ScmConfOperator {

    List<Config> getConf(ConfigFilter filter) throws ScmConfigException;

    List<Version> getConfVersion(VersionFilter filter) throws ScmConfigException;

    ScmConfOperateResult updateConf(ConfigUpdator config) throws ScmConfigException;

    ScmConfOperateResult deleteConf(ConfigFilter config) throws ScmConfigException;

    ScmConfOperateResult createConf(Config config) throws ScmConfigException;
}