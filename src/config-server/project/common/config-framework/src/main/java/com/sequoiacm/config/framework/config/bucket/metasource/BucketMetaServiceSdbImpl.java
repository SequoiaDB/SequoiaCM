package com.sequoiacm.config.framework.config.bucket.metasource;

import javax.annotation.PostConstruct;

import com.sequoiacm.config.framework.config.workspace.metasource.WorkspaceMetaServiceSdbImpl;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.IndexName;
import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbTableDao;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.TableCreatedResult;
import com.sequoiacm.infrastructure.common.TableMetaCommon;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

@Repository
public class BucketMetaServiceSdbImpl implements BucketMetaService {
    private static final Logger logger = LoggerFactory.getLogger(BucketMetaServiceSdbImpl.class);
    @Autowired
    private SequoiadbMetasource sdbMetaSource;

    @Autowired
    private WorkspaceMetaServiceSdbImpl workspaceMetaService;

    private final String BUCKET_ID_TYPE = "scm_bucket";
    private final String ID_TABLE_FIELD_TYPE = "type";
    private final String ID_TABLE_FIELD_ID = "id";

    @PostConstruct
    public void ensureTable() throws MetasourceException {
        BasicBSONObject clOption = new BasicBSONObject();
        sdbMetaSource.ensureCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_BUCKET, clOption);
        sdbMetaSource.ensureIndex(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_BUCKET, "bucket_name_idx",
                new BasicBSONObject(FieldName.Bucket.NAME, 1), true);
        sdbMetaSource.ensureIndex(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_BUCKET, "bucket_id_idx",
                new BasicBSONObject(FieldName.Bucket.ID, 1), false);

        sdbMetaSource.ensureCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_ID_GEN, clOption);
        sdbMetaSource.ensureIndex(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_ID_GEN, "idx_type",
                new BasicBSONObject(ID_TABLE_FIELD_TYPE, 1), true);
        TableDao bucketIdTable = sdbMetaSource.getCollection(
                MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_ID_GEN);
        try {
            bucketIdTable.insert(new BasicBSONObject().append(ID_TABLE_FIELD_TYPE, BUCKET_ID_TYPE)
                    .append(ID_TABLE_FIELD_ID, 0));
        }
        catch (MetasourceException e) {
            if (e.getError() != ScmConfError.METASOURCE_RECORD_EXIST) {
                throw e;
            }
        }
    }

    @Override
    public TableCreatedResult createBucketFileTable(String wsName, long bucketId)
            throws ScmConfigException {
        BSONObject clOption = TableMetaCommon.genBucketTableOption();
        String clName = "BUCKET_FILE_" + bucketId;
        // 创建桶集合，由于桶集合名由bucketId拼接生成，理论上唯一，因此这里如果出现集合存在的异常，不能忽略
        TableCreatedResult opResult = workspaceMetaService.createClInWorkspaceMetaCs(wsName, clName,
                clOption, false);
        String csName = opResult.getCsName();

        try {
            sdbMetaSource.ensureIndex(csName, clName,
                    IndexName.BucketFile.FILE_NAME_UNIQUE_IDX,
                    new BasicBSONObject(FieldName.BucketFile.FILE_NAME, 1), true);
        }
        catch (Exception e) {
            dropBucketFileTableSilence(csName + "." + clName, true);
            throw e;
        }
        return opResult;
    }

    @Override
    public TableDao getBucketTable(Transaction transaction) {
        if (transaction == null) {
            return sdbMetaSource.getCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                    MetaSourceDefine.SequoiadbTableName.CL_BUCKET);
        }
        return sdbMetaSource.getCollection(transaction,
                MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_BUCKET);
    }

    @Override
    public long genBucketId() throws MetasourceException {
        SequoiadbTableDao bucketIdTable = sdbMetaSource.getCollection(
                MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_ID_GEN);

        BSONObject updater = new BasicBSONObject();
        BSONObject incId = new BasicBSONObject(ID_TABLE_FIELD_ID, 1);
        updater.put(SequoiadbHelper.DOLLAR_INC, incId);
        BSONObject newRecord = bucketIdTable.updateAndReturnNew(
                new BasicBSONObject(ID_TABLE_FIELD_TYPE, BUCKET_ID_TYPE), updater);
        if (newRecord == null) {
            throw new MetasourceException(ScmConfError.SYSTEM_ERROR,
                    "id record is not exist: table:"
                            + MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM + "."
                            + MetaSourceDefine.SequoiadbTableName.CL_ID_GEN);
        }
        return BsonUtils.getNumberChecked(newRecord, ID_TABLE_FIELD_ID).longValue();
    }

    @Override
    public void dropBucketFileTableSilence(String bucketFileTable, boolean skipRecycleBin) {
        try {
            String[] csClArr = bucketFileTable.split("\\.");
            sdbMetaSource.dropCollection(csClArr[0], csClArr[1], skipRecycleBin);
        }
        catch (Exception e) {
            logger.warn("failed to drop bucket file table: name={}", bucketFileTable, e);
        }
    }
}
