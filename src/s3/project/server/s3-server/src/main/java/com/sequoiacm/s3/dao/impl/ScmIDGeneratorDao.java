package com.sequoiacm.s3.dao.impl;

import com.sequoiacm.contentserver.service.MetaSourceService;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.metasource.MetaSourceDefine;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.s3.common.S3CommonDefine;
import com.sequoiacm.s3.core.IDGenerator;
import com.sequoiacm.s3.dao.IDGeneratorDao;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiadb.base.DBCollection;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Arrays;

@Repository("IDGeneratorDao")
public class ScmIDGeneratorDao implements IDGeneratorDao {
    private static final Logger logger = LoggerFactory.getLogger(ScmIDGeneratorDao.class);

    @Autowired
    MetaSourceService metaSourceService;

    @Override
    public Long getNewId(String type) throws S3ServerException {
        try {
            return queryAndUpdateId(type);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.METASOUCE_ERROR,
                    "query and update id failed. type:" + type, e);
        }
    }

    private long queryAndUpdateId(String type) throws ScmMetasourceException, ScmServerException {
        MetaSource ms = metaSourceService.getMetaSource();
        String tableName = S3CommonDefine.ID_GENERATOR_TABLE_NAME;
        MetaAccessor cl = ms.createMetaAccessor(tableName);

        BSONObject matcher = new BasicBSONObject();
        matcher.put(IDGenerator.ID_TYPE, type);

        BSONObject update = new BasicBSONObject();
        update.put(IDGenerator.ID_ID, 1);
        BSONObject updateId = new BasicBSONObject();
        updateId.put(SequoiadbHelper.SEQUOIADB_MODIFIER_INC, update);

        BSONObject hint = new BasicBSONObject();
        hint.put("", "");
        BSONObject record = cl.queryAndUpdate(matcher, updateId, hint);
        return (long) record.get(IDGenerator.ID_ID);
    }

    @Override
    public void initIdGeneratorTable() throws S3ServerException {
        createIDGenerator();
        initId(S3CommonDefine.IdType.TYPE_UPLOAD);
    }

    private void initId(String type) throws S3ServerException {
        try {
            MetaSource ms = metaSourceService.getMetaSource();
            String tableName = S3CommonDefine.ID_GENERATOR_TABLE_NAME;
            MetaAccessor cl = ms.createMetaAccessor(tableName);

            BSONObject insert = new BasicBSONObject();
            insert.put(IDGenerator.ID_TYPE, type);
            insert.put(IDGenerator.ID_ID, 0L);
            cl.insert(insert, DBCollection.FLG_INSERT_CONTONDUP);
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.METASOUCE_ERROR, "init id failed. id type:" + type,
                    e);
        }
    }

    private void createIDGenerator() throws S3ServerException {
        try {
            MetaAccessor idAccessor = metaSourceService.getMetaSource()
                    .createMetaAccessor(S3CommonDefine.ID_GENERATOR_TABLE_NAME);

            idAccessor.ensureTable(null, Arrays.asList(IDGenerator.ID_TYPE));
        }
        catch (Exception e) {
            throw new S3ServerException(S3Error.METASOUCE_ERROR,
                    "create " + S3CommonDefine.ID_GENERATOR_TABLE_NAME + " table failed.", e);
        }
    }
}
