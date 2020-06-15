package com.sequoiadb.infrastructure.map.server.dao;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.infrastructure.metasource.MetaCursor;
import com.sequoiadb.infrastructure.map.ScmMapServerException;

public interface IMapDao {
    BSONObject put(String mapClName, BSONObject entry) throws ScmMapServerException;

    public void putAll(String mapClName, List<BSONObject> entryList) throws ScmMapServerException;

    public long count(String mapClName, BSONObject filter) throws ScmMapServerException;

    public BSONObject get(String mapClName, BSONObject key) throws ScmMapServerException;

    public MetaCursor list(String mapClName, BSONObject condition, BSONObject selector,
            BSONObject orderby, long skip, long limit) throws ScmMapServerException;

    public BSONObject remove(String mapClName, BSONObject key) throws ScmMapServerException;

    public boolean removeAll(String mapClName, BSONObject filter) throws ScmMapServerException;

}
