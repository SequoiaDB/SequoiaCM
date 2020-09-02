package com.sequoiadb.infrastructure.map.client.model;

import java.util.Map;

import com.sequoiadb.infrastructure.map.ScmMapServerException;

public interface ScmMapGroup {
    public <K, V> Map<K, V> createMap(String mapName, Class<?> keyClass, Class<?> valueClass)
            throws ScmMapServerException;

    public <K, V> Map<K, V> getMap(String mapName) throws ScmMapServerException;

    public void deleteMap(String mapName) throws ScmMapServerException;
}
