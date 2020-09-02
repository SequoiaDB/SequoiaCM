package com.sequoiadb.infrastructure.map.client.service;

import java.io.InputStream;
import java.util.List;

import org.bson.BSONObject;

import com.alibaba.fastjson.JSONObject;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.client.BsonReader;
import com.sequoiadb.infrastructure.map.client.CloseableHttpResponseInputStream;
import com.sequoiadb.infrastructure.map.client.RestBsonReader;
import com.sequoiadb.infrastructure.map.client.model.ScmEntry;

import feign.Response;

class MapClientServiceImpel implements IMapClientService {
    private MapFeignClient client;
    private String groupName;

    public MapClientServiceImpel(MapFeignClient client, String groupName) {
        this.client = client;
        this.groupName = groupName;
    }

    @Override
    public BSONObject createMap(String mapName, Class<?> keyClass, Class<?> valueClass)
            throws ScmMapServerException {
        return client.createMap(groupName, mapName, keyClass.getTypeName(),
                valueClass.getTypeName());
    }

    @Override
    public BSONObject getMap(String mapName) throws ScmMapServerException {
        return client.getMap(groupName, mapName);
    }

    @Override
    public void deleteMap(String mapName) throws ScmMapServerException {
        client.deleteMap(groupName, mapName);
    }

    @Override
    public BSONObject put(String mapName, ScmEntry<?, ?> entry) throws ScmMapServerException {
        return client.putMapEntry(groupName, mapName, entry.toBson());
    }

    @Override
    public void putAll(String mapName, List<BSONObject> entryList) throws ScmMapServerException {
        String entrys = JSONObject.toJSONString(entryList);
        client.putEntryList(groupName, mapName, entrys);
    }

    @Override
    public long count(String mapName, BSONObject filter) throws ScmMapServerException {
        return client.count(groupName, mapName, filter);
    }

    @Override
    public BSONObject get(String mapName, BSONObject key) throws ScmMapServerException {
        return client.getMapValue(groupName, mapName, key);
    }

    @Override
    public BsonReader listEntry(String mapName, BSONObject condition, BSONObject orderby, long skip,
            long limit) throws ScmMapServerException {
        Response response = client.listMapEntry(groupName, mapName, condition, orderby, skip,
                limit);
        InputStream is = new CloseableHttpResponseInputStream(response);
        return new RestBsonReader(is);
    }

    @Override
    public BsonReader listKey(String mapName, BSONObject condition, BSONObject orderby, long skip,
            long limit) throws ScmMapServerException {
        Response response = client.listMapKey(groupName, mapName, condition, orderby, skip, limit);
        InputStream is = new CloseableHttpResponseInputStream(response);
        return new RestBsonReader(is);
    }

    @Override
    public BSONObject remove(String mapName, BSONObject key) throws ScmMapServerException {
        return client.remove(groupName, mapName, key);
    }

    @Override
    public boolean removeAll(String mapName, BSONObject filter) throws ScmMapServerException {
        return client.removeAll(groupName, mapName, filter);
    }

}
