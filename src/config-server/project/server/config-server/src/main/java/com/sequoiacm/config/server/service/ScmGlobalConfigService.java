package com.sequoiacm.config.server.service;


import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

import java.util.Map;


public interface ScmGlobalConfigService {
    void setGlobalConfig(String configName, String configValue) throws ScmConfigException;

    Map<String, String> getGlobalConfig(String configName) throws ScmConfigException;
}
