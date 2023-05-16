package com.sequoiacm.config.server.service;

import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class ScmGlobalConfigServiceImpl implements ScmGlobalConfigService {
    @Autowired
    private Metasource metasource;

    public void setGlobalConfig(String configName, String configValue) throws MetasourceException {
        metasource.getScmGlobalConfigTableDao().setGlobalConfig(configName, configValue);
    }

    public Map<String, String> getGlobalConfig(String configName) throws MetasourceException {
        if (configName == null) {
            return metasource.getScmGlobalConfigTableDao().getAllGlobalConfig();
        }
        String value = metasource.getScmGlobalConfigTableDao().getGlobalConfig(configName);
        return Collections.singletonMap(configName, value);
    }
}
