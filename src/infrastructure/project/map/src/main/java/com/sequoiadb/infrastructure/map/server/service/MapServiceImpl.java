package com.sequoiadb.infrastructure.map.server.service;

import java.util.List;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.infrastructure.metasource.MetaCursor;
import com.sequoiadb.infrastructure.map.CommonDefine;
import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.server.cache.MapMetaCache;
import com.sequoiadb.infrastructure.map.server.dao.IMapDao;
import com.sequoiadb.infrastructure.map.server.dao.IMapTableDao;
import com.sequoiadb.infrastructure.map.server.model.MapMeta;

@Service
public class MapServiceImpl implements IMapService {
    private static final Logger logger = LoggerFactory.getLogger(MapServiceImpl.class);
    @Autowired
    private IMapDao mapDao;

    @Autowired
    private IMapTableDao mapTableDao;

    @Autowired
    private MapMetaCache mapMetaCache;

    private MapMeta getMapMetaInDb(String groupName, String mapName) throws ScmMapServerException {
        // if map name not exist ,it throw exception
        BSONObject mapBson = mapTableDao.getMap(groupName, mapName);
        MapMeta mapMeta = new MapMeta(mapBson);
        mapMetaCache.put(mapMeta);
        return mapMeta;
    }

    private BSONObject _removeField(BSONObject mapBson) throws ScmMapServerException {
        mapBson.removeField(CommonDefine.FieldName._ID);
        mapBson.removeField(CommonDefine.FieldName.MAP_CL_NAME);
        return mapBson;
    }

    @Override
    public BSONObject createMap(String groupName, String mapName, String keyType, String valueType)
            throws ScmMapServerException {
        try {
            MapMeta mapMeta = new MapMeta(groupName, mapName, keyType, valueType);
            BSONObject mapBson = mapTableDao.createMap(mapMeta);
            mapMetaCache.put(mapMeta);
            return _removeField(mapBson);
        }
        catch (ScmMapServerException e) {
            throw new ScmMapServerException(e.getError(),
                    "create map failed: mapGroup=" + groupName + ", mapName=" + mapName, e);
        }
    }

    @Override
    public BSONObject getMap(String groupName, String mapName) throws ScmMapServerException {
        try {
            BSONObject mapBson = mapTableDao.getMap(groupName, mapName);
            return _removeField(mapBson);
        }
        catch (ScmMapServerException e) {
            throw new ScmMapServerException(e.getError(),
                    "get map failed: mapGroup=" + groupName + ", mapName=" + mapName, e);
        }
    }

    @Override
    public void deleteMap(String groupName, String mapName) throws ScmMapServerException {
        try {
            mapTableDao.deleteMap(groupName, mapName);
            mapMetaCache.remove(groupName, mapName);
        }
        catch (ScmMapServerException e) {
            throw new ScmMapServerException(e.getError(),
                    "delete map failed: mapGroup=" + groupName + ", mapName=" + mapName, e);
        }
    }

    @Override
    public BSONObject put(String groupName, String mapName, BSONObject entry)
            throws ScmMapServerException {
        try {
            MapMeta mapMeta = mapMetaCache.get(groupName, mapName);
            if (mapMeta == null) {
                mapMeta = getMapMetaInDb(groupName, mapName);
            }
            try {
                return mapDao.put(mapMeta.getClName(), entry);
            }
            catch (ScmMapServerException e) {
                // mapMeta's clSuffix is error in cache
                if (e.getError() == ScmMapError.MAP_META_TABLE_NOT_EXIST) {
                    mapMeta = getMapMetaInDb(groupName, mapName);
                    return mapDao.put(mapMeta.getClName(), entry);
                }
                throw e;
            }
        }
        catch (ScmMapServerException e) {
            throw new ScmMapServerException(e.getError(), "put map entry failed: mapGroup="
                    + groupName + ", mapName=" + mapName + ", entry" + entry, e);
        }
    }

    @Override
    public void putAll(String groupName, String mapName, List<BSONObject> entryList)
            throws ScmMapServerException {
        try {
            MapMeta mapMeta = mapMetaCache.get(groupName, mapName);
            if (mapMeta == null) {
                mapMeta = getMapMetaInDb(groupName, mapName);
            }
            try {
                mapDao.putAll(mapMeta.getClName(), entryList);
            }
            catch (ScmMapServerException e) {
                // mapMeta's clSuffix is error in cache
                if (e.getError() == ScmMapError.MAP_META_TABLE_NOT_EXIST) {
                    mapMeta = getMapMetaInDb(groupName, mapName);
                    mapDao.putAll(mapMeta.getClName(), entryList);
                }
                throw e;
            }
        }
        catch (ScmMapServerException e) {
            throw new ScmMapServerException(e.getError(), "put map entry list failed: mapGroup="
                    + groupName + ", mapName=" + mapName + ", entryList" + entryList, e);
        }
    }

