package com.sequoiacm.contentserver.common;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.IndexName;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.bizconfig.ContenserverConfClient;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import com.sequoiacm.infrastructure.common.TableCreatedResult;
import com.sequoiacm.infrastructure.common.TableMetaCommon;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceUpdater;
import com.sequoiacm.metasource.MetaSourceDefine;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.metasource.sequoiadb.config.SdbClFileInfo;
import com.sequoiacm.metasource.sequoiadb.config.SdbMetaSourceLocation;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.DBQuery;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;

public class FileTableCreator {

    private static final Logger logger = LoggerFactory.getLogger(FileTableCreator.class);

    public static void createSubFileTable(SdbMetaSource metaSource, ScmWorkspaceInfo wsInfo,
            BSONObject file) throws ScmMetasourceException {
        long createTime = (long) file.get(FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        String fileId = (String) file.get(FieldName.FIELD_CLFILE_ID);
        Date createDate = new Date(createTime);
        String timezone = ScmIdParser.getTimezoneName(fileId);

        Sequoiadb sdb = null;
        try {
            sdb = metaSource.getConnection();
            createSubCl(sdb, wsInfo.getName(), createDate, timezone,
                    (SdbMetaSourceLocation) wsInfo.getMetaLocation());
            createSubHistoryFile(sdb, wsInfo.getName(), createDate, timezone,
                    (SdbMetaSourceLocation) wsInfo.getMetaLocation());
        }
        finally {
            metaSource.releaseConnection(sdb);
        }
    }

    public static BucketConfig createBucketTable(SdbMetaSource metaSource, String user, String ws,
            String bucketName) throws ScmMetasourceException, ScmConfigException {
        Sequoiadb sdb = null;
        try {
            sdb = metaSource.getConnection();
            if (isBucketExist(sdb, bucketName)) {
                throw new ScmConfigException(ScmConfError.BUCKET_EXIST,
                        "bucket exist, bucket: " + bucketName);
            }
            Date createTime = new Date();
            long bucketId = genBucketId(sdb);
            String tableName = createBucketFileTable(sdb, ws, bucketId);
            BucketConfig config = new BucketConfig();
            config.setId(bucketId);
            config.setCreateTime(createTime.getTime());
            config.setUpdateTime(createTime.getTime());
            config.setFileTable(tableName);
            config.setCreateUser(user);
            config.setWorkspace(ws);
            config.setName(bucketName);
            config.setVersionStatus(ScmBucketVersionStatus.Disabled.name());
            config.setCustomTag(new HashMap<>());
            config.setUpdateUser(user);
            return config;
        }
        finally {
            metaSource.releaseConnection(sdb);
        }
    }

    private static String createBucketFileTable(Sequoiadb sdb, String wsName, long bucketId)
            throws ScmMetasourceException {
        BSONObject clOption = TableMetaCommon.genBucketTableOption();
        String clName = "BUCKET_FILE_" + bucketId;
        // 创建桶集合，由于桶集合名由bucketId拼接生成，理论上唯一，因此这里如果出现集合存在的异常，不能忽略
        String csName = createClAndGetCsName(sdb, wsName, clName, clOption, false);

        try {
            SequoiadbHelper.createIndex(sdb, csName, clName,
                    IndexName.BucketFile.FILE_NAME_UNIQUE_IDX,
                    new BasicBSONObject(FieldName.BucketFile.FILE_NAME, 1), true, false);
        }
        catch (Exception e) {
            dropCLSilence(sdb, csName, clName, true);
            throw e;
        }
        return csName + "." + clName;
    }

    private static long genBucketId(Sequoiadb sdb) throws ScmMetasourceException {
        BSONObject updater = new BasicBSONObject();
        BSONObject incId = new BasicBSONObject("id", 1);
        updater.put(SequoiadbHelper.SEQUOIADB_MODIFIER_INC, incId);
        BSONObject newRecord = updateAndReturnNew(sdb, new BasicBSONObject("type", "scm_bucket"),
                updater);
        if (newRecord == null) {
            throw new ScmMetasourceException(
                    "id record is not exist: table:" + MetaSourceDefine.CsName.CS_SCMSYSTEM + "."
                            + MetaSourceDefine.SystemClName.CL_ID_GEN);
        }
        return BsonUtils.getNumberChecked(newRecord, "id").longValue();
    }

    private static BSONObject updateAndReturnNew(Sequoiadb db, BSONObject matcher,
            BSONObject updator) throws ScmMetasourceException {
        DBCursor cursor = null;
        try {
            DBCollection cl = db.getCollectionSpace(MetaSourceDefine.CsName.CS_SCMSYSTEM)
                    .getCollection(MetaSourceDefine.SystemClName.CL_ID_GEN);
            cursor = cl.queryAndUpdate(matcher, null, null, null, updator, 0, -1,
                    DBQuery.FLG_QUERY_WITH_RETURNDATA, true);
            BSONObject ret = null;
            while (cursor.hasNext()) {
                ret = cursor.getNext();
            }
            return ret;
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                throw new ScmMetasourceException(
                        "record alredy exist:csName=" + MetaSourceDefine.CsName.CS_SCMSYSTEM
                                + ",clName=" + MetaSourceDefine.SystemClName.CL_ID_GEN + ",matcher="
                                + matcher + ",updator=" + updator,
                        e);
            }
            throw new ScmMetasourceException(
                    "update failed:csName=" + MetaSourceDefine.CsName.CS_SCMSYSTEM + ",clName="
                            + MetaSourceDefine.SystemClName.CL_ID_GEN + ",matcher=" + matcher
                            + ",updator=" + updator,
                    e);
        }
        catch (Exception e) {
            throw new ScmMetasourceException(
                    "update failed:csName=" + MetaSourceDefine.CsName.CS_SCMSYSTEM + ",clName="
                            + MetaSourceDefine.SystemClName.CL_ID_GEN + ",matcher=" + matcher
                            + ",updator=" + updator,
                    e);
        }
        finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    private static boolean isBucketExist(Sequoiadb sdb, String bucketName) {
        CollectionSpace cs = sdb.getCollectionSpace(MetaSourceDefine.CsName.CS_SCMSYSTEM);
        DBCollection cl = cs.getCollection(MetaSourceDefine.SystemClName.CL_BUCKET);
        BSONObject bucketRecord = cl.queryOne(
                new BasicBSONObject(FieldName.Bucket.NAME, bucketName), null, null, null, 0);
        return bucketRecord != null;
    }

    private static void createSubHistoryFile(Sequoiadb sdb, String wsName, Date createDate,
            String timezone, SdbMetaSourceLocation location) throws ScmMetasourceException {
        SdbClFileInfo metaCLInfo = location.getClFileInfo(getClName(), createDate, timezone);
        String subClName = metaCLInfo.getClHistoryName();
        BSONObject options = mergeClOptions(generatorClFileOptions(), metaCLInfo.getClOptions());
        // 创建文件子表，若集合存在，忽略异常
        String csName = createClAndGetCsName(sdb, wsName, subClName, options, true);
        String clFullName = csName + "." + subClName;
        try {
            BSONObject indexDef = new BasicBSONObject();
            indexDef.put(FieldName.FIELD_CLFILE_ID, 1);
            indexDef.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, 1);
            indexDef.put(FieldName.FIELD_CLFILE_MINOR_VERSION, 1);
            SequoiadbHelper.createIndex(sdb, csName, subClName,
                    "idx_" + FieldName.FIELD_CLFILE_ID + "_version", indexDef,
                    true, false);

            createDataIdIndex(sdb, csName, subClName);

            indexDef = new BasicBSONObject();
            indexDef.put(FieldName.FIELD_CLFILE_NAME, 1);
            indexDef.put(FieldName.FIELD_CLFILE_VERSION_SERIAL, -1);
            indexDef.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, -1);
            indexDef.put(FieldName.FIELD_CLFILE_MINOR_VERSION, -1);
            SequoiadbHelper.createIndex(sdb, csName, subClName,
                    IndexName.HistoryFile.NAME_VERSION_UNION_IDX, indexDef, false, false);

            createDataCreateTimeIndex(sdb, csName, subClName);

            attachCl(sdb, getCsName(wsName), getClName() + "_HISTORY", clFullName,
                    metaCLInfo.getLowMonth(), metaCLInfo.getUpperMonth());
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "create file table failed:cl=" + clFullName, e);
        }
    }

    private static void createSubCl(Sequoiadb sdb, String wsName, Date createDate, String timezone,
            SdbMetaSourceLocation location) throws ScmMetasourceException {
        SdbClFileInfo metaCLInfo = location.getClFileInfo(getClName(), createDate, timezone);
        String subClName = metaCLInfo.getClName();
        BSONObject options = mergeClOptions(generatorClFileOptions(), metaCLInfo.getClOptions());
        // 创建文件子表，若集合存在，忽略异常
        String csName = createClAndGetCsName(sdb, wsName, subClName, options, true);
        String clFullName = csName + "." + subClName;
        try {
            BSONObject indexDef = new BasicBSONObject();
            indexDef.put(FieldName.FIELD_CLFILE_ID, 1);
            SequoiadbHelper.createIndex(sdb, csName, subClName,
                    "idx_" + FieldName.FIELD_CLFILE_ID, indexDef, true, false);

            createDataIdIndex(sdb, csName, subClName);

            indexDef = new BasicBSONObject();
            indexDef.put(FieldName.FIELD_CLFILE_NAME, 1);
            SequoiadbHelper.createIndex(sdb, csName, subClName,
                    "idx_" + FieldName.FIELD_CLREL_FILENAME, indexDef, false,
                    false);

            indexDef = new BasicBSONObject();
            indexDef.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, 1);
            SequoiadbHelper.createIndex(sdb, csName, subClName,
                    "idx_" + FieldName.FIELD_CLFILE_INNER_CREATE_TIME, indexDef,
                    false, false);

            createDataCreateTimeIndex(sdb, csName, subClName);

            attachCl(sdb, getCsName(wsName), getClName(), clFullName, metaCLInfo.getLowMonth(),
                    metaCLInfo.getUpperMonth());
        }
        catch (SdbMetasourceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "create file table failed:cl=" + clFullName, e);
        }
    }

    private static void createDataIdIndex(Sequoiadb sdb, String csName, String subClName)
            throws SdbMetasourceException {
        BasicBSONObject indexDef = new BasicBSONObject();
        indexDef.put(FieldName.FIELD_CLFILE_FILE_DATA_ID, 1);
        SequoiadbHelper.createIndex(sdb, csName, subClName,
                "idx_" + FieldName.FIELD_CLFILE_FILE_DATA_ID, indexDef, false,
                false);
    }

    private static void createDataCreateTimeIndex(Sequoiadb sdb, String csName, String subClName)
            throws SdbMetasourceException {
        BasicBSONObject indexDef = new BasicBSONObject();
        indexDef.put(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME, 1);
        SequoiadbHelper.createIndex(sdb, csName, subClName,
                "idx_" + FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME, indexDef,
                false, false);
    }

    private static void attachCl(Sequoiadb sdb, String csName, String clName, String clFullName,
            String lowMonth, String upperMonth) throws SdbMetasourceException {
        BSONObject lowb = new BasicBSONObject();
        lowb.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, lowMonth);
        BSONObject upperb = new BasicBSONObject();
        upperb.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, upperMonth);

        BSONObject options = new BasicBSONObject();
        options.put("LowBound", lowb);
        options.put("UpBound", upperb);
        logger.info("attaching cl:cl=" + clFullName + ",options=" + options.toString());
        SequoiadbHelper.attachCL(sdb, csName, clName, clFullName, options);
    }

    private static String createClAndGetCsName(Sequoiadb sdb, String wsName, String clName,
            BSONObject clOptions, boolean isIgnoreClExistErr) throws ScmMetasourceException {
        BSONObject wsRecord = getWorkspace(sdb, wsName);
        if (null == wsRecord) {
            throw new ScmMetasourceException("workspace is not exist,workspace: " + wsName);
        }
        try {
            TableCreatedResult tableCreatedResult = TableMetaCommon.createTable(sdb,
                    wsRecord, wsName, clName, clOptions, isIgnoreClExistErr);
            String cs = tableCreatedResult.getCsName();
            if (!tableCreatedResult.isInExtraCs()) {
                // cs 不在 extra_meta_cs 里，需要添加到工作区的 extra_meta_cs 列表里
                addToExtraCsListSilence(wsRecord, wsName, cs);
            }
            return cs;
        }
        catch (Exception e) {
            throw new ScmMetasourceException(
                    "failed to create cl, cl: " + clName + ", clOptions: " + clOptions, e);
        }
    }

    private static void addToExtraCsListSilence(BSONObject wsRecord, String wsName, String newCs) {
        WorkspaceUpdater updator = new WorkspaceUpdater(wsName, wsRecord);
        updator.setAddExtraMetaCs(newCs);
        try {
            ContenserverConfClient.getInstance().updateWorkspaceConf(updator);
        }
        catch (Exception e) {
            // 通知添加 cs 失败，忽略异常，下一次创建子表也会继续通知
            logger.warn(
                    "Failed to update workspace extra meta cs list,workspace: {}, extraMetaCS: {}",
                    wsName, newCs, e);
        }
    }

    private static void dropCLSilence(Sequoiadb db, String csName, String clName,
            boolean skipRecycleBin) {
        try {
            CollectionSpace cs = db.getCollectionSpace(csName);
            TableMetaCommon.dropCLWithSkipRecycleBin(cs, clName, skipRecycleBin);
        }
        catch (Exception e) {
            logger.warn("failed to drop cl:{}", csName + "." + clName, e);
        }
    }

    private static BSONObject getWorkspace(Sequoiadb sdb, String wsName) {
        CollectionSpace cs = sdb.getCollectionSpace(MetaSourceDefine.CsName.CS_SCMSYSTEM);
        DBCollection cl = cs.getCollection(MetaSourceDefine.SystemClName.CL_WORKSPACE);
        BSONObject matcher = new BasicBSONObject(FieldName.FIELD_CLWORKSPACE_NAME, wsName);
        return cl.queryOne(matcher, null, null, null, 0);
    }

    private static String getClName() {
        return MetaSourceDefine.WorkspaceCLName.CL_FILE;
    }

    private static String getCsName(String wsName) {
        return wsName + "_META";
    }

    private static BSONObject mergeClOptions(BSONObject innerOptions, BSONObject userOptions) {
        BSONObject res = new BasicBSONObject();
        if (innerOptions != null) {
            res.putAll(innerOptions);
        }
        if (userOptions != null) {
            res.putAll(userOptions);
        }
        Boolean compressed = BsonUtils.getBoolean(res, "Compressed");
        if (compressed != null && !compressed) {
            res.removeField("CompressionType");
        }
        return res;
    }

    private static BSONObject generatorClFileOptions() {
        BSONObject key = new BasicBSONObject(FieldName.FIELD_CLFILE_ID, 1);
        BSONObject options = new BasicBSONObject();
        options.put("ShardingType", "hash");
        options.put("ShardingKey", key);
        options.put("Compressed", true);
        options.put("CompressionType", "lzw");
        options.put("ReplSize", -1);
        options.put("AutoSplit", true);
        options.put("EnsureShardingIndex", false);

        return options;
    }
}
