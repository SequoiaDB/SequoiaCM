package com.sequoiacm.config.framework.workspace.metasource;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.config.framework.lock.ScmLockManager;
import com.sequoiacm.config.framework.lock.ScmLockPathFactory;
import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.lock.ScmLock;
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
    public void createWorkspaceMetaTable(WorkspaceConfig wsConfig) throws MetasourceException {
        String wsName = wsConfig.getWsName();
        BSONObject metalocation = wsConfig.getMetalocation();
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
                        FieldName.FIELD_CLWORKSPACE_META_CS);
                if (customCsOptions != null) {
                    csOption.putAll(customCsOptions);
                }
            }

            logger.info("creating cs:csName=" + wsName
                    + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL + ",options="
                    + csOption.toString());
            CollectionSpace wsCs = sdb.createCollectionSpace(
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
    public void deleteWorkspaceMetaTable(BSONObject wsRecord) throws MetasourceException {
        List<String> extraMetaList = getExtraCsList(wsRecord);
        extraMetaList.add(BsonUtils.getStringChecked(wsRecord, FieldName.FIELD_CLWORKSPACE_NAME)
                + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL);
        Sequoiadb sdb = sdbMetasource.getConnection();
        try {
            for (String cs : extraMetaList) {
                try {
                    dropCS(sdb, cs);
                }
                catch (Exception e) {
                    logger.warn("failed to drop cs: {}", cs, e);
                }
            }
        }
        finally {
            sdbMetasource.releaseConnection(sdb);
        }
    }

    private void dropCS(Sequoiadb sdb, String cs) throws MetasourceException {
        try {
            sdb.dropCollectionSpace(cs);
            logger.info("drop workspace collectionspace:cs=" + cs);
        }
        catch (Exception e) {
            throw new MetasourceException("failed drop workspace collectionspace:cs=" + cs, e);
        }
    }

    @Override
    public void deleteWorkspaceMetaTable(String wsName) throws MetasourceException {
        Sequoiadb sdb = sdbMetasource.getConnection();
        try {
            dropCS(sdb, wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL);
        }
        finally {
            sdbMetasource.releaseConnection(sdb);
        }
    }

    public String createClInWorkspaceMetaCs(String wsName, String clName, BSONObject clOption)
            throws ScmConfigException {
        SysWorkspaceTableDao wsTable = getSysWorkspaceTable(null);
        BSONObject wsRecord = wsTable.queryOne(
                new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME, wsName), null, null);
        if (wsRecord == null) {
            throw new ScmConfigException(ScmConfError.WORKSPACE_NOT_EXIST,
                    "workspace not exist:" + wsName);
        }

        List<String> extraCsList = getExtraCsList(wsRecord);
        // 逆序遍历，最后一个CS是最近建立，先在它上面尝试
        for(int i = extraCsList.size() - 1; i >= 0; i--){
            if (createCL(extraCsList.get(i), maxClInExtraMetaCs, clName, clOption)) {
                return extraCsList.get(i) + "." + clName;
            }
        }

        String wsMetaCsName = wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL;
        if (createCL(wsMetaCsName, maxClInMetaCs, clName, clOption)) {
            return wsMetaCsName + "." + clName;
        }

        ScmLock lock = ScmLockManager.getInstance()
                .acquiresLock(ScmLockPathFactory.createWorkspaceExtraCsLockPath());
        try {
            wsRecord = wsTable.queryOne(
                    new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME, wsName), null, null);
            if (wsRecord == null) {
                throw new ScmConfigException(ScmConfError.WORKSPACE_NOT_EXIST,
                        "workspace not exist:" + wsName);
            }
            List<String> extraCsListInLock = getExtraCsList(wsRecord);
            if (!extraCsListInLock.equals(extraCsList)) {
                // 锁后有人建立了新的CS，再这个CS上先试下能不能建立CL
                String latestCsName = extraCsListInLock.get(extraCsListInLock.size() - 1);
                if (createCL(latestCsName, maxClInExtraMetaCs, clName, clOption)) {
                    return latestCsName + "." + clName;
                }
            }
            String newExtraCsName = getNextExtraCsName(wsName, extraCsListInLock);
            boolean isCreateCs = createExtraMetaCs(wsRecord, newExtraCsName);
            boolean isUpdateWsExtCsList = false;
            try {
                extraCsListInLock.add(newExtraCsName);
                if (!createCL(newExtraCsName, maxClInExtraMetaCs, clName, clOption)) {
                    // 在刚刚创建的 CS 下建立 CL，发生了超出CL个数限制异常，逻辑上是不可能的，因为这个 CS 还没有登记到工作区元数据上
                    // 其它人还看不到这个 CS，所以这里抛个系统异常
                    wsTable.update(new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME, wsName),
                            new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_EXTRA_META_CS,
                                    extraCsListInLock));
                    isUpdateWsExtCsList = true;
                    throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                            "failed to create cl in extra meta cs:" + newExtraCsName + ", cl="
                                    + clName + ", clOption=" + clOption);
                }
                wsTable.update(new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME, wsName),
                        new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_EXTRA_META_CS,
                                extraCsListInLock));

                isUpdateWsExtCsList = true;
                return newExtraCsName + "." + clName;
            }
            catch (Exception e) {
                if (isCreateCs && !isUpdateWsExtCsList) {
                    dropCSSilence(newExtraCsName);
                }
                throw e;
            }
        }
        finally {
            lock.unlock();
        }

    }

    private void dropCSSilence(String cs) {
        Sequoiadb db = null;
        try {
            db = sdbMetasource.getConnection();
            db.dropCollectionSpace(cs);
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

    // 返回 false 表示 cs 存在
    private boolean createExtraMetaCs(BSONObject ws, String csName) throws MetasourceException {
        BSONObject metaLocation = BsonUtils.getBSONChecked(ws,
                FieldName.FIELD_CLWORKSPACE_META_LOCATION);
        String domain = BsonUtils.getStringChecked(metaLocation,
                FieldName.FIELD_CLWORKSPACE_LOCATION_DOMAIN);
        BSONObject csOption = new BasicBSONObject("Domain", domain);
        BSONObject metaOptions = BsonUtils.getBSON(metaLocation,
                FieldName.FIELD_CLWORKSPACE_META_OPTIONS);
        if (metaOptions != null) {
            BSONObject customCsOptions = BsonUtils.getBSON(metaOptions,
                    FieldName.FIELD_CLWORKSPACE_META_CS);
            if (customCsOptions != null) {
                csOption.putAll(customCsOptions);
            }
        }
        Sequoiadb db = sdbMetasource.getConnection();
        try {
            db.createCollectionSpace(csName, csOption);
            return true;
        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_DMS_CS_EXIST.getErrorCode()) {
                throw e;
            }
            return false;
        }
        finally {
            sdbMetasource.releaseConnection(db);
        }

    }

    private String getNextExtraCsName(String wsName, List<String> extraCsListInLock) {
        if (extraCsListInLock.size() <= 0) {
            return wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL + "_" + 1;
        }
        String latestExtraCS = extraCsListInLock.get(extraCsListInLock.size() - 1);
        String numStr = latestExtraCS.substring(latestExtraCS.lastIndexOf("_") + 1);
        int newExtraCsNum = Integer.parseInt(numStr) + 1;
        return wsName + MetaSourceDefine.SequoiadbTableName.CS_WORKSPACE_META_TAIL + "_"
                + newExtraCsNum;
    }

    // maxClInCs <= -1 表示不限制CS下有多少集合
    // 返回 false 表示 CS 下已经超过 maxClInCs 个集合，无法创建集合
    private boolean createCL(String csName, int maxClInCs, String clName, BSONObject clOption)
            throws MetasourceException {
        Sequoiadb db = sdbMetasource.getConnection();
        try {
            CollectionSpace cs = db.getCollectionSpace(csName);
            if (maxClInCs <= -1) {
                cs.createCollection(clName, clOption);
                return true;
            }
            if (cs.getCollectionNames().size() >= maxClInCs) {
                return false;
            }
            cs.createCollection(clName, clOption);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_NOSPC.getErrorCode()) {
                return false;
            }
            throw e;
        }
        finally {
            sdbMetasource.releaseConnection(db);
        }
        return true;
    }

    private List<String> getExtraCsList(BSONObject wsRecord) {
        List<String> ret = new ArrayList<>();
        BasicBSONList extraMetaCS = BsonUtils.getArray(wsRecord,
                FieldName.FIELD_CLWORKSPACE_EXTRA_META_CS);
        if (extraMetaCS == null) {
            return ret;
        }
        for (Object csName : extraMetaCS) {
            ret.add((String) csName);
        }
        return ret;
    }

    @Override
    public TableDao getWorkspaceHistoryTable() {
        return sdbMetasource.getCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_WORKSPACE_HISTORY);
    }
}
