package com.sequoiadb.infrastructure.map.server.dao;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.stereotype.Repository;

import com.sequoiacm.infrastructure.metasource.template.SequoiadbTemplate;
import com.sequoiacm.infrastructure.metasource.template.SequoiadbTemplate.SequoiadbCollectionTemplate;
import com.sequoiacm.infrastructure.metasource.template.SequoiadbTransaction;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import com.sequoiadb.infrastructure.map.CommonDefine;
import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.ScmMetasourceException;
import com.sequoiadb.infrastructure.map.ScmSystemException;
import com.sequoiadb.infrastructure.map.server.model.MapMeta;

@Repository
public class MapTableDaoImpl implements IMapTableDao {
    private static final String CS_SCMSYSTEM = "SCMSYSTEM";
    private static final String CL_MAP = "MAP";

    private SequoiadbTemplate template;

    public MapTableDaoImpl() {
        this.template = new SequoiadbTemplate();
    }

    private void createMapMetaTable() throws ScmMapServerException {
        try {
            SequoiadbCollectionTemplate clTemplate = template.collectionSpace(CS_SCMSYSTEM)
                    .createCollection(CL_MAP);
            try {
                BasicBSONObject mapIdx = new BasicBSONObject(CommonDefine.FieldName.MAP_NAME, 1);
                mapIdx.put(CommonDefine.FieldName.MAP_GROUP_NAME, 1);
                clTemplate.ensureIndex("idx_group_name", mapIdx, true);
            }
            catch (Exception e) {
                template.collectionSpace(CS_SCMSYSTEM).dropCollection(CL_MAP, true);
                throw e;
            }
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_EXIST.getErrorCode()) {
                throw new ScmMapServerException(ScmMapError.MAP_META_TABLE_ALREADY_EXIST,
                        "map meta table already exist, create table failed:table=" + CS_SCMSYSTEM
                                + "." + CL_MAP,
                        e);
            }
            throw new ScmMetasourceException(
                    "create map meta table failed:table=" + CS_SCMSYSTEM + "." + CL_MAP, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "create map meta table failed:table=" + CS_SCMSYSTEM + "." + CL_MAP, e);
        }
    }

    @Override
    public BSONObject createMap(MapMeta mapMeta) throws ScmMapServerException {
        try {
            BSONObject matcher = new BasicBSONObject(CommonDefine.FieldName.MAP_NAME,
                    mapMeta.getName());
            matcher.put(CommonDefine.FieldName.MAP_GROUP_NAME, mapMeta.getGroupName());
            SequoiadbCollectionTemplate mapMetaCl = template.collection(CS_SCMSYSTEM, CL_MAP);
            BSONObject mapBson = mapMetaCl.findOne(matcher);
            if (mapBson != null) {
                throw new ScmMapServerException(ScmMapError.MAP_TABLE_ALREADY_EXIST,
                        "map name already exist, create map table failed:table=" + CS_SCMSYSTEM
                                + "." + CL_MAP + ", mapGroup=" + mapMeta.getGroupName()
                                + ", mapName=" + mapMeta.getName());
            }

            // create map table
            SequoiadbCollectionTemplate mapTemplate = template.collectionSpace(CS_SCMSYSTEM)
                    .createCollection(mapMeta.getClName());
            try {
                BasicBSONObject mapKeyIdx = new BasicBSONObject(CommonDefine.FieldName.KEY, 1);
                mapTemplate.ensureIndex("idx_key", mapKeyIdx, true);
                // create map meta
                BSONObject insertBson = mapMeta.toBson();
                template.collection(CS_SCMSYSTEM, CL_MAP).insert(insertBson);
                return insertBson;
            }
            catch (Exception e) {
                template.collectionSpace(CS_SCMSYSTEM).dropCollection(mapMeta.getClName(), true);
                throw e;
            }

        }
        catch (BaseException e) {
            // MAP collection not exist
            if (e.getErrorCode() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                createMapMetaTable();
                return createMap(mapMeta);
            }

            // map name collection or map meta data exist
            if (e.getErrorCode() == SDBError.SDB_DMS_EXIST.getErrorCode()
                    || e.getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                throw new ScmMapServerException(ScmMapError.MAP_TABLE_ALREADY_EXIST,
                        "map name already exist, create map table failed:table=" + CS_SCMSYSTEM
                                + "." + CL_MAP + ", mapGroup=" + mapMeta.getGroupName()
                                + ", mapName=" + mapMeta.getName(),
                        e);
            }
            throw new ScmMetasourceException(
                    "create map table failed:table=" + CS_SCMSYSTEM + "." + CL_MAP + ", mapGroup="
                            + mapMeta.getGroupName() + ", mapName=" + mapMeta.getName(),
                    e);
        }
        catch (ScmMapServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "create map table failed:table=" + CS_SCMSYSTEM + "." + CL_MAP + ", mapGroup="
                            + mapMeta.getGroupName() + ", mapName=" + mapMeta.getName(),
                    e);
        }

    }

    @Override
    public BSONObject getMap(String groupName, String mapName) throws ScmMapServerException {
        try {
            BSONObject matcher = new BasicBSONObject(CommonDefine.FieldName.MAP_NAME, mapName);
            matcher.put(CommonDefine.FieldName.MAP_GROUP_NAME, groupName);
            BSONObject mapBson = template.collection(CS_SCMSYSTEM, CL_MAP).findOne(matcher);
            if (mapBson == null) {
                throw new ScmMapServerException(ScmMapError.MAP_TABLE_NOT_EXIST,
                        "map name not exist, get map meta data failed:table=" + CS_SCMSYSTEM + "."
                                + CL_MAP + ", mapGroup=" + groupName + ", mapName=" + mapName);
            }
            return mapBson;
        }
        catch (BaseException e) {
            // MAP collection not exist
            if (e.getErrorCode() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                createMapMetaTable();
                throw new ScmMapServerException(ScmMapError.MAP_TABLE_NOT_EXIST,
                        "map name not exist, get map meta data failed:table=" + CS_SCMSYSTEM + "."
                                + CL_MAP + ", mapGroup=" + groupName + ", mapName=" + mapName);
            }
            throw new ScmMetasourceException("get map meta data failed:table=" + CS_SCMSYSTEM + "."
                    + CL_MAP + ", mapGroup=" + groupName + ", mapName=" + mapName, e);
        }
        catch (ScmMapServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("get map meta data failed:table=" + CS_SCMSYSTEM + "."
                    + CL_MAP + ", mapGroup=" + groupName + ", mapName=" + mapName, e);
        }
    }

    @Override
    public void deleteMap(String groupName, String mapName) throws ScmMapServerException {
        SequoiadbTransaction transaction = new SequoiadbTransaction();
        try {
            transaction.begin();
            BSONObject matcher = new BasicBSONObject(CommonDefine.FieldName.MAP_NAME, mapName);
            BSONObject mapBson = null;
            try {
                mapBson = template.collection(CS_SCMSYSTEM, CL_MAP).queryAndDelete(matcher, null,
                        transaction);
            }
            catch (BaseException e) {
                // MAP collection not exist
                if (e.getErrorCode() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                    createMapMetaTable();
                }
            }
            if (mapBson == null) {
                throw new ScmMapServerException(ScmMapError.MAP_TABLE_NOT_EXIST,
                        "map name not exist, delete map table failed:table=" + CS_SCMSYSTEM + "."
                                + CL_MAP + ", mapGroup=" + groupName + ", mapName=" + mapName);
            }
            String clName = (String) mapBson.get(CommonDefine.FieldName.MAP_CL_NAME);
            template.collectionSpace(CS_SCMSYSTEM).dropCollection(clName);
            transaction.commit();
        }
        catch (BaseException e) {
            transaction.rollback();
            throw new ScmMetasourceException("delete map table failed:table=" + CS_SCMSYSTEM + "."
                    + CL_MAP + ", mapGroup=" + groupName + ", mapName=" + mapName, e);

        }
        catch (ScmMapServerException e) {
            transaction.rollback();
            throw e;
        }
        catch (Exception e) {
            transaction.rollback();
            throw new ScmSystemException("delete map table failed:table=" + CS_SCMSYSTEM + "."
                    + CL_MAP + ", mapGroup=" + groupName + ", mapName=" + mapName, e);
        }
    }
}
