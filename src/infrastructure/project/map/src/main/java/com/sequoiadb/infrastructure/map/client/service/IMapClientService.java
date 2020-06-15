package com.sequoiadb.infrastructure.map.client.service;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.client.BsonReader;
import com.sequoiadb.infrastructure.map.client.model.ScmEntry;

public interface IMapClientService {

    BSONObject createMap(String mapName, Class<?> keyClass, Class<?> valueClass)
            throws ScmMapServerException;

    BSONObject getMap(String mapName) throws ScmMapServerException;

    void deleteMap(String mapName) throws ScmMapServerException;

    long count(String mapName, BSONObject filter) throws ScmMapServerException;

    BSONObject put(String mapName, ScmEntry<?, ?> entry) throws ScmMapServerException;

    void putAll(String mapName, List<BSONObject> entryList) throws ScmMapServerException;

    BSONObject get(String mapName, BSONObject key) throws ScmMapServerException;

    BSONObject remove(String mapName, BSONObject key) throws ScmMapServerException;

    boolean removeAll(String mapName, BSONObject filter) throws ScmMapServerException;

    BsonReader listEntry(String mapName, BSONObject condition, BSONObject orderby, long skip,
            long limit) throws ScmMapServerException;

    BsonReader listKey(String mapName, BSONObject condition, BSONObject orderby, long skip,
            long limit) throws ScmMapServerException;

}
