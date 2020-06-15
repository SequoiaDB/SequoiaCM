package com.sequoiadb.infrastructure.map.server.service;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.metasource.MetaCursor;
import com.sequoiadb.infrastructure.map.ScmMapServerException;

public interface IMapService {
    public BSONObject createMap(String mapGroupName, String mapName, String keyType,
            String valueType) throws ScmMapServerException;

    public BSONObject getMap(String mapGroupName, String mapName) throws ScmMapServerException;

    public void deleteMap(String mapGroupName, String mapName) throws ScmMapServerException;

    public BSONObject put(String mapGroupName, String mapName, BSONObject entry)
            throws ScmMapServerException;

    public void putAll(String mapGroupName, String mapName, List<BSONObject> entryList)
            throws ScmMapServerException;

    public long count(String mapGroupName, String mapName, BSONObject filter)
            throws ScmMapServerException;

    public BSONObject get(String mapGroupName, String mapName, BSONObject key)
            throws ScmMapServerException;

    public MetaCursor list(String mapGroupName, String mapName, BSONObject condition,
            BSONObject selector, BSONObject orderby, long skip, long limit)
            throws ScmMapServerException;

    public BSONObject remove(String mapGroupName, String mapName, BSONObject key)
            throws ScmMapServerException;

    public boolean removeAll(String mapGroupName, String mapName, BSONObject filter)
            throws ScmMapServerException;;

}
