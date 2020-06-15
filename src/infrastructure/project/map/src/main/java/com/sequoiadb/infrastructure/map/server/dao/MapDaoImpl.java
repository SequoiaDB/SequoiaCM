package com.sequoiadb.infrastructure.map.server.dao;

import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.stereotype.Repository;

import com.sequoiacm.infrastructure.metasource.MetaCursor;
import com.sequoiacm.infrastructure.metasource.template.ITransaction;
import com.sequoiacm.infrastructure.metasource.template.SequoiadbTemplate;
import com.sequoiacm.infrastructure.metasource.template.SequoiadbTransaction;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import com.sequoiadb.infrastructure.map.CommonDefine;
import com.sequoiadb.infrastructure.map.ScmMapError;
import com.sequoiadb.infrastructure.map.ScmMapServerException;
import com.sequoiadb.infrastructure.map.ScmMetasourceException;
import com.sequoiadb.infrastructure.map.ScmSystemException;

@Repository
public class MapDaoImpl implements IMapDao {
    private static final String CS_SCMSYSTEM = "SCMSYSTEM";
    private SequoiadbTemplate template;

    public MapDaoImpl() {
        this.template = new SequoiadbTemplate();
    }

    // return old value
    @Override
    public BSONObject put(String clName, BSONObject entry) throws ScmMapServerException {
        return upsert(clName, entry, null);
    }

    public BSONObject upsert(String clName, BSONObject entry, SequoiadbTransaction context)
            throws ScmMapServerException {
        try {
            template.collection(CS_SCMSYSTEM, clName).insert(entry, context);
            return null;
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                throw new ScmMapServerException(ScmMapError.MAP_TABLE_NOT_EXIST,
                        "map name not exist, insert map entry failed:table=" + CS_SCMSYSTEM + "."
                                + clName + " mapEntry=" + entry,
                        e);
            }
            if (e.getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                return _update(clName, entry, context);
            }
            throw new ScmMetasourceException(
                    "insert map entry failed:table=" + CS_SCMSYSTEM + "." + clName, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "insert map entry failed:table=" + CS_SCMSYSTEM + "." + clName, e);
        }

    }

    private BSONObject _update(String clName, BSONObject entry, ITransaction context)
            throws ScmMapServerException {
        try {
            Object key = entry.get(CommonDefine.FieldName.KEY);
            BSONObject matcher = new BasicBSONObject(CommonDefine.FieldName.KEY, key);
            BSONObject selector = new BasicBSONObject(CommonDefine.FieldName.VALUE, null);
            BSONObject midifier = new BasicBSONObject(CommonDefine.Updater.SET, entry);
            return template.collection(CS_SCMSYSTEM, clName).queryAndUpdate(matcher, selector,
                    midifier, false);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                throw new ScmMapServerException(ScmMapError.MAP_TABLE_NOT_EXIST,
                        "map name not exist, update map entry failed:table=" + CS_SCMSYSTEM + "."
                                + clName + " mapEntry=" + entry,
                        e);
            }
            throw new ScmMetasourceException(
                    "update map entry failed:table=" + CS_SCMSYSTEM + "." + clName, e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "update map entry failed:table=" + CS_SCMSYSTEM + "." + clName, e);
        }
    }

    @Override
    public void putAll(String clName, List<BSONObject> entryList) throws ScmMapServerException {
        try {
            for (BSONObject entry : entryList) {
                upsert(clName, entry, null);
            }
        }
        catch (BaseException e) {
            throw new ScmMetasourceException(
                    "upsert map entry list failed:table=" + CS_SCMSYSTEM + "." + clName, e);
        }
        catch (ScmMapServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "update map entry failed:table=" + CS_SCMSYSTEM + "." + clName, e);
        }
    }

    // if key not exist，return null
    @Override
    public BSONObject get(String clName, BSONObject key) throws ScmMapServerException {
        try {
            BSONObject selector = new BasicBSONObject(CommonDefine.FieldName.VALUE, null);
            BSONObject mapBson = template.collection(CS_SCMSYSTEM, clName).findOne(key, selector);
            return mapBson;
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                throw new ScmMapServerException(ScmMapError.MAP_TABLE_NOT_EXIST,
                        "map name not exist, get map value failed:table=" + CS_SCMSYSTEM + "."
                                + clName + ", key = " + key,
                        e);
            }
            throw new ScmMetasourceException(
                    "get map value failed:table=" + CS_SCMSYSTEM + "." + clName + ", key = " + key,
                    e);
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "get map value failed:table=" + CS_SCMSYSTEM + "." + clName + ", key = " + key,
                    e);
        }
    }

    @Override
    public long count(String clName, BSONObject filter) throws ScmMapServerException {
        try {
            return template.collection(CS_SCMSYSTEM, clName).count(filter);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                throw new ScmMapServerException(ScmMapError.MAP_TABLE_NOT_EXIST,
                        "map name not exist, get map count failed:table=" + CS_SCMSYSTEM + "."
                                + clName + ", filter = " + filter,
                        e);
            }
            throw new ScmMetasourceException("get map count failed:table=" + CS_SCMSYSTEM + "."
                    + clName + ", filter = " + filter, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("get map count failed:table=" + CS_SCMSYSTEM + "." + clName
                    + ", filter = " + filter, e);
        }
    }

    @Override
    public MetaCursor list(String clName, BSONObject condition, BSONObject selector,
            BSONObject orderby, long skip, long limit) throws ScmMapServerException {
        try {
            return template.collection(CS_SCMSYSTEM, clName).find(condition, selector, orderby,
                    skip, limit);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                throw new ScmMapServerException(ScmMapError.MAP_TABLE_NOT_EXIST,
                        "map name not exist, list map failed:table=" + CS_SCMSYSTEM + "." + clName
                                + ", condition = " + condition,
                        e);
            }
            throw new ScmMetasourceException("list map failed:table=" + CS_SCMSYSTEM + "." + clName
                    + ", condition = " + condition, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("list map failed:tabl=" + CS_SCMSYSTEM + "." + clName
                    + ", condition = " + condition, e);
        }
    }

    @Override
    public BSONObject remove(String clName, BSONObject key) throws ScmMapServerException {
        BSONObject selector = new BasicBSONObject(CommonDefine.FieldName.VALUE, null);
        return _remove(clName, key, selector);
    }

    private BSONObject _remove(String clName, BSONObject filter, BSONObject selector)
            throws ScmMapServerException, ScmMetasourceException, ScmSystemException {
        try {
            return template.collection(CS_SCMSYSTEM, clName).queryAndDelete(filter, selector);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                throw new ScmMapServerException(ScmMapError.MAP_TABLE_NOT_EXIST,
                        "map name not exist, delete map entry failed:table=" + CS_SCMSYSTEM + "."
                                + clName + ", key = " + filter,
                        e);
            }
            throw new ScmMetasourceException("delete map entry failed:table=" + CS_SCMSYSTEM + "."
                    + clName + ", key = " + filter, e);
        }
        catch (Exception e) {
            throw new ScmSystemException("delete map entry failed:table=" + CS_SCMSYSTEM + "."
                    + clName + ", key = " + filter, e);
        }
    }

    @Override
    public boolean removeAll(String clName, BSONObject filter)
            throws ScmMetasourceException, ScmSystemException, ScmMapServerException {
        if (_remove(clName, filter, null) != null) {
            return true;
        }
        return false;

    }

}
