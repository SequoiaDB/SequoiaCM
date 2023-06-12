package com.sequoiacm.config.framework.config.workspace.metasource;

import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;
import com.sequoiacm.infrastructure.common.TableCreatedResult;
import com.sequoiacm.infrastructure.common.TableMetaCommon;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

@Repository
public class WorkspaceMetaServiceSdbImpl implements WorkspaceMetaSerivce {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceMetaServiceSdbImpl.class);

    @Autowired
    private SequoiadbMetasource sdbMetasource;
    @Value("${scm.workspace.metacs.clThreshold:1000}")
    private int maxClInMetaCs;
    @Value("${scm.workspace.extraMetacs.clThreshold:5000}")
    private int maxClInExtraMetaCs;

    @Override
    public SysWorkspaceTableDao getSysWorkspaceTable(Transaction transaction) {
        if (transaction == null) {
            return new SysWorkspaceTableDaoSdbImpl(sdbMetasource);
        }
        return new SysWorkspaceTableDaoSdbImpl(transaction);
    }

    @Override
    public SysWorkspaceHistoryTableDaoSdbImpl getSysWorkspaceHistoryTable(Transaction transaction) {
        if (transaction == null) {
            return new SysWorkspaceHistoryTableDaoSdbImpl(sdbMetasource);
        }
        return new SysWorkspaceHistoryTableDaoSdbImpl(transaction);
    }

    @PostConstruct
    public void repairMetadata() throws MetasourceException {
        // SEQUOIACM-1055: 解决 3.1.0 版本所创建工作区，可能存在的目录兼容性问题
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY,
                new BasicBSONObject("$exists", 0));
        BSONObject updater = new BasicBSONObject();
        updater.put(FieldName.FIELD_CLWORKSPACE_ENABLE_DIRECTORY, true);
        new SysWorkspaceTableDaoSdbImpl(sdbMetasource).update(matcher, updater);
    }

    @Override
    public void createWorkspaceMetaTable(WorkspaceConfig wsConfig) throws MetasourceException {
        String wsName = wsConfig.getWsName();
        BSONObject metalocation = wsConfig.getMetalocation();
        String domain = (String) metalocation.get("domain");
        if (domain == null) {
            throw new IllegalArgumentException("metalocation missing domain filed:" + metalocation);
        }

        CollectionSpace wsCs = null;
        DBCollection tagLibCl = null;
        Sequoiadb sdb = sdbMetasource.getConnection();
        try {
            BSONObject csOption = new BasicBSONObject("Domain", domain);
            BSONObject metaOptions = BsonUtils.getBSON(metalocation,
                    FieldName.FIELD_CLWORKSPACE_META_OPTIONS);
            if (metaOptions != null) {
                BSONObject customCsOptions = BsonUtils.getBSON(metaOptions,
                        FieldName.FIELD_CLWORKSPACE_META_CS);
                if (customCsOptions != null) {
                    csOption.putAll(customCsOptions);
                }
            }

            logger.info("creating cs:csName=" + wsName
                    + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL + ",options="
                    + csOption.toString());
            wsCs = sdb.createCollectionSpace(
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL, csOption);

            if (wsConfig.isEnableDirectory()) {
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
                BSONObject relClShardingKey = new BasicBSONObject(
                        FieldName.FIELD_CLREL_DIRECTORY_ID, 1);
                relClShardingKey.put(FieldName.FIELD_CLREL_FILENAME, 1);
                relClOptions.put("ShardingKey", relClShardingKey);
                logger.info("creating cl:clName={}.{},options={}",
                        wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                        MetaSourceDefine.SequoiadbTableName.CL_FILE_RELATION,
                        relClOptions.toString());
                DBCollection relCl = wsCs.createCollection(
                        MetaSourceDefine.SequoiadbTableName.CL_FILE_RELATION, relClOptions);
                BSONObject relIdx = new BasicBSONObject();
                relIdx.put(FieldName.FIELD_CLREL_DIRECTORY_ID, 1);
                relIdx.put(FieldName.FIELD_CLREL_FILENAME, 1);
                logger.info("creating index:clName={},key={},isUnique={},enforced={}",
                        relCl.getFullName(), relIdx.toString(), true, true);
                relCl.createIndex("idx_name_pid", relIdx, true, true);
            }
            // CLASS
            logger.info("creating cl:clName={}.{},options=null",
                    wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                    MetaSourceDefine.SequoiadbTableName.CL_CLASS);
            DBCollection classCl = wsCs
                    .createCollection(MetaSourceDefine.SequoiadbTableName.CL_CLASS, null);
            BSONObject classNameIdx = new BasicBSONObject();
            classNameIdx.put(FieldName.Class.FIELD_NAME, 1);
            logger.info("creating index:clName={},key={},isUnique={},enforced={}",
                    classCl.getFullName(), classNameIdx.toString(), true, true);
            classCl.createIndex("idx_class_name", classNameIdx, true, true);
            BSONObject classIdIdx = new BasicBSONObject();
            classIdIdx.put(FieldName.Class.FIELD_ID, 1);
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
            attrNameIdx.put(FieldName.Attribute.FIELD_NAME, 1);
            logger.info("creating index:clName={},key={},isUnique={},enforced={}",
                    attrCl.getFullName(), attrNameIdx.toString(), true, true);
            attrCl.createIndex("idx_attr_name", attrNameIdx, true, true);
            BSONObject attrIdIdx = new BasicBSONObject();
            attrIdIdx.put(FieldName.Attribute.FIELD_ID, 1);
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
            classAttrRelIdx.put(FieldName.ClassAttrRel.FIELD_CLASS_ID, 1);
            classAttrRelIdx.put(FieldName.ClassAttrRel.FIELD_ATTR_ID, 1);
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
            if (wsConfig.getBatchShardingType().equals(ScmShardingType.NONE.getName())) {
                BSONObject key = new BasicBSONObject(FieldName.Batch.FIELD_ID, 1);
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
            else {
                BSONObject batchOptions = new BasicBSONObject();
                batchOptions.put("ShardingType", "range");
                BSONObject batchClShardingKey = new BasicBSONObject(
                        FieldName.Batch.FIELD_INNER_CREATE_MONTH, 1);
                batchOptions.put("ShardingKey", batchClShardingKey);
                batchOptions.put("IsMainCL", true);
                logger.info("creating cl:clName={}.{},options={}",
                        wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL,
                        MetaSourceDefine.SequoiadbTableName.CL_BATCH, batchOptions.toString());
                wsCs.createCollection(MetaSourceDefine.SequoiadbTableName.CL_BATCH, batchOptions);
            }

            // TAG LIB
            BasicBSONObject tagLibCsOption = new BasicBSONObject("Domain",
                    BsonUtils.getStringChecked(wsConfig.getTagLibMetaOption(),
                            FieldName.FIELD_CLWORKSPACE_TAG_LIB_META_OPTION_DOMAIN));
            String[] csClArr = wsConfig.getTagLibTableName().split("\\.");
            if (csClArr.length != 2) {
                throw new IllegalArgumentException(
                        "tagLibTableName is invalid: " + wsConfig.getTagLibTableName());
            }
            String tagLibCsName = csClArr[0];
            CollectionSpace tagLibCs;
            try {
                tagLibCs = sdb.createCollectionSpace(tagLibCsName, tagLibCsOption);
            }
            catch (BaseException e) {
                if (e.getErrorCode() != SDBError.SDB_DMS_CS_EXIST.getErrorCode()) {
                    throw e;
                }
                tagLibCs = sdb.getCollectionSpace(tagLibCsName);
            }
            tagLibCl = tagLibCs.createCollection(csClArr[1]);

            BasicBSONObject tagUniqueIdx = new BasicBSONObject(FieldName.TagLib.TAG, 1);
            tagLibCl.createIndex("tag_unique_idx", tagUniqueIdx, true, false);

            BasicBSONObject customTagUniqueIdx = new BasicBSONObject(FieldName.TagLib.CUSTOM_TAG,
                    1);
            tagLibCl.createIndex("custom_tag_unique_idx", customTagUniqueIdx, true, false);

            if (Objects.equals(wsConfig.getTagRetrievalStatus(),
                    CommonDefine.WorkspaceTagRetrievalStatus.ENABLED)) {
                tagLibCl.createIndex("tag_lib_fulltext_idx",
                        FieldName.TagLib.tagLibFulltextIdxDef(),
                        FieldName.TagLib.tagLibFulltextIdxAttr(), null);
            }
        }
        catch (Exception e) {
            if (wsCs != null) {
                dropCSSilence(wsCs.getName(), true);
            }
            if (tagLibCl != null) {
                dropCLSilence(tagLibCl, true);
            }
            throw new MetasourceException("create meta collection failed:wsName=" + wsName, e);
        }
        finally {
            sdbMetasource.releaseConnection(sdb);
        }
    }

    private void dropCSSilence(String cs, boolean skipRecycleBin) {
        Sequoiadb db = null;
        try {
            db = sdbMetasource.getConnection();
            TableMetaCommon.dropCSWithSkipRecycleBin(db, cs, skipRecycleBin);
        }
        catch (Exception e) {
            logger.warn("failed to drop cs:{}", cs, e);
        }
        finally {
            if (db != null) {
                sdbMetasource.releaseConnection(db);
            }
        }

    }

    private void dropCLSilence(DBCollection cl, boolean skipRecycleBin) {
        try {
            CollectionSpace cs = cl.getCollectionSpace();
            TableMetaCommon.dropCLWithSkipRecycleBin(cs, cl.getName(), skipRecycleBin);
        }
        catch (Exception e) {
            logger.warn("drop collection failed:clName={}", cl.getFullName(), e);
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
    public void deleteWorkspaceMetaTable(BSONObject wsRecord, boolean skipRecycleBin)
            throws MetasourceException {
        List<String> extraMetaList = TableMetaCommon.getExtraCsList(wsRecord);
        extraMetaList.add(BsonUtils.getStringChecked(wsRecord, FieldName.FIELD_CLWORKSPACE_NAME)
                + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL);
        Sequoiadb sdb = sdbMetasource.getConnection();
        try {
            for (String cs : extraMetaList) {
                try {
                    dropCS(sdb, cs, skipRecycleBin);
                }
                catch (Exception e) {
                    logger.warn("failed to drop cs: {}", cs, e);
                }
            }
            String tagLibTable = BsonUtils.getString(wsRecord,
                    FieldName.FIELD_CLWORKSPACE_TAG_LIB_TABLE);
            if (tagLibTable != null) {
                String[] csClArr = tagLibTable.split("\\.");
                if (csClArr.length != 2) {
                    logger.warn("failed to drop tagLibTable, tagLibTableName is invalid: "
                            + tagLibTable);
                }
                String tagLibCsName = csClArr[0];
                try {
                    CollectionSpace tagLibCs = sdb.getCollectionSpace(tagLibCsName);
                    TableMetaCommon.dropCLWithSkipRecycleBin(tagLibCs, csClArr[1], skipRecycleBin);
                }
                catch (BaseException e) {
                    if (e.getErrorCode() != SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()
                            && e.getErrorCode() != SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                        logger.warn("failed to drop tagLibTable: {}", tagLibTable, e);
                    }
                }
            }
        }
        finally {
            sdbMetasource.releaseConnection(sdb);
        }
    }

    private void dropCS(Sequoiadb sdb, String cs, boolean skipRecycleBin)
            throws MetasourceException {
        try {
            TableMetaCommon.dropCSWithSkipRecycleBin(sdb, cs, skipRecycleBin);
            logger.info("drop workspace collectionspace:cs=" + cs);
        }
        catch (Exception e) {
            throw new MetasourceException("failed drop workspace collectionspace:cs=" + cs, e);
        }
    }

    public TableCreatedResult createClInWorkspaceMetaCs(String wsName, String clName,
            BSONObject clOption, boolean isIgnoreClExistErr) throws ScmConfigException {
        SysWorkspaceTableDao wsTable = getSysWorkspaceTable(null);
        BSONObject wsRecord = wsTable.queryOne(
                new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME, wsName), null, null);
        if (wsRecord == null) {
            throw new ScmConfigException(ScmConfError.WORKSPACE_NOT_EXIST,
                    "workspace not exist:" + wsName);
        }
        Sequoiadb db = null;
        try {
            db = sdbMetasource.getConnection();
            TableCreatedResult tableCreatedResult = TableMetaCommon.createTable(db,
                    wsRecord, wsName, clName, clOption, isIgnoreClExistErr);
            return tableCreatedResult;
        }
        catch (Exception e) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "failed to create cl, cl=" + clName + ", clOption=" + clOption);
        }
        finally {
            sdbMetasource.releaseConnection(db);
        }

    }
}
