package com.sequoiacm.infrastructure.config.client.service;

import java.util.List;
import java.util.Map;

import com.sequoiacm.infrastructure.config.core.common.ScmServiceUpdateConfigResult;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import org.bson.BSONObject;

public interface ScmConfigPropService {
    public ScmServiceUpdateConfigResult updateConfigProps(Map<String, String> updateProps,
            List<String> deleteProps, boolean acceptUnknownProps) throws ScmConfigException;

    BSONObject getConfigProps(List<String> asList);
}
