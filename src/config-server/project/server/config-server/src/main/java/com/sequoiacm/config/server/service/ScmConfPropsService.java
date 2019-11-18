package com.sequoiacm.config.server.service;

import java.util.List;
import java.util.Map;

import com.sequoiacm.config.server.common.ScmTargetType;
import com.sequoiacm.config.server.module.ScmUpdateConfPropsResultSet;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public interface ScmConfPropsService {
    ScmUpdateConfPropsResultSet updateConfProps(ScmTargetType type, List<String> targets,
            Map<String, String> updateProps, List<String> deleteProps, boolean acceptUnknownProps) throws ScmConfigException;
}