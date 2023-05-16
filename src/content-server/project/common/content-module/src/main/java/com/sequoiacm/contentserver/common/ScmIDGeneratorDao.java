package com.sequoiacm.contentserver.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;

import javax.annotation.PostConstruct;

@Repository("IDGeneratorDao")
public class ScmIDGeneratorDao implements IDGeneratorDao {

    private static final Logger logger = LoggerFactory.getLogger(ScmIDGeneratorDao.class);

    @Autowired
    MetaSourceService metaSourceService;

    @Override
    public long getNewId(String type) throws ScmServerException {
        try {
            return queryAndUpdateId(type, 1);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to gen id: type:" + type, e);
        }
    }

    @Override
    public List<Long> getNewIds(String type, int count) throws ScmServerException {
        try {
            long latestId = queryAndUpdateId(type, count);
            List<Long> ret = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                ret.add(latestId - i);
            }
            return ret;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to gen id: type:" + type, e);
        }
    }

    private long queryAndUpdateId(String type, int inc)
            throws ScmMetasourceException, ScmServerException {
        MetaSource ms = metaSourceService.getMetaSource();
        MetaAccessor cl = ms.createMetaAccessor(CommonDefine.IdGenerator.ID_GENERATOR_TABLE_NAME);

        BSONObject matcher = new BasicBSONObject();
        matcher.put(CommonDefine.IdGenerator.ID_TYPE, type);

        BSONObject update = new BasicBSONObject();
        update.put(CommonDefine.IdGenerator.ID_ID, inc);
        BSONObject updateId = new BasicBSONObject();
        updateId.put(SequoiadbHelper.SEQUOIADB_MODIFIER_INC, update);

        BSONObject hint = new BasicBSONObject();
        hint.put("", "");
        BSONObject record = cl.queryAndUpdate(matcher, updateId, hint, true);
        if (record == null) {
            if (initId(cl, type, inc)) {
                return inc;
            }
            record = cl.queryAndUpdate(matcher, updateId, hint);
        }
        return BsonUtils.getNumberChecked(record, CommonDefine.IdGenerator.ID_ID).longValue();
    }

    private boolean initId(MetaAccessor idMetaAccessor, String type, long id)
            throws ScmMetasourceException {
        BSONObject insert = new BasicBSONObject();
        insert.put(CommonDefine.IdGenerator.ID_TYPE, type);
        insert.put(CommonDefine.IdGenerator.ID_ID, id);
        try {
            idMetaAccessor.insert(insert);
            return true;
        }
        catch (ScmMetasourceException e) {
            if (e.getScmError() == ScmError.METASOURCE_RECORD_EXIST) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public void ensureTable() throws ScmServerException, ScmMetasourceException {
        MetaAccessor idAccessor = metaSourceService.getMetaSource()
                .createMetaAccessor(CommonDefine.IdGenerator.ID_GENERATOR_TABLE_NAME);
        idAccessor.ensureTable(null, Collections.singletonList(CommonDefine.IdGenerator.ID_TYPE));
    }
}
