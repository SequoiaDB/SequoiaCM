package com.sequoiacm.contentserver.common;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.statistics.common.ScmStatisticsFileMeta;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaRelAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ScmFileOperateUtils {
    private static final Logger logger = LoggerFactory.getLogger(ScmFileOperateUtils.class);

    private static BucketInfoManager bucketInfoManager;

    @Autowired
    public void setBucketInfoManager(BucketInfoManager bucketInfoManager) {
        ScmFileOperateUtils.bucketInfoManager = bucketInfoManager;
    }

    public static ScmLock lockDirForCreateFile(ScmWorkspaceInfo wsInfo, String parentId)
            throws ScmServerException {
        if (wsInfo.isEnableDirectory()
                && !parentId.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            ScmLockPath lockPath = ScmLockPathFactory.createDirLockPath(wsInfo.getName(), parentId);
            return ScmLockManager.getInstance().acquiresReadLock(lockPath);
        }
        return null;
    }

    public static void checkFileName(ScmWorkspaceInfo ws, String name) throws ScmServerException {
        if (!ScmArgChecker.File.checkFileName(name)) {
            throw new ScmServerException(ScmError.INVALID_ARGUMENT,
                    "invalid file name : fileName=" + name);
        }
        if (ws.isEnableDirectory()) {
            if (name.contains("/")) {
                throw new ScmServerException(ScmError.INVALID_ARGUMENT,
                        "the workspace directory feature is enable, file name can not contains / : ws="
                                + ws.getName() + ", fileName=" + name);
            }
        }
    }

    public static void checkDirForCreateFile(ScmWorkspaceInfo wsInfo, String parentId)
            throws ScmServerException {
        if (wsInfo.isEnableDirectory()) {
            ScmMetaService metaservice = ScmContentModule.getInstance().getMetaService();
            BSONObject parentDirMatcher = new BasicBSONObject();
            parentDirMatcher.put(FieldName.FIELD_CLDIR_ID, parentId);
            if (metaservice.getDirCount(wsInfo.getName(), parentDirMatcher) <= 0) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "parent directory not exists:preantDirectoryId=" + parentId);
            }
        }
    }

    public static void insertFileRelForCreateFile(ScmWorkspaceInfo wsInfo, BSONObject fileInfo,
            TransactionContext context) throws ScmServerException, ScmMetasourceException {
        if (wsInfo.isEnableDirectory()) {
            BSONObject relInsertor = ScmMetaSourceHelper.createRelInsertorByFileInsertor(fileInfo);
            MetaRelAccessor relAccessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource().getRelAccessor(wsInfo.getName(), context);
            relAccessor.insert(relInsertor);
        }
    }

    public static void updateFileRelForUpdateFile(ScmWorkspaceInfo wsInfo, String fileId,
            BSONObject oldFileRecord, BSONObject relUpdator, TransactionContext context)
            throws ScmServerException, ScmMetasourceException {
        if (wsInfo.isEnableDirectory()) {
            MetaRelAccessor relAccessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource().getRelAccessor(wsInfo.getName(), context);
            String oldDirId = BsonUtils.getStringChecked(oldFileRecord,
                    FieldName.FIELD_CLFILE_DIRECTORY_ID);
            String oldFileName = BsonUtils.getStringChecked(oldFileRecord,
                    FieldName.FIELD_CLFILE_NAME);
            relAccessor.updateRel(fileId, oldDirId, oldFileName, relUpdator);
        }
    }

    public static void updateBucketFileForUpdateFile(BSONObject fileUpdater,
            BSONObject oldFileRecord, TransactionContext context)
            throws ScmServerException, ScmMetasourceException {
        Number bucketId = BsonUtils.getNumber(oldFileRecord, FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        if (bucketId == null) {
            return;
        }
        ScmBucket bucket = bucketInfoManager.getBucketById(bucketId.longValue());
        if (bucket == null) {
            return;
        }
        // 若文件在 Bucket 中，尝试更新文件名、BucketID 上层就会报错，不会走到这个函数，这里如果发现 fileUpdater
        // 包含这两个属性的更新，也进行报错
        if (fileUpdater.containsField(FieldName.FIELD_CLFILE_NAME)) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "can not rename bucket file：" + oldFileRecord);
        }
        if (fileUpdater.containsField(FieldName.FIELD_CLFILE_FILE_BUCKET_ID)) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "can not reset bucket id：" + oldFileRecord);
        }
        BSONObject bucketFileUpdater = ScmMetaSourceHelper
                .createBucketFileUpdatorByFileUpdator(fileUpdater);
        MetaAccessor bucketFileAccessor = bucket.getFileTableAccessor(context);
        bucketFileAccessor.update(
                new BasicBSONObject(FieldName.BucketFile.FILE_NAME,
                        oldFileRecord.get(FieldName.FIELD_CLFILE_NAME)),
                new BasicBSONObject(ScmMetaSourceHelper.SEQUOIADB_MODIFIER_SET, bucketFileUpdater));
    }

    public static void deleteFileRelForDeleteFile(ScmWorkspaceInfo ws, String fileID,
            BSONObject deletedFileRecord, TransactionContext context)
            throws ScmServerException, ScmMetasourceException {
        if (ws.isEnableDirectory()) {
            MetaRelAccessor relAccessor = ScmContentModule.getInstance().getMetaService()
                    .getMetaSource().getRelAccessor(ws.getName(), context);
            String dirId = BsonUtils.getStringChecked(deletedFileRecord,
                    FieldName.FIELD_CLFILE_DIRECTORY_ID);
            String fileName = BsonUtils.getStringChecked(deletedFileRecord,
                    FieldName.FIELD_CLFILE_NAME);
            relAccessor.deleteRel(fileID, dirId, fileName);
        }
    }

    private static Object checkExistString(BSONObject obj, String fieldName)
            throws ScmServerException {
        Object value = obj.get(fieldName);
        if (value == null) {
            throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR,
                    "field[" + fieldName + "] is not exist!");
        }

        if (!(value instanceof String)) {
            throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR,
                    "field[" + fieldName + "] is not String format!");
        }

        return value;
    }

    public static BSONObject checkFileObj(ScmWorkspaceInfo ws, BSONObject fileObj)
            throws ScmServerException {
        BSONObject result = new BasicBSONObject();

        String fieldName = FieldName.FIELD_CLFILE_NAME;

        String fileName = (String) fileObj.get(fieldName);
        ScmFileOperateUtils.checkFileName(ws, fileName);
        result.put(fieldName, fileName);

        fieldName = FieldName.FIELD_CLFILE_FILE_AUTHOR;
        result.put(fieldName, fileObj.get(fieldName));

        fieldName = FieldName.FIELD_CLFILE_FILE_TITLE;
        result.put(fieldName, checkExistString(fileObj, fieldName));

        fieldName = FieldName.FIELD_CLFILE_FILE_MIME_TYPE;
        result.put(fieldName, checkExistString(fileObj, fieldName));

        fieldName = FieldName.FIELD_CLFILE_DIRECTORY_ID;
        result.put(fieldName, fileObj.get(fieldName));

        Object classId = fileObj.get(FieldName.FIELD_CLFILE_FILE_CLASS_ID);
        if (classId != null) {
            result.put(FieldName.FIELD_CLFILE_FILE_CLASS_ID, classId);
        }

        fieldName = FieldName.FIELD_CLFILE_PROPERTIES;
        BSONObject classValue = (BSONObject) fileObj.get(fieldName);
        result.put(fieldName, ScmArgumentChecker.checkAndCorrectClass(classValue, fieldName));

        fieldName = FieldName.FIELD_CLFILE_TAGS;
        BSONObject tagsValue = (BSONObject) fileObj.get(fieldName);
        result.put(fieldName, ScmArgumentChecker.checkAndCorrectTags(tagsValue, fieldName));

        fieldName = FieldName.FIELD_CLFILE_INNER_CREATE_TIME;
        Object obj = fileObj.get(fieldName);
        if (null != obj) {
            result.put(fieldName, toLongValue(fieldName, obj));
        }
        result.put(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA,
                fileObj.get(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA));

        Object customMeta = fileObj.get(FieldName.FIELD_CLFILE_CUSTOM_METADATA);
        if (customMeta != null) {
            result.put(FieldName.FIELD_CLFILE_CUSTOM_METADATA, customMeta);
        }

        result.put(FieldName.FIELD_CLFILE_FILE_ETAG,
                fileObj.get(FieldName.FIELD_CLFILE_FILE_ETAG));

        return result;
    }

    private static long toLongValue(String keyName, Object obj) throws ScmServerException {
        try {
            long l = ScmSystemUtils.toLongValue(obj);
            return l;
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.ATTRIBUTE_FORMAT_ERROR,
                    "field[" + keyName + "] is not long type:obj=" + obj, e);
        }
    }

    public static void addExtraField(ScmWorkspaceInfo ws, BSONObject obj, String fileId,
            String dataId, Date fileCreateDate, Date dataCreateDate, String userName, int siteId,
            int majorVersion, int minorVersion) throws ScmServerException {
        obj.put(FieldName.FIELD_CLFILE_ID, fileId);
        obj.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
        obj.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
        obj.put(FieldName.FIELD_CLFILE_TYPE, 1);
        obj.put(FieldName.FIELD_CLFILE_BATCH_ID, "");
        obj.put(FieldName.FIELD_CLFILE_FILE_DATA_ID, dataId);
        obj.put(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME, dataCreateDate.getTime());
        obj.put(FieldName.FIELD_CLFILE_FILE_DATA_TYPE, ENDataType.Normal.getValue());

        BSONObject sites = new BasicBSONList();
        BSONObject oneSite = new BasicBSONObject();
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID, siteId);
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TIME, dataCreateDate.getTime());
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME, dataCreateDate.getTime());

        sites.put("0", oneSite);
        obj.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST, sites);

        obj.put(FieldName.FIELD_CLFILE_INNER_USER, userName);
        obj.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, fileCreateDate.getTime());
        obj.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH,
                ScmSystemUtils.getCurrentYearMonth(fileCreateDate));
        obj.put(FieldName.FIELD_CLFILE_INNER_UPDATE_USER, userName);
        obj.put(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME, fileCreateDate.getTime());

        obj.put(FieldName.FIELD_CLFILE_EXTRA_STATUS, ServiceDefine.FileStatus.NORMAL);
        obj.put(FieldName.FIELD_CLFILE_EXTRA_TRANS_ID, "");

        String dirId = (String) obj.get(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        if (dirId == null || dirId.equals("")) {
            obj.put(FieldName.FIELD_CLFILE_DIRECTORY_ID, CommonDefine.Directory.SCM_ROOT_DIR_ID);
        }
        else if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "can not specify parent directory for file");
        }

    }

    public static void deleteBucketFileRelForDeleteFile(BucketInfoManager bucketInfoMgr,
            BSONObject deletedFileRecord, TransactionContext context)
            throws ScmMetasourceException, ScmServerException {
        Number bucketId = BsonUtils.getNumber(deletedFileRecord,
                FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        if (bucketId == null) {
            return;
        }
        ScmBucket bucket = bucketInfoMgr.getBucketById(bucketId.longValue());
        if (bucket == null) {
            logger.warn("failed to remove bucket relation, bucket not found: fileId="
                    + deletedFileRecord.get(FieldName.FIELD_CLFILE_ID) + ", bucketId=" + bucketId);
            return;
        }
        MetaAccessor accessor = bucket.getFileTableAccessor(context);
        accessor.delete(new BasicBSONObject(FieldName.BucketFile.FILE_NAME,
                deletedFileRecord.get(FieldName.FIELD_CLFILE_NAME)));

    }

    public static  ScmStatisticsFileMeta createStatisticsFileMeta(BSONObject fileInfo, String workspace,
                                                          String userName, long trafficSize, String breakpointFileName) {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        String mySiteName = contentModule.getSiteInfo(contentModule.getLocalSite()).getName();
        String mimeType = com.sequoiacm.infrastructure.common.BsonUtils.getString(fileInfo, FieldName.FIELD_CLFILE_FILE_MIME_TYPE);
        String batchId = com.sequoiacm.infrastructure.common.BsonUtils.getString(fileInfo, FieldName.FIELD_CLFILE_BATCH_ID);
        String versionStr = com.sequoiacm.infrastructure.common.BsonUtils.getInteger(fileInfo, FieldName.FIELD_CLFILE_MAJOR_VERSION)
                + "." + com.sequoiacm.infrastructure.common.BsonUtils.getInteger(fileInfo, FieldName.FIELD_CLFILE_MINOR_VERSION);
        long size = com.sequoiacm.infrastructure.common.BsonUtils.getLongChecked(fileInfo, FieldName.FIELD_CLFILE_FILE_SIZE);
        long dataCreateTime = com.sequoiacm.infrastructure.common.BsonUtils.getLongChecked(fileInfo,
                FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME);
        if (trafficSize <= -1) {
            trafficSize = size;
        }
        return new ScmStatisticsFileMeta(workspace, mySiteName, userName, mimeType, versionStr,
                batchId, size, trafficSize, dataCreateTime, breakpointFileName);

    }

}