    @Override
    public long count(String groupName, String mapName, BSONObject filter)
            throws ScmMapServerException {
        try {
            MapMeta mapMeta = mapMetaCache.get(groupName, mapName);
            if (mapMeta == null) {
                mapMeta = getMapMetaInDb(groupName, mapName);
            }
            try {
                return mapDao.count(mapMeta.getClName(), filter);
            }
            catch (ScmMapServerException e) {
                // mapMeta's clSuffix is error in cache
                if (e.getError() == ScmMapError.MAP_META_TABLE_NOT_EXIST) {
                    mapMeta = getMapMetaInDb(groupName, mapName);
                    return mapDao.count(mapMeta.getClName(), filter);
                }
                throw e;
            }
        }
        catch (ScmMapServerException e) {
            throw new ScmMapServerException(e.getError(), "count map failed: mapGroup=" + groupName
                    + ", mapName=" + mapName + ", filter" + filter, e);
        }
    }

    @Override
    public BSONObject get(String groupName, String mapName, BSONObject key)
            throws ScmMapServerException {
        try {
            MapMeta mapMeta = mapMetaCache.get(groupName, mapName);
            if (mapMeta == null) {
                mapMeta = getMapMetaInDb(groupName, mapName);
            }
            try {
                return mapDao.get(mapMeta.getClName(), key);
            }
            catch (ScmMapServerException e) {
                // mapMeta's clSuffix is error in cache
                if (e.getError() == ScmMapError.MAP_META_TABLE_NOT_EXIST) {
                    mapMeta = getMapMetaInDb(groupName, mapName);
                    return mapDao.get(mapMeta.getClName(), key);
                }
                throw e;
            }
        }
        catch (ScmMapServerException e) {
            throw new ScmMapServerException(e.getError(), "get map value failed by key: mapGroup="
                    + groupName + ", mapName=" + mapName + ", key" + key, e);
        }
    }

    @Override
    public MetaCursor list(String groupName, String mapName, BSONObject condition,
            BSONObject selector, BSONObject orderby, long skip, long limit)
            throws ScmMapServerException {
        try {
            MapMeta mapMeta = mapMetaCache.get(groupName, mapName);
            if (mapMeta == null) {
                mapMeta = getMapMetaInDb(groupName, mapName);
            }
            try {
                return mapDao.list(mapMeta.getClName(), condition, selector, orderby, skip, limit);
            }
            catch (ScmMapServerException e) {
                // mapMeta's clSuffix is error in cache
                if (e.getError() == ScmMapError.MAP_META_TABLE_NOT_EXIST) {
                    mapMeta = getMapMetaInDb(groupName, mapName);
                    return mapDao.list(mapMeta.getClName(), condition, selector, orderby, skip,
                            limit);
                }
                throw e;
            }
        }
        catch (ScmMapServerException e) {
            throw new ScmMapServerException(e.getError(), "list map entry failed: mapGroup="
                    + groupName + ", mapName=" + mapName + ", condition" + condition, e);
        }
    }

    @Override
    public BSONObject remove(String groupName, String mapName, BSONObject key)
            throws ScmMapServerException {
        try {
            MapMeta mapMeta = mapMetaCache.get(groupName, mapName);
            if (mapMeta == null) {
                mapMeta = getMapMetaInDb(groupName, mapName);
            }
            try {
                return mapDao.remove(mapMeta.getClName(), key);
            }
            catch (ScmMapServerException e) {
                // mapMeta's clSuffix is error in cache
                if (e.getError() == ScmMapError.MAP_META_TABLE_NOT_EXIST) {
                    mapMeta = getMapMetaInDb(groupName, mapName);
                    return mapDao.remove(mapMeta.getClName(), key);
                }
                throw e;
            }
        }
        catch (ScmMapServerException e) {
            throw new ScmMapServerException(e.getError(),
                    "remove map entry failed by key: mapGroup=" + groupName + ", mapName=" + mapName
                            + ", key" + key,
                    e);
        }
    }

    @Override
    public boolean removeAll(String groupName, String mapName, BSONObject filter)
            throws ScmMapServerException {
        try {
            MapMeta mapMeta = mapMetaCache.get(groupName, mapName);
            if (mapMeta == null) {
                mapMeta = getMapMetaInDb(groupName, mapName);
            }
            try {
                return mapDao.removeAll(mapMeta.getClName(), filter);
            }
            catch (ScmMapServerException e) {
                // mapMeta's clSuffix is error in cache
                if (e.getError() == ScmMapError.MAP_META_TABLE_NOT_EXIST) {
                    mapMeta = getMapMetaInDb(groupName, mapName);
                    return mapDao.removeAll(mapName, filter);
                }
                throw e;
            }
        }
        catch (ScmMapServerException e) {
            throw new ScmMapServerException(e.getError(), "remove map entry list failed: mapGroup="
                    + groupName + ", mapName=" + mapName + ", filter" + filter, e);
        }
    }

}