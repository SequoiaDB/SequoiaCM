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
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
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
        String newFileName = BsonUtils.getString(fileUpdater, FieldName.FIELD_CLREL_FILENAME);
        if (newFileName != null
                && !newFileName.equals(oldFileRecord.get(FieldName.FIELD_CLFILE_NAME))) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR,
                    "can not rename bucket file：" + oldFileRecord);
        }
        Number newBucketId = BsonUtils.getNumber(fileUpdater,
                FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        if (newBucketId != null && newBucketId.longValue() != bucketId.longValue()) {
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

    public static BSONObject createBucketFileRel(BSONObject fileInfo) {
        BasicBSONObject ret = new BasicBSONObject();
        ret.put(FieldName.BucketFile.FILE_ID, fileInfo.get(FieldName.FIELD_CLFILE_ID));
        ret.put(FieldName.BucketFile.FILE_NAME, fileInfo.get(FieldName.FIELD_CLFILE_NAME));
        String etag = (String) fileInfo.get(FieldName.FIELD_CLFILE_FILE_MD5);
        if (etag == null) {
            etag = (String) fileInfo.get(FieldName.FIELD_CLFILE_FILE_ETAG);
        }
        else {
            etag = SignUtil.toHex(etag);
        }
        ret.put(FieldName.BucketFile.FILE_ETAG, etag);
        ret.put(FieldName.BucketFile.FILE_CREATE_USER,
                fileInfo.get(FieldName.FIELD_CLFILE_INNER_USER));
        ret.put(FieldName.BucketFile.FILE_UPDATE_TIME,
                fileInfo.get(FieldName.FIELD_CLFILE_INNER_UPDATE_TIME));
        ret.put(FieldName.BucketFile.FILE_MIME_TYPE,
                fileInfo.get(FieldName.FIELD_CLFILE_FILE_MIME_TYPE));
        ret.put(FieldName.BucketFile.FILE_MAJOR_VERSION,
                fileInfo.get(FieldName.FIELD_CLFILE_MAJOR_VERSION));
        ret.put(FieldName.BucketFile.FILE_MINOR_VERSION,
                fileInfo.get(FieldName.FIELD_CLFILE_MINOR_VERSION));
        ret.put(FieldName.BucketFile.FILE_SIZE, fileInfo.get(FieldName.FIELD_CLFILE_FILE_SIZE));
        ret.put(FieldName.BucketFile.FILE_CREATE_TIME,
                fileInfo.get(FieldName.FIELD_CLFILE_INNER_CREATE_TIME));
        ret.put(FieldName.BucketFile.FILE_DELETE_MARKER,
                BsonUtils.getBooleanOrElse(fileInfo, FieldName.FIELD_CLFILE_DELETE_MARKER, false));

        String versionSerial = BsonUtils.getString(fileInfo, FieldName.FIELD_CLFILE_VERSION_SERIAL);
        if (versionSerial != null) {
            ret.put(FieldName.BucketFile.FILE_VERSION_SERIAL, versionSerial);
        }
        return ret;
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

    public static void addDataInfo(BSONObject formatFileObject, String dataId, Date dataCreateTime,
            int siteId, long size, String md5, int wsVersion) {
        addDataInfo(formatFileObject, dataId, dataCreateTime, siteId, size, md5, null, wsVersion);
    }

    public static void addDataInfo(BSONObject formatFileObject, String dataId, Date dataCreateTime,
            int siteId, long size, String md5, String etag, int wsVersion) {
        formatFileObject.put(FieldName.FIELD_CLFILE_FILE_DATA_ID, dataId);
        formatFileObject.put(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME,
                dataCreateTime.getTime());
        formatFileObject.put(FieldName.FIELD_CLFILE_FILE_SIZE, size);
        formatFileObject.put(FieldName.FIELD_CLFILE_FILE_MD5, md5);
        if (etag == null && md5 != null && md5.length() > 0) {
            formatFileObject.put(FieldName.FIELD_CLFILE_FILE_ETAG, SignUtil.toHex(md5));
        }
        else {
            formatFileObject.put(FieldName.FIELD_CLFILE_FILE_ETAG, etag);
        }

        BSONObject oneSite = new BasicBSONObject();
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID, siteId);
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TIME, dataCreateTime.getTime());
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME, dataCreateTime.getTime());
        oneSite.put(FieldName.FIELD_CLFILE_FILE_SITE_LIST_WS_VERSION, wsVersion);
        BSONObject sites = BsonUtils.getBSONChecked(formatFileObject,
                FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        sites.put("0", oneSite);
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

    public static ScmStatisticsFileMeta createStatisticsFileMeta(FileMeta fileInfo,
            String workspace, String userName, long trafficSize, String breakpointFileName) {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        String mySiteName = contentModule.getSiteInfo(contentModule.getLocalSite()).getName();
        String versionStr = fileInfo.getMajorVersion() + "." + fileInfo.getMinorVersion();
        if (trafficSize <= -1) {
            trafficSize = fileInfo.getSize();
        }
        return new ScmStatisticsFileMeta(workspace, mySiteName, userName, fileInfo.getMimeType(),
                versionStr, fileInfo.getBatchId(), fileInfo.getSize(), trafficSize,
                fileInfo.getDataCreateTime(), breakpointFileName);

    }

}
