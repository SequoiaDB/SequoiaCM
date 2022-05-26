package com.sequoiacm.contentserver.dao;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.common.ScmArgumentChecker;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metadata.MetaDataManager;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdParser;
import com.sequoiacm.infrastructure.lock.ScmLock;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FileInfoUpdatorDao {
    private static final Logger logger = LoggerFactory.getLogger(FileInfoUpdatorDao.class);
    private ScmWorkspaceInfo ws;
    private BSONObject updator;
    private ScmMetaService metaService;
    private String fileId;
    private int majorVersion;
    private int minorVersion;
    private String user;
    private BSONObject latestFileInfo;

    public FileInfoUpdatorDao(String user, ScmWorkspaceInfo ws, String fileId, int majorVersion,
            int minorVersion, BSONObject updator) throws ScmServerException {
        this.ws = ws;
        checkUpdateInfoObj(updator);
        this.updator = updator;
        this.fileId = fileId;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.user = user;
        this.metaService = ScmContentModule.getInstance().getMetaService();
        this.latestFileInfo = metaService.getCurrentFileInfo(ws.getMetaLocation(), ws.getName(),
                fileId, false);
        if (latestFileInfo == null) {
            throw new ScmFileNotFoundException("file not exist:id=" + fileId + ",majorVersion="
                    + majorVersion + ",minorVersion=" + minorVersion);
        }
    }

    public BSONObject getFileInfoBeforeUpdate() {
        return latestFileInfo;
    }

    public BSONObject updateInfo() throws ScmServerException {
        logger.debug("updating file:wsName=" + ws.getName() + ",fileId=" + fileId + ",version="
                + ScmSystemUtils.getVersionStr(majorVersion, minorVersion) + ",new="
                + updator.toString());

        if (updator.containsField(FieldName.FIELD_CLFILE_DIRECTORY_ID)) {
            moveFile();
        }
        else if (updator.containsField(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_PATH)) {
            if (!ws.isEnableDirectory()) {
                throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                        "can not update file parent directory, directory feature is disable:ws="
                                + ws.getName() + ", fileId=" + fileId);
            }
            String moveToPath = (String) updator
                    .get(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_PATH);
            moveToPath = ScmSystemUtils.formatDirPath(moveToPath);
            BSONObject parentDir = DirOperator.getInstance().getDirByPath(ws, moveToPath);
            if (parentDir == null) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "directory not exist:path=" + moveToPath);
            }
            updator.removeField(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_PATH);
            updator.put(FieldName.FIELD_CLFILE_DIRECTORY_ID,
                    parentDir.get(FieldName.FIELD_CLDIR_ID));
            moveFile();
        }
        else if (updator.containsField(FieldName.FIELD_CLFILE_NAME)) {
            rename();
        }
        else if (updator.containsField(FieldName.FIELD_CLFILE_FILE_CLASS_ID)
                || updator.containsField(FieldName.FIELD_CLFILE_PROPERTIES)
                || MetaDataManager.getInstence().isUpdateSingleClassProperty(updator,
                        FieldName.FIELD_CLFILE_PROPERTIES)) {
            updateClassProperties();
        }
        else {
            // update other property
            updateFileInfo();
        }
        return updator;
    }

    private void updateClassProperties() throws ScmServerException, ScmInvalidArgumentException {
        String classIdKey = FieldName.FIELD_CLFILE_FILE_CLASS_ID;
        String propertiesKey = FieldName.FIELD_CLFILE_PROPERTIES;
        String classId = (String) latestFileInfo.get(classIdKey);
        MetaDataManager.getInstence().checkUpdateProperties(ws.getName(), updator, classIdKey,
                propertiesKey, classId);
        updateFileInfo();
    }

    private void updateFileInfo() throws ScmServerException {
        FileInfoUpdater fileInfoUpdater = FileInfoUpdaterFactory.create(updator);
        fileInfoUpdater.update(ws, fileId, majorVersion, minorVersion, updator,
                user);
    }

    private void rename() throws ScmServerException {
        Number bucketId = BsonUtils.getNumber(latestFileInfo,
                FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        if (bucketId != null) {
            throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                    "can not rename file because the file in bucket:ws=" + ws + ", fileId=" + fileId
                            + ", bucketId=" + bucketId);
        }
        String fileName = (String) updator.get(FieldName.FIELD_CLFILE_NAME);
        ScmFileOperateUtils.checkFileName(ws, fileName);

        if (ws.isBatchFileNameUnique()) {
            checkBatchFileNameUniqueAndUpdate(fileName);
            return;
        }

        updateFileInfo();
    }

    private void checkBatchFileNameUniqueAndUpdate(String newFileName) throws ScmServerException {
        String oldFileName = (String) latestFileInfo.get(FieldName.FIELD_CLFILE_NAME);
        if (newFileName.equals(oldFileName)) {
            updateFileInfo();
            return;
        }

        String batchId = (String) latestFileInfo.get(FieldName.FIELD_CLFILE_BATCH_ID);
        if (batchId == null || batchId.length() <= 0) {
            updateFileInfo();
            return;
        }

        ScmLockPath batchLockPath = ScmLockPathFactory.createBatchLockPath(ws.getName(), batchId);
        ScmLock batchLock = ScmLockManager.getInstance().acquiresLock(batchLockPath,
                PropertiesUtils.getServerConfig().getFileRenameBatchLockTimeout());
        if (batchLock == null) {
            throw new ScmServerException(ScmError.OPERATION_TIMEOUT,
                    "acquires batch lock timeout:ws=" + ws.getName() + ", batch=" + batchId
                            + ", file=" + fileId);
        }
        try {
            String batchCreateMonth = ScmSystemUtils.getCreateMonthFromBatchId(ws, batchId);
            BSONObject batch = metaService.getBatchInfo(ws, batchId, batchCreateMonth);
            if (batch == null) {
                updateFileInfo();
                return;
            }
            BasicBSONList files = BsonUtils.getArray(batch, FieldName.Batch.FIELD_FILES);
            if (files == null || files.size() <= 1) {
                updateFileInfo();
                return;
            }
            BasicBSONObject condition = new BasicBSONObject();
            ArrayList<String> ids = new ArrayList<>(files.size());
            for (Object file : files) {
                BSONObject fileBson = (BSONObject) file;
                ids.add(BsonUtils.getStringChecked(fileBson, FieldName.FIELD_CLFILE_ID));
            }
            BasicBSONObject dollarInFileIds = new BasicBSONObject("$in", ids);
            condition.put(FieldName.FIELD_CLFILE_ID, dollarInFileIds);

            ArrayList<String> fileCreateMonths = new ArrayList<>(files.size());
            for (String id : ids) {
                ScmIdParser idParser = new ScmIdParser(id);
                fileCreateMonths.add(idParser.getMonth());
            }
            BasicBSONObject dollarInCreateMonths = new BasicBSONObject("$in", fileCreateMonths);
            condition.put(FieldName.FIELD_CLFILE_INNER_CREATE_MONTH, dollarInCreateMonths);

            condition.put(FieldName.FIELD_CLFILE_NAME, newFileName);
            long sameNameFileCount = ScmContentModule.getInstance().getMetaService()
                    .getCurrentFileCount(ws, condition);
            if (sameNameFileCount > 0) {
                throw new ScmServerException(ScmError.BATCH_FILE_SAME_NAME,
                        "rename file failed, the batch already attach a file with same name:ws="
                                + ws.getName() + ", batch=" + batchId + ", fileName="
                                + newFileName);
            }
            updateFileInfo();
        }
        finally {
            batchLock.unlock();
        }
    }

    private void moveFile() throws ScmServerException {
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "can not update file parent directory, directory feature is disable:ws="
                            + ws.getName() + ", fileId=" + fileId);
        }
        String parentDirId = (String) updator.get(FieldName.FIELD_CLFILE_DIRECTORY_ID);

        // updator only have one properties now,
        String fileName = (String) updator.get(FieldName.FIELD_CLFILE_NAME);
        if (fileName == null) {
            fileName = (String) latestFileInfo.get(FieldName.FIELD_CLFILE_NAME);
        }

        checkExistDir(fileName, parentDirId);

        BSONObject parentDirMatcher = new BasicBSONObject();
        parentDirMatcher.put(FieldName.FIELD_CLDIR_ID, parentDirId);

        ScmLockPath lockPath = ScmLockPathFactory.createDirLockPath(ws.getName(), parentDirId);
        ScmLock rLock = null;
        if (!parentDirId.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            rLock = readLock(lockPath);
        }
        try {
            if (metaService.getDirCount(ws.getName(), parentDirMatcher) <= 0) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "parent directory not exist:id=" + parentDirId);
            }
            updateFileInfo();
        }
        finally {
            unlock(rLock, lockPath);
        }

    }

    private void unlock(ScmLock lock, ScmLockPath lockPath) {
        try {
            if (lock != null) {
                lock.unlock();
            }
        }
        catch (Exception e) {
            logger.warn("failed to unlock:path={}", lockPath, e);
        }
    }

    private ScmLock readLock(ScmLockPath lockPath) throws ScmServerException {
        return ScmLockManager.getInstance().acquiresReadLock(lockPath);
    }

    private ScmLock writeLock(ScmLockPath lockPath) throws ScmServerException {
        return ScmLockManager.getInstance().acquiresWriteLock(lockPath);
    }

    private void checkExistDir(String name, String parentDirId) throws ScmServerException {
        BSONObject existDirMatcher = new BasicBSONObject();
        existDirMatcher.put(FieldName.FIELD_CLDIR_NAME, name);
        existDirMatcher.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, parentDirId);
        long dirCount = metaService.getDirCount(ws.getName(), existDirMatcher);
        if (dirCount > 0) {
            throw new ScmServerException(ScmError.DIR_EXIST,
                    "a directory with the same name exists:name=" + name + ",parentDirectoryId="
                            + parentDirId);
        }
    }

    private void checkUpdateInfoObj(BSONObject updateInfoObj) throws ScmServerException {
        // only one properties now.
        Set<String> objFields = updateInfoObj.keySet();
        if (objFields.size() != 1) {
            if (objFields.size() != 2) {
                throw new ScmInvalidArgumentException(
                        "invlid argument,updates only one properties at a time:updator="
                                + updateInfoObj);
            }

            // key number = 2
            if (!objFields.contains(FieldName.FIELD_CLFILE_PROPERTIES)
                    || !objFields.contains(FieldName.FIELD_CLFILE_FILE_CLASS_ID)) {
                // must contain id and properties
                throw new ScmInvalidArgumentException(
                        "invlid argument,updates only classId and properties at a time:updator="
                                + updateInfoObj);
            }
        }

        Set<String> availableFields = new HashSet<>();
        availableFields.add(FieldName.FIELD_CLFILE_FILE_AUTHOR);
        availableFields.add(FieldName.FIELD_CLFILE_NAME);
        availableFields.add(FieldName.FIELD_CLFILE_FILE_TITLE);
        availableFields.add(FieldName.FIELD_CLFILE_FILE_MIME_TYPE);

        availableFields.add(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        availableFields.add(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_PATH);

        availableFields.add(FieldName.FIELD_CLFILE_FILE_CLASS_ID);
        availableFields.add(FieldName.FIELD_CLFILE_PROPERTIES);
        availableFields.add(FieldName.FIELD_CLFILE_TAGS);
        availableFields.add(FieldName.FIELD_CLFILE_CUSTOM_METADATA);

        for (String field : objFields) {
            // SEQUOIACM-312
            // {class_properties.key:value}
            if (field.startsWith(FieldName.FIELD_CLFILE_PROPERTIES + ".")) {
                String subKey = field.substring((FieldName.FIELD_CLFILE_PROPERTIES + ".").length());
                MetaDataManager.getInstence().validateKeyFormat(subKey,
                        FieldName.FIELD_CLFILE_PROPERTIES);
            }
            else if (!availableFields.contains(field)) {
                throw new ScmOperationUnsupportedException(
                        "field can't be modified:fieldName=" + field);
            }
        }

        // value type is string. and can't be null
        Set<String> valueCheckStringFields = new HashSet<>();
        valueCheckStringFields.add(FieldName.FIELD_CLFILE_FILE_AUTHOR);
        valueCheckStringFields.add(FieldName.FIELD_CLFILE_NAME);
        valueCheckStringFields.add(FieldName.FIELD_CLFILE_FILE_TITLE);
        valueCheckStringFields.add(FieldName.FIELD_CLFILE_FILE_MIME_TYPE);

        valueCheckStringFields.add(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        valueCheckStringFields.add(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_PATH);

        valueCheckStringFields.add(FieldName.FIELD_CLFILE_FILE_CLASS_ID);

        for (String field : valueCheckStringFields) {
            if (updateInfoObj.containsField(field)) {
                ScmMetaSourceHelper.checkExistString(updateInfoObj, field);
            }
        }

        // value type is bson, check the format
        String fieldName = FieldName.FIELD_CLFILE_PROPERTIES;
        if (updateInfoObj.containsField(fieldName)) {
            BSONObject classValue = (BSONObject) updateInfoObj.get(fieldName);
            updateInfoObj.put(fieldName,
                    ScmArgumentChecker.checkAndCorrectClass(classValue, fieldName));
        }
        fieldName = FieldName.FIELD_CLFILE_TAGS;
        if (updateInfoObj.containsField(fieldName)) {
            BSONObject tagsValue = (BSONObject) updateInfoObj.get(fieldName);
            updateInfoObj.put(fieldName,
                    ScmArgumentChecker.checkAndCorrectTags(tagsValue, fieldName));
        }
    }
}
