package com.sequoiadb.infrastructure.map.server.dao;

import org.bson.BSONObject;

import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.server.model.MapMeta;

public interface IMapTableDao {
    public BSONObject createMap(MapMeta mapMeta) throws ScmMapServerException;

    public BSONObject getMap(String groupNmae, String mapName) throws ScmMapServerException;

    public void deleteMap(String groupNmae, String mapName) throws ScmMapServerException;

}
