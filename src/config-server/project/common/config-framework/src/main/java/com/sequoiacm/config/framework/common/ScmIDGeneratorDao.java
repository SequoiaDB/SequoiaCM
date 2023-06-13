package com.sequoiacm.config.framework.common;

import java.util.Collections;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbTableDao;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

import javax.annotation.PostConstruct;

@Repository("IDGeneratorDao")
public class ScmIDGeneratorDao implements IDGeneratorDao {

    @Autowired
    SequoiadbMetasource sdbMetaSource;

    @Override
    public int getNewId(String type, InitIdCallback callback) throws ScmConfigException {
        try {
            return queryAndUpdateId(type, 1, callback);
        }
        catch (MetasourceException e) {
            throw new ScmConfigException(e.getError(), "failed to gen id: type:" + type, e);
        }
    }

    private int queryAndUpdateId(String type, int inc, InitIdCallback callback)
            throws MetasourceException {
        SequoiadbTableDao cl = sdbMetaSource.getCollection(
                CommonDefine.IdGenerator.ID_GENERATOR_TABLE_CS,
                CommonDefine.IdGenerator.ID_GENERATOR_TABLE_CL);

        BSONObject matcher = new BasicBSONObject();
        matcher.put(CommonDefine.IdGenerator.ID_TYPE, type);

        BSONObject update = new BasicBSONObject();
        update.put(CommonDefine.IdGenerator.ID_ID, inc);
        BSONObject updateId = new BasicBSONObject();
        updateId.put(SequoiadbHelper.DOLLAR_INC, update);

        BSONObject hint = new BasicBSONObject();
        hint.put("", "");
        BSONObject record = cl.queryAndUpdate(matcher, updateId, hint, true);
        if (record == null) {
            int newId = inc;
            if (callback != null) {
                newId = callback.getInitId();
            }
            if (initId(cl, type, newId)) {
                return newId;
            }
            record = cl.queryAndUpdate(matcher, updateId, hint, true);
        }
        return BsonUtils.getNumberChecked(record, CommonDefine.IdGenerator.ID_ID).intValue();
    }

    private boolean initId(SequoiadbTableDao cl, String type, long id) throws MetasourceException {
        BSONObject insert = new BasicBSONObject();
        insert.put(CommonDefine.IdGenerator.ID_TYPE, type);
        insert.put(CommonDefine.IdGenerator.ID_ID, id);
        try {
            cl.insert(insert);
            return true;
        }
        catch (MetasourceException e) {
            if (e.getError() == ScmConfError.METASOURCE_RECORD_EXIST) {
                return false;
            }
            throw e;
        }
    }

    @PostConstruct
    public void ensureTable() throws MetasourceException {
        SequoiadbTableDao cl = sdbMetaSource.getCollection(
                CommonDefine.IdGenerator.ID_GENERATOR_TABLE_CS,
                CommonDefine.IdGenerator.ID_GENERATOR_TABLE_CL);
        cl.ensureTable(null, Collections.singletonList(CommonDefine.IdGenerator.ID_TYPE));
    }
}
