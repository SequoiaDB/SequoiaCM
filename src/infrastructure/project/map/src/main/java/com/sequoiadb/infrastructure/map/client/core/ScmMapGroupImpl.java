package com.sequoiadb.infrastructure.map.client.core;

import java.util.Map;

import org.bson.BSONObject;

import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.client.model.ScmMapGroup;
import com.sequoiadb.infrastructure.map.client.service.IMapClientService;
import com.sequoiadb.infrastructure.map.client.service.MapClientServiceFactory;
import com.sequoiadb.infrastructure.map.client.service.MapFeignClient;

class ScmMapGroupImpl implements ScmMapGroup {
    private String groupName;
    private IMapClientService mapClientService;

    public String getGroupName() {
        return groupName;
    }

    public ScmMapGroupImpl(MapFeignClient client, String groupName) {
        this.groupName = groupName;
        this.mapClientService = MapClientServiceFactory.getInstance().createClientService(client,
                groupName);
    }

    public <K, V> Map<K, V> createMap(String mapName, Class<?> keyClass, Class<?> valueClass)
            throws ScmMapServerException {
        if (BSONObject.class.isAssignableFrom(keyClass)) {
            throw new ScmMapServerException(ScmMapError.INVALID_ARGUMENT,
                    "key class invaild: keyClass=" + keyClass);
        }
        BSONObject mapBson = mapClientService.createMap(mapName, keyClass, valueClass);
        return new ScmMapImpl<K, V>(mapClientService, mapBson);
    }

    public <K, V> Map<K, V> getMap(String mapName) throws ScmMapServerException {
        BSONObject mapBson = mapClientService.getMap(mapName);
        return new ScmMapImpl<K, V>(mapClientService, mapBson);
    }

    public void deleteMap(String mapName) throws ScmMapServerException {
        mapClientService.deleteMap(mapName);
    }
}
