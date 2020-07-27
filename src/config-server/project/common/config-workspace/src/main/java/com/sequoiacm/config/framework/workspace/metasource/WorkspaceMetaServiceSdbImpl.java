package com.sequoiacm.config.framework.workspace.metasource;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.common.FieldName;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

@Repository
public class WorkspaceMetaServiceSdbImpl implements WorkspaceMetaSerivce {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceMetaServiceSdbImpl.class);

    @Autowired
    private SequoiadbMetasource sdbMetasource;

    @Override
    public SysWorkspaceTableDao getSysWorkspaceTable(Transaction transaction) {
        return new SysWorkspaceTableDaoSdbImpl(transaction);
    }

    @Override
    public void createWorkspaceMetaTable(String wsName, BSONObject metalocation)
            throws MetasourceException {
        String domain = (String) metalocation.get("domain");
        if (domain == null) {
            throw new IllegalArgumentException("metalocation missing domain filed:" + metalocation);
        }

        Sequoiadb sdb = sdbMetasource.getConnection();
        try {
            BSONObject csOption = new BasicBSONObject("Domain", domain);
            BSONObject metaOptions = BsonUtils.getBSON(metalocation,
                    FieldName.FIELD_CLWORKSPACE_META_OPTIONS);
            if (metaOptions != null) {
                BSONObject customCsOptions = BsonUtils.getBSON(metaOptions,
                        FieldName.FIELD_CLWORKSPACE_META_CS_OPTIONS);
                if (customCsOptions != null) {
                    csOption.putAll(customCsOptions);
                }
            }

            logger.info("creating cs:csName=" + wsName
                    + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL + ",options="
                    + csOption.toString());
            CollectionSpace wsCs = sdb.createCollectionSpace(
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL, csOption);

            // DIRECTORY
            logger.info("creating cl:clName={}.{}",
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                    MetaSourceDefine.SequoiadbTableName.CL_DIRECTORY);
            DBCollection dirCl = wsCs
                    .createCollection(MetaSourceDefine.SequoiadbTableName.CL_DIRECTORY);
            BasicBSONObject dirPidNameIdx = new BasicBSONObject();
            dirPidNameIdx.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, 1);
            dirPidNameIdx.put(FieldName.FIELD_CLDIR_NAME, 1);
            dirCl.createIndex("idx_name_pid", dirPidNameIdx, true, true);
            BasicBSONObject dirIdIdx = new BasicBSONObject(FieldName.FIELD_CLDIR_ID, 1);
            logger.info("creating index:clName={},key={},isUnique={},enforced={}",
                    dirCl.getFullName(), dirIdIdx.toString(), true, true);
            dirCl.createIndex("idx_id", dirIdIdx, true, true);

            // FILE_DIRECTORY_REL
            BSONObject relClOptions = new BasicBSONObject();
            relClOptions.put("ShardingType", "hash");
            relClOptions.put("AutoSplit", true);
            BSONObject relClShardingKey = new BasicBSONObject(FieldName.FIELD_CLREL_DIRECTORY_ID,
                    1);
            relClOptions.put("ShardingKey", relClShardingKey);
            logger.info("creating cl:clName={}.{},options={}",
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                    MetaSourceDefine.SequoiadbTableName.CL_FILE_RELATION, relClOptions.toString());
            DBCollection relCl = wsCs.createCollection(
                    MetaSourceDefine.SequoiadbTableName.CL_FILE_RELATION, relClOptions);
            BSONObject relIdx = new BasicBSONObject();
            relIdx.put(FieldName.FIELD_CLREL_DIRECTORY_ID, 1);
            relIdx.put(FieldName.FIELD_CLREL_FILENAME, 1);
            logger.info("creating index:clName={},key={},isUnique={},enforced={}",
                    relCl.getFullName(), relIdx.toString(), true, true);
            relCl.createIndex("idx_name_pid", relIdx, true, true);

            // CLASS
            logger.info("creating cl:clName={}.{},options=null",
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                    MetaSourceDefine.SequoiadbTableName.CL_CLASS);
            DBCollection classCl = wsCs
                    .createCollection(MetaSourceDefine.SequoiadbTableName.CL_CLASS, null);
            BSONObject classNameIdx = new BasicBSONObject();
            classNameIdx.put(FieldName.FIELD_CLCLASS_NAME, 1);
            logger.info("creating index:clName={},key={},isUnique={},enforced={}",
                    classCl.getFullName(), classNameIdx.toString(), true, true);
            classCl.createIndex("idx_class_name", classNameIdx, true, true);
            BSONObject classIdIdx = new BasicBSONObject();
            classIdIdx.put(FieldName.FIELD_CLCLASS_ID, 1);
            logger.info("creating index:clName={},key={},isUnique={},enforced={}",
                    classCl.getFullName(), classIdIdx.toString(), true, true);
            classCl.createIndex("idx_class_id", classIdIdx, true, true);

            // ATTRIBUTE
            logger.info("creating cl:clName={}.{},options=null",
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                    MetaSourceDefine.SequoiadbTableName.CL_ATTRIBUTE);
            DBCollection attrCl = wsCs
                    .createCollection(MetaSourceDefine.SequoiadbTableName.CL_ATTRIBUTE, null);
            BSONObject attrNameIdx = new BasicBSONObject();
            attrNameIdx.put(FieldName.FIELD_CLATTR_NAME, 1);
            logger.info("creating index:clName={},key={},isUnique={},enforced={}",
                    attrCl.getFullName(), attrNameIdx.toString(), true, true);
            attrCl.createIndex("idx_attr_name", attrNameIdx, true, true);
            BSONObject attrIdIdx = new BasicBSONObject();
            attrIdIdx.put(FieldName.FIELD_CLATTR_ID, 1);
            logger.info("creating index:clName={},key={},isUnique={},enforced={}",
                    attrCl.getFullName(), attrIdIdx.toString(), true, true);
            attrCl.createIndex("idx_attr_id", attrIdIdx, true, true);

            // CLASS_ATTR_REL
            logger.info("creating cl:clName={}.{},options=null",
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                    MetaSourceDefine.SequoiadbTableName.CL_CLASS_ATTR_REL);
            DBCollection attrAttrRelCl = wsCs
                    .createCollection(MetaSourceDefine.SequoiadbTableName.CL_CLASS_ATTR_REL, null);
            BSONObject classAttrRelIdx = new BasicBSONObject();
            classAttrRelIdx.put(FieldName.FIELD_CL_CLASS_ATTR_REL_CLASS_ID, 1);
            classAttrRelIdx.put(FieldName.FIELD_CL_CLASS_ATTR_REL_ATTR_ID, 1);
            logger.info("creating index:clName={},key={},isUnique={},enforced={}",
                    attrAttrRelCl.getFullName(), classAttrRelIdx.toString(), true, true);
            attrAttrRelCl.createIndex("idx_rel_id", classAttrRelIdx, true, true);

            // FILE
            BSONObject fileClOptions = new BasicBSONObject();
            fileClOptions.put("ShardingType", "range");
            BSONObject fileClShardingKey = new BasicBSONObject(
                    FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, 1);
            fileClOptions.put("ShardingKey", fileClShardingKey);
            fileClOptions.put("IsMainCL", true);
            logger.info("creating cl:clName={}.{},options={}",
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                    MetaSourceDefine.SequoiadbTableName.CL_FILE, fileClOptions.toString());
            wsCs.createCollection(MetaSourceDefine.SequoiadbTableName.CL_FILE, fileClOptions);

            // FILE_HISTORY
            logger.info("creating cl:clName={}.{},options={}",
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                    MetaSourceDefine.SequoiadbTableName.CL_FILE_HISTORY, fileClOptions.toString());
            DBCollection fileHistoryCl = wsCs.createCollection(
                    MetaSourceDefine.SequoiadbTableName.CL_FILE_HISTORY, fileClOptions);
            BasicBSONObject fileHistorIdx = new BasicBSONObject();
            fileHistorIdx.put(FieldName.FIELD_CLFILE_ID, 1);
            fileHistorIdx.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, 1);
            fileHistorIdx.put(FieldName.FIELD_CLFILE_MINOR_VERSION, 1);
            logger.info("creating index:clName={},key={},isUnique={},enforced={}",
                    fileHistoryCl.getFullName(), fileHistorIdx.toString(), false, false);
            fileHistoryCl.createIndex("idx_file_history", fileHistorIdx, false, false);

            // BREAKPOINT_FILE
            BSONObject breakpointFileOption = new BasicBSONObject();
            breakpointFileOption.put("ShardingType", "hash");
            BasicBSONObject breakpointFileShardingKey = new BasicBSONObject(
                    FieldName.FIELD_CLBREAKPOINTFILE_FILE_NAME, 1);
            breakpointFileOption.put("ShardingKey", breakpointFileShardingKey);
            breakpointFileOption.put("AutoSplit", true);
            breakpointFileOption.put("EnsureShardingIndex", false);
            logger.info("creating cl:clName={}.{},options={}",
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                    MetaSourceDefine.SequoiadbTableName.CL_BREAKPOINT_FILE, breakpointFileOption);
            DBCollection breakpointFileCl = wsCs.createCollection(
                    MetaSourceDefine.SequoiadbTableName.CL_BREAKPOINT_FILE, breakpointFileOption);
            BasicBSONObject breakpointFileIdx = new BasicBSONObject(
                    FieldName.FIELD_CLBREAKPOINTFILE_FILE_NAME, 1);
            logger.info("creating index:clName={},key={},isUnique={},enforced={}",
                    breakpointFileCl.getFullName(), breakpointFileIdx.toString(), true, false);
            breakpointFileCl.createIndex("file_name_index", breakpointFileIdx, true, false);

            // TRANSACTION_LOG
            logger.info("creating cl:clName={}.{}",
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                    MetaSourceDefine.SequoiadbTableName.CL_TRANSACTION_LOG);
            wsCs.createCollection(MetaSourceDefine.SequoiadbTableName.CL_TRANSACTION_LOG);

            // BATCH
            BSONObject key = new BasicBSONObject(FieldName.FIELD_CLBATCH_ID, 1);
            BSONObject batchOptions = new BasicBSONObject();
            batchOptions.put("ShardingType", "hash");
            batchOptions.put("ShardingKey", key);
            batchOptions.put("Compressed", true);
            batchOptions.put("CompressionType", "lzw");
            batchOptions.put("ReplSize", -1);
            batchOptions.put("AutoSplit", true);
            logger.info("creating cl:clName={}.{},options={}",
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                    MetaSourceDefine.SequoiadbTableName.CL_BATCH, batchOptions.toString());
            wsCs.createCollection(MetaSourceDefine.SequoiadbTableName.CL_BATCH, batchOptions);
        }
        catch (Exception e) {
            throw new MetasourceException("create meta collection failed:wsName=" + wsName, e);
        }
        finally {
            sdbMetasource.releaseConnection(sdb);
        }
    }

    @Override
    public TableDao getWorkspaceDirTableDao(String wsName, Transaction transaction)
            throws MetasourceException {
        return sdbMetasource.getCollection(transaction,
                wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                MetaSourceDefine.SequoiadbTableName.CL_DIRECTORY);
    }

    @Override
    public TableDao getDataTableNameHistoryDao() throws MetasourceException {
        return sdbMetasource.getCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_DATA_TABLE_NAME_HISTORY);
    }

    @Override
    public void deleteWorkspaceMetaTable(String wsName) throws MetasourceException {
        Sequoiadb sdb = sdbMetasource.getConnection();
        try {
            sdb.dropCollectionSpace(
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL);
            logger.info("drop workspace collectionspace:cs=" + wsName
                    + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL);
        }
        catch (Exception e) {
            throw new MetasourceException("failed drop workspace collectionspace:wsName=" + wsName
                    + ",cs=" + wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                    e);
        }
        finally {
            sdbMetasource.releaseConnection(sdb);
        }
    }

    @Override
    public TableDao getWorkspaceHistoryTable() {
        return sdbMetasource.getCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_WORKSPACE_HISTORY);
    }

}
