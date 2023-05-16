package com.sequoiacm.config.metasource;

import com.sequoiacm.config.metasource.exception.MetasourceException;

import java.util.Map;

public interface ScmGlobalConfigTableDao {

    void setGlobalConfig(String configName, String configValue) throws MetasourceException;

    String getGlobalConfig(String configName) throws MetasourceException;

    Map<String, String> getAllGlobalConfig() throws MetasourceException;
}
