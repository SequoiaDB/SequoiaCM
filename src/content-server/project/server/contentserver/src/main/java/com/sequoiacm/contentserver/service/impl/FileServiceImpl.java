package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.*;
import com.sequoiacm.contentserver.common.*;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.dao.*;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.job.ScmJobCacheFile;
import com.sequoiacm.contentserver.job.ScmJobManager;
import com.sequoiacm.contentserver.job.ScmJobTransferFile;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.listener.OperationCompleteCallback;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.ClientUploadConf;
import com.sequoiacm.contentserver.model.ScmDataInfoDetail;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.service.IBatchService;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserPasswordType;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.audit.ScmUserAuditType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.monitor.FlowRecorder;
import com.sequoiacm.infrastructure.strategy.element.SiteInfo;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.TransactionContext;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class FileServiceImpl implements IFileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private FileOperationListenerMgr listenerMgr;

    @Autowired
    private IDirService dirService;

    @Autowired
    private IBatchService batchService;

    @Autowired
    private ScmAudit audit;

    @Override
    public BSONObject getFileInfoById(ScmUser user, String workspaceName, String fileId,
            int majorVersion, int minorVersion) throws ScmServerException {
        BSONObject fileInfo = getFileInfoById(workspaceName, fileId, majorVersion, minorVersion);
        ScmFileServicePriv.getInstance().checkDirPriorityById(user, workspaceName, dirService,
                (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID), ScmPrivilegeDefine.READ,
                "get file by id");
        audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0, "get file info by file id="
                + fileId + ", fileName=" + (String) fileInfo.get(FieldName.FIELD_CLFILE_NAME));
        return fileInfo;
    }

    @Override
    public BSONObject getFileInfoById(String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoChecked(workspaceName);
        BSONObject fileInfo = contentModule.getMetaService().getFileInfo(ws.getMetaLocation(),
                workspaceName, fileId, majorVersion, minorVersion);
        if (fileInfo == null) {
            throw new ScmFileNotFoundException(
                    "file not exist:workspace=" + workspaceName + ",fileId=" + fileId + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }
        return fileInfo;
    }

    @Override
    public BSONObject getFileInfoByPath(ScmUser user, String workspaceName, String filePath,
            int majorVersion, int minorVersion) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriority(user, workspaceName, filePath,
                ScmPrivilegeDefine.READ, "get file by path");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoChecked(workspaceName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to get file, directory is disable:ws=" + workspaceName + ", filePath="
                            + filePath);
        }
        String fileName = ScmSystemUtils.basename(filePath);
        String parentDirPath = ScmSystemUtils.dirname(filePath);
        BSONObject fileInfo = null;
        try {
            BSONObject parentDir = dirService.getDirInfoByPath(workspaceName, parentDirPath);
            String parentDirId = (String) parentDir.get(FieldName.FIELD_CLDIR_ID);
            fileInfo = contentModule.getMetaService().getFileInfo(ws, parentDirId, fileName,
                    majorVersion, minorVersion);
        }
        catch (ScmServerException e) {
            // DIR_NOT_FOUND ==> FILE_NOT_FOUND
            if (e.getError() != ScmError.DIR_NOT_FOUND) {
                throw e;
            }
        }
        if (fileInfo == null) {
            throw new ScmFileNotFoundException("file not exist:workspace=" + workspaceName
                    + ",filePath=" + filePath + ",version="
                    + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }

        audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0, "get file by file path="
                + filePath + ", fileName=" + (String) fileInfo.get(FieldName.FIELD_CLFILE_NAME));
        return fileInfo;
    }

    @Override
    public MetaCursor getFileList(String workspaceName, BSONObject condition, int scope,
            BSONObject orderby, long skip, long limit, BSONObject selector)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoChecked(workspaceName);

        MetaCursor metaCursor = null;
        try {
            if (selector == null) {
                selector = new BasicBSONObject();
                selector.put(FieldName.FIELD_CLFILE_ID, null);
                selector.put(FieldName.FIELD_CLFILE_NAME, null);
                selector.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, null);
                selector.put(FieldName.FIELD_CLFILE_MINOR_VERSION, null);
                selector.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, null);
                selector.put(FieldName.FIELD_CLFILE_INNER_USER, null);
                selector.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, null);
            }

            if (scope == CommonDefine.Scope.SCOPE_CURRENT) {
                metaCursor = contentModule.getMetaService().queryCurrentFile(ws, condition,
                        selector, orderby, skip, limit);
            }
            else {
                ScmArgChecker.File.checkHistoryFileMatcher(condition);
                if (scope == CommonDefine.Scope.SCOPE_HISTORY) {
                    ScmArgChecker.File.checkHistoryFileOrderby(orderby);
                    metaCursor = contentModule.getMetaService().queryHistoryFile(
                            ws.getMetaLocation(), workspaceName, condition, selector, orderby, skip,
                            limit);
                }
                else if (scope == CommonDefine.Scope.SCOPE_ALL) {
                    if (orderby != null || skip != 0 || limit != -1) {
                        throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                                "query all file unsupport orderby/skip/limit");
                    }
                    metaCursor = contentModule.getMetaService().queryAllFile(ws, condition,
                            selector);
                }
                else {
                    throw new ScmInvalidArgumentException("unknown scope:scope=" + scope);
                }
            }
        }
        catch (InvalidArgumentException e) {
            throw new ScmInvalidArgumentException("Invalid condition: " + condition, e);
        }
        return metaCursor;
    }

    @Override
    public MetaCursor getFileList(ScmUser user, String workspaceName, BSONObject condition,
            int scope, BSONObject orderby, long skip, long limit, BSONObject selector)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.LOW_LEVEL_READ, "list files");
        MetaCursor ret = getFileList(workspaceName, condition, scope, orderby, skip, limit,
                selector);
        String message = "list file ";
        if (null != condition) {
            message += "by condition=" + condition.toString();
        }
        audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0, message);
        return ret;
    }

    @Override
    public MetaCursor getDirSubFileList(ScmUser user, String workspaceName, String dirId,
            BSONObject condition, int scope, BSONObject orderby, long skip, long limit,
            BSONObject selector) throws ScmServerException {

        ScmFileServicePriv.getInstance().checkDirPriorityById(user, workspaceName, dirService,
                dirId, ScmPrivilegeDefine.READ, "list directory's files");

        BSONObject matcher = new BasicBSONObject(FieldName.FIELD_CLFILE_DIRECTORY_ID, dirId);
        if (condition != null) {
            BasicBSONList arrayCond = new BasicBSONList();
            arrayCond.add(condition);
            arrayCond.add(matcher);

            matcher = new BasicBSONObject();
            matcher.put("$and", arrayCond);
        }

        audit.info(ScmAuditType.DIR_DQL, user, workspaceName, 0,
                "list directory's files, directoryId=" + dirId + ", matcher" + matcher.toString());
        return getFileList(workspaceName, matcher, CommonDefine.Scope.SCOPE_CURRENT, orderby, skip,
                limit, selector);
    }

    // when overwrite file, may be to forward request of delete file.
    @Override
    public BSONObject uploadFile(ScmUser user, String workspaceName, InputStream is,
            BSONObject fileInfo, String sessionId, String userDetail,
            ScmUserPasswordType passwordType, ClientUploadConf uploadConf)
            throws ScmServerException {

        if (uploadConf.isOverwrite()) {
            ScmFileServicePriv.getInstance().checkDirPriorityById(user, workspaceName, dirService,
                    (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID),
                    ScmPrivilegeDefine.CREATE.getFlag() | ScmPrivilegeDefine.DELETE.getFlag(),
                    "overwrite file for delete and create");
            ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                    ScmPrivilegeDefine.UPDATE, "overwrite file for detach batch");
        }
        else {
            // create file priv
            ScmFileServicePriv.getInstance().checkDirPriorityById(user, workspaceName, dirService,
                    (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID),
                    ScmPrivilegeDefine.CREATE, "create file");
        }
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoChecked(workspaceName);

        BSONObject checkedFileObj = checkFileObj(fileInfo);

        Date fileCreateDate = new Date();
        String fileId = ScmIdGenerator.FileId.get(fileCreateDate);

        if (null != checkedFileObj.get(FieldName.FIELD_CLFILE_INNER_CREATE_TIME)) {
            // reset to user's create time
            fileCreateDate = new Date(
                    (long) checkedFileObj.get(FieldName.FIELD_CLFILE_INNER_CREATE_TIME));
            fileId = ScmIdGenerator.FileId.get(fileCreateDate);
        }

        String dataId = fileId;
        Date dataCreateDate = fileCreateDate;
        addExtraField(wsInfo, checkedFileObj, fileId, dataId, fileCreateDate, dataCreateDate,
                user.getUsername(), contentModule.getLocalSite(), 1, 0);

        // checkDirWithSameNameExist(checkedFileObj, workspaceName);

        ScmDataInfo dataInfo = new ScmDataInfo(ENDataType.Normal.getValue(), dataId,
                dataCreateDate);
        listenerMgr.preCreate(wsInfo, checkedFileObj);
        FileCreatorDao dao = new FileCreatorDao(contentModule.getLocalSite(), wsInfo,
                checkedFileObj, dataInfo, uploadConf.isNeedMd5());
        BSONObject finfo = null;
        try {
            dao.write(is);
            finfo = insertFileInfo(wsInfo, dao, checkedFileObj, uploadConf, sessionId, user,
                    passwordType, userDetail);
        }
        catch (ScmServerException e) {
            dao.rollback();
            throw e;
        }
        catch (Exception e) {
            dao.rollback();
            throw e;
        }
        long uploadSize = CommonHelper.toLongValue(finfo.get(FieldName.FIELD_CLFILE_FILE_SIZE));
        try {
            FlowRecorder.getInstance().addUploadSize(workspaceName, uploadSize);
        }
        catch (Exception e) {
            logger.error("add flow record failed", e);
        }

        listenerMgr.postCreate(wsInfo, fileId).onComplete();

        audit.info(ScmAuditType.CREATE_FILE, user, workspaceName, 0,
                "create file , file id=" + finfo.get(FieldName.FIELD_CLFILE_ID) + ", file name="
                        + String.valueOf(fileInfo.get(FieldName.FIELD_CLFILE_NAME)));
        return finfo;
    }

    @Override
    public BSONObject uploadFile(ScmUser user, String workspaceName, String breakpointFileName,
            BSONObject fileInfo, String sessionId, String userDetail,
            ScmUserPasswordType passwordType, ClientUploadConf uploadConf)
            throws ScmServerException {

        if (uploadConf.isOverwrite()) {
            ScmFileServicePriv.getInstance().checkDirPriorityById(user, workspaceName, dirService,
                    (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID),
                    ScmPrivilegeDefine.CREATE.getFlag() | ScmPrivilegeDefine.DELETE.getFlag(),
                    "overwrite file for delete and create");
            ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                    ScmPrivilegeDefine.UPDATE, "overwrite file for detach batch");
        }
        else {
            // create file priv
            ScmFileServicePriv.getInstance().checkDirPriorityById(user, workspaceName, dirService,
                    (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID),
                    ScmPrivilegeDefine.CREATE, "create file");
        }

        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoChecked(workspaceName);
        BSONObject ret;
        OperationCompleteCallback callback;
        String fileId;
        ScmLock lock = lockBreakpointFile(workspaceName, breakpointFileName);
        try {
            BreakpointFile breakpointFile = contentModule.getMetaService()
                    .getBreakpointFile(workspaceName, breakpointFileName);
            if (breakpointFile == null) {
                throw new ScmInvalidArgumentException(String.format(
                        "BreakpointFile is not found: /%s/%s", workspaceName, breakpointFileName));
            }

            if (breakpointFile.getSiteId() != contentModule.getLocalSite()) {
                throw new ScmInvalidArgumentException(
                        String.format("BreakpointFile[/%s/%s] should be uploaded in site[%s]",
                                workspaceName, breakpointFileName, breakpointFile.getSiteName()));
            }

            if (!breakpointFile.isCompleted()) {
                throw new ScmInvalidArgumentException(String.format(
                        "Uncompleted BreakpointFile: /%s/%s", workspaceName, breakpointFileName));
            }

            if (uploadConf.isNeedMd5() && !breakpointFile.isNeedMd5()) {
                throw new ScmInvalidArgumentException(String.format(
                        "BreakpointFile has no md5: /%s/%s", workspaceName, breakpointFileName));
            }

            if (!fileInfo.containsField(FieldName.FIELD_CLFILE_NAME)) {
                fileInfo.put(FieldName.FIELD_CLFILE_NAME, breakpointFileName);
            }

            BSONObject checkedFileObj = checkFileObj(fileInfo);
            Date dataCreateDate = new Date(breakpointFile.getCreateTime());
            String dataId = breakpointFile.getDataId();

            Date fileCreateDate = new Date();
            fileId = ScmIdGenerator.FileId.get(fileCreateDate);
            if (null != checkedFileObj.get(FieldName.FIELD_CLFILE_INNER_CREATE_TIME)) {
                // reset to user's create time
                fileCreateDate = new Date(
                        (long) checkedFileObj.get(FieldName.FIELD_CLFILE_INNER_CREATE_TIME));
                fileId = ScmIdGenerator.FileId.get(fileCreateDate);
            }

            addExtraField(wsInfo, checkedFileObj, fileId, dataId, fileCreateDate, dataCreateDate,
                    user.getUsername(), contentModule.getLocalSite(), 1, 0);

            checkedFileObj.put(FieldName.FIELD_CLFILE_FILE_SIZE, breakpointFile.getUploadSize());
            checkedFileObj.put(FieldName.FIELD_CLFILE_FILE_DATA_TYPE, ENDataType.Normal.getValue());

            if (breakpointFile.getMd5() != null) {
                checkedFileObj.put(FieldName.FIELD_CLFILE_FILE_MD5, breakpointFile.getMd5());
            }

            listenerMgr.preCreate(wsInfo, checkedFileObj);
            IFileCreatorDao fileDao = new BreakpointFileConvertor(wsInfo, breakpointFile,
                    checkedFileObj);
            ret = insertFileInfo(wsInfo, fileDao, checkedFileObj, uploadConf, sessionId, user,
                    passwordType, userDetail);
            callback = listenerMgr.postCreate(wsInfo, fileId);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            lock.unlock();
        }
        callback.onComplete();

        audit.info(ScmAuditType.CREATE_FILE, user, workspaceName, 0,
                "create breakpointFile , file id=" + fileInfo.get(FieldName.FIELD_CLFILE_ID)
                        + ", breakpointFileName=" + breakpointFileName);
        return ret;
    }

    private BSONObject insertFileInfo(ScmWorkspaceInfo ws, IFileCreatorDao fileDao,
            BSONObject fileInfo, ClientUploadConf uploadConf, String sessionId, ScmUser user,
            ScmUserPasswordType passwordType, String userDetail) throws ScmServerException {
        try {
            return fileDao.insert();
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_EXIST && uploadConf.isOverwrite()) {
                String parentDirId = (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID);
                String fileName = (String) fileInfo.get(FieldName.FIELD_CLFILE_NAME);
                ScmContentModule contentService = ScmContentModule.getInstance();
                String existFileId = contentService.getMetaService().getFileId(ws, parentDirId,
                        fileName);
                if (existFileId != null) {
                    deleteCurrentFile(sessionId, fileDao.getWorkspaceName(), user, passwordType,
                            userDetail, existFileId);
                }
                return fileDao.insert();

            }
            if (e.getError() != ScmError.COMMIT_UNCERTAIN_STATE) {
                // rollback lob
                fileDao.processException();
            }
            throw e;
        }
        catch (Exception e) {
            fileDao.processException();
            throw e;
        }
    }

    private void deleteCurrentFile(String sessionId, String workspaceName, ScmUser user,
            ScmUserPasswordType passwordType, String userDetail, String existFileId)
            throws ScmServerException {
        try {
            ScmContentModule contentService = ScmContentModule.getInstance();
            ScmWorkspaceInfo wsInfo = contentService.getWorkspaceInfoChecked(workspaceName);
            BSONObject existFileInfo = contentService.getMetaService()
                    .getCurrentFileInfo(wsInfo.getMetaLocation(), workspaceName, existFileId);
            if (existFileInfo == null) {
                return;
            }

            String batchId = (String) existFileInfo.get(FieldName.FIELD_CLFILE_BATCH_ID);
            if (batchId != null && !batchId.isEmpty()) {
                try {
                    batchService.detachFile(user, workspaceName, batchId, existFileId);
                    audit.info(ScmAuditType.UPDATE_BATCH,
                            ScmUserAuditType.getScmUserAuditType(passwordType.toString()),
                            user.getUsername(), workspaceName, 0,
                            "overwrite file, detach batch's file batchId=" + batchId + ", fileId="
                                    + existFileId);
                }
                catch (ScmServerException e) {
                    if (e.getError() != ScmError.BATCH_NOT_FOUND
                            && e.getError() != ScmError.FILE_NOT_IN_BATCH) {
                        throw e;
                    }
                }
            }

            ScmFileServicePriv.getInstance().checkDirPriorityByFileId(user, workspaceName, this,
                    existFileId, -1, -1, dirService, ScmPrivilegeDefine.DELETE,
                    "overwrite file,delete old file");
            deleteFile(sessionId, userDetail, workspaceName, existFileId, -1, -1, true);
            audit.info(ScmAuditType.DELETE_FILE, user, workspaceName, 0,
                    "overwrite file, delete old file by file id=" + existFileId);
        }
        catch (ScmServerException e) {
            if (e.getError() != ScmError.FILE_NOT_FOUND) {
                throw e;
            }
        }
    }

    @Override
    public FileReaderDao downloadFile(String sessionId, String userDetail, String workspaceName,
            BSONObject fileInfo, int readFlag) throws ScmServerException {
        return new FileReaderDao(sessionId, userDetail,
                ScmContentModule.getInstance().getWorkspaceInfoChecked(workspaceName), fileInfo,
                readFlag);
    }

    @Override
    public FileReaderDao downloadFile(String sessionId, String userDetail, ScmUser user,
            String workspaceName, BSONObject fileInfo, int readFlag) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriorityById(user, workspaceName, dirService,
                (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID), ScmPrivilegeDefine.READ,
                "read file");
        FileReaderDao dao = downloadFile(sessionId, userDetail, workspaceName, fileInfo, readFlag);
        audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0,
                "read file, file id=" + fileInfo.get(FieldName.FIELD_CLFILE_ID) + ", fileName="
                        + String.valueOf(fileInfo.get(FieldName.FIELD_CLFILE_NAME)));
        return dao;
    }

    public void deleteFile(String sessionid, String userDetail, String workspaceName, String fileId,
            int majorVersion, int minorVersion, boolean isPhysical) throws ScmServerException {
        try {
            FileDeletorDao dao = new FileDeletorDao();
            ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                    .getWorkspaceInfoChecked(workspaceName);
            dao.init(sessionid, userDetail, wsInfo, fileId, majorVersion, minorVersion, isPhysical,
                    listenerMgr);
            dao.delete();
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void deleteFile(String sessionid, String userDetail, ScmUser user, String workspaceName,
            String fileId, int majorVersion, int minorVersion, boolean isPhysical)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriorityByFileId(user, workspaceName, this, fileId,
                majorVersion, minorVersion, dirService, ScmPrivilegeDefine.DELETE, "delete file");
        deleteFile(sessionid, userDetail, workspaceName, fileId, majorVersion, minorVersion,
                isPhysical);
        audit.info(ScmAuditType.DELETE_FILE, user, workspaceName, 0,
                "delete file by file id=" + fileId);
    }

    public long countFiles(ScmUser user, String workspaceName, int scope, BSONObject condition)
            throws ScmServerException {

        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.LOW_LEVEL_READ, "count file");
        long ret = countFiles(workspaceName, scope, condition);
        String message = "count file";
        if (null != condition) {
            message += " by condition=" + condition.toString();
        }
        audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0, message);
        return ret;
    }

    @Override
    public long countFiles(String workspaceName, int scope, BSONObject condition)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo =contentModule.getWorkspaceInfoChecked(workspaceName);

        if (scope == CommonDefine.Scope.SCOPE_CURRENT) {
            return contentModule.getMetaService().getCurrentFileCount(wsInfo, condition);
        }

        try {
            ScmArgChecker.File.checkHistoryFileMatcher(condition);
        }
        catch (InvalidArgumentException e) {
            throw new ScmInvalidArgumentException("Invalid condition: " + condition, e);
        }
        if (scope == CommonDefine.Scope.SCOPE_HISTORY) {
            return contentModule.getMetaService().getHistoryFileCount(wsInfo.getMetaLocation(),
                    workspaceName, condition);
        }
        if (scope == CommonDefine.Scope.SCOPE_ALL) {
            return contentModule.getMetaService().getAllFileCount(wsInfo.getMetaLocation(),
                    workspaceName, condition);
        }
        throw new ScmInvalidArgumentException("unknown scope:scope=" + scope);
    }

    @Override
    public long sumFileSizes(String workspaceName, int scope, BSONObject condition)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo =contentModule.getWorkspaceInfoChecked(workspaceName);

        if (scope == CommonDefine.Scope.SCOPE_CURRENT) {
            return contentModule.getMetaService().getCurrentFileSizeSum(wsInfo, condition);
        }

        try {
            ScmArgChecker.File.checkHistoryFileMatcher(condition);
        }
        catch (InvalidArgumentException e) {
            throw new ScmInvalidArgumentException("Invalid condition: " + condition, e);
        }
        if (scope == CommonDefine.Scope.SCOPE_HISTORY) {
            return contentModule.getMetaService().getHistoryFileSizeSum(wsInfo.getMetaLocation(),
                    workspaceName, condition);
        }
        if (scope == CommonDefine.Scope.SCOPE_ALL) {
            return contentModule.getMetaService().getAllFileSizeSum(wsInfo.getMetaLocation(),
                    workspaceName, condition);
        }
        throw new ScmInvalidArgumentException("unknown scope:scope=" + scope);
    }

    @Override
    public BSONObject updateFileInfo(ScmUser user, String workspaceName, String fileId,
            BSONObject fileInfo, int majorVersion, int minorVersion) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriorityByFileId(user, workspaceName, this, fileId,
                majorVersion, minorVersion, dirService, ScmPrivilegeDefine.UPDATE,
                "update file by id");
        BSONObject ret;
        OperationCompleteCallback callback = null;
        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoChecked(workspaceName);
        ScmLockPath lockPath = ScmLockPathFactory.createFileLockPath(ws.getName(), fileId);
        ScmLock writeLock = ScmLockManager.getInstance().acquiresWriteLock(lockPath);
        try {
            FileInfoUpdatorDao dao = new FileInfoUpdatorDao(user.getUsername(), ws, fileId,
                    majorVersion, minorVersion, fileInfo);
            ret = dao.updateInfo();
            callback = listenerMgr.postUpdate(ws, dao.getFileInfoBeforeUpdate());
        }
        finally {
            writeLock.unlock();
        }
        callback.onComplete();

        audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                "update file by file id=" + fileId);
        return ret;
    }

    @Override
    public void asyncTransferFile(ScmUser user, String workspaceName, String fileId,
            int majorVersion, int minorVersion, String userTargetSite) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriorityByFileId(user, workspaceName, this, fileId,
                majorVersion, minorVersion, dirService, ScmPrivilegeDefine.UPDATE,
                "async transfer file");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo =contentModule.getWorkspaceInfoChecked(workspaceName);
        int localSiteId =contentModule.getLocalSite();

        // 1. check file
        BSONObject file = ScmContentModule.getInstance().getMetaService().getFileInfo(
                wsInfo.getMetaLocation(), workspaceName, fileId, majorVersion, minorVersion);
        if (null == file) {
            throw new ScmFileNotFoundException("file is not exist:fileId=" + fileId + ",version="
                    + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }

        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<ScmFileLocation> siteList = new ArrayList<>();
        CommonHelper.getFileLocationList(sites, siteList);

        // 2. get transfer target site
        int transferTargetSiteId;
        if (null != userTargetSite) {
            // get target site id
            ScmSite siteInfo =contentModule.getSiteInfo(userTargetSite);
            if (null == siteInfo) {
                throw new ScmServerException(ScmError.SITE_NOT_EXIST,
                        "target site not exist in workspace:workspace=" + workspaceName
                                + ",targetSite=" + userTargetSite);
            }
            transferTargetSiteId = siteInfo.getId();
        }
        else {
            transferTargetSiteId = ScmStrategyMgr.getInstance()
                    .getDefaultAsyncTransferTargetSite(wsInfo, localSiteId);
        }
        ScmStrategyMgr.getInstance().checkTransferSite(wsInfo, localSiteId, transferTargetSiteId);

        // 3.check local
        if (!CommonHelper.isSiteExist(localSiteId, siteList)) {
            // local site is not exist
            throw new ScmServerException(ScmError.DATA_NOT_EXIST,
                    "file is not exist in local site:fileId=" + fileId + ",localSiteId="
                            + localSiteId + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }

        // 4. check remote
        if (CommonHelper.isSiteExist(transferTargetSiteId, siteList)) {
            // remote site is exist. just response and return
            audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                    "async transfer file by file id=" + fileId);
            return;
        }

        String dataId = (String) file.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        ScmJobTransferFile job = new ScmJobTransferFile(wsInfo, fileId, majorVersion, minorVersion,
                dataId, transferTargetSiteId);
        ScmJobManager.getInstance().schedule(job, 0);
        audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                "async transfer file by file id=" + fileId);
    }

    @Override
    public void asyncCacheFile(ScmUser user, String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriorityByFileId(user, workspaceName, this, fileId,
                majorVersion, minorVersion, dirService, ScmPrivilegeDefine.UPDATE,
                "async cache file");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo =contentModule.getWorkspaceInfoChecked(workspaceName);

        // 1. check cache site
        int localSiteId =contentModule.getLocalSite();
        ScmStrategyMgr.getInstance().checkCacheSite(wsInfo, localSiteId);

        // 2. check file
        BSONObject file = ScmContentModule.getInstance().getMetaService().getFileInfo(
                wsInfo.getMetaLocation(), workspaceName, fileId, majorVersion, minorVersion);
        if (null == file) {
            throw new ScmFileNotFoundException("file is not exist:fileId=" + fileId);
        }

        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<ScmFileLocation> siteList = new ArrayList<>();
        CommonHelper.getFileLocationList(sites, siteList);
        List<Integer> siteIdList = CommonHelper.getFileLocationIdList(siteList);

        // 3. check local
        if (CommonHelper.isSiteExist(localSiteId, siteList)) {
            // local site is already exist. just return.
            audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                    "async cache file by file id=" + fileId);
            return;
        }

        // 4. get remote site
        int remoteSiteId = ScmStrategyMgr.getInstance().getAsyncCacheRemoteSite(wsInfo, siteIdList,
                localSiteId, fileId);

        String dataId = (String) file.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        ScmJobCacheFile job = new ScmJobCacheFile(wsInfo, fileId, majorVersion, minorVersion,
                dataId, remoteSiteId);
        ScmJobManager.getInstance().schedule(job, 0);
        audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                "async cache file by file id=" + fileId);
    }

    private static ScmLock lockBreakpointFile(String workspaceName, String breakpointFileName)
            throws ScmServerException {
        ScmLockPath lockPath = ScmLockPathFactory.createBPLockPath(workspaceName,
                breakpointFileName);
        return ScmLockManager.getInstance().acquiresLock(lockPath);
    }

    private BSONObject checkFileObj(BSONObject fileObj) throws ScmServerException {
        BSONObject result = new BasicBSONObject();

        String fieldName = FieldName.FIELD_CLFILE_NAME;

        String fileName = (String) fileObj.get(fieldName);
        if (!ScmArgChecker.File.checkFileName(fileName)) {
            throw new ScmInvalidArgumentException("invalid arg:fileName=" + fileName);
        }
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

    private static void addExtraField(ScmWorkspaceInfo ws, BSONObject obj, String fileId,
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

    // private static void checkDirWithSameNameExist(BSONObject fileInfo, String
    // workspaceName)
    // throws ScmServerException {
    // String fileName = (String) fileInfo.get(FieldName.FIELD_CLFILE_NAME);
    // String parentID = (String)
    // fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID);
    // ScmMetaService metaService =
    // ScmContentServer.getInstance().getMetaService();
    //
    // BSONObject existDirMatcher = new BasicBSONObject();
    // existDirMatcher.put(FieldName.FIELD_CLDIR_NAME, fileName);
    // existDirMatcher.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, parentID);
    // if (metaService.getDirCount(workspaceName, existDirMatcher) > 0) {
    // throw new ScmServerException(ScmError.DIR_EXIST,
    // "a directory with the same name already exists:name=" + fileName
    // + ",parentDirectoryId=" + parentID);
    // }
    // }

    @Override
    public BSONObject updateFileContent(ScmUser user, String workspaceName, String fileId,
            InputStream newFileContent, int majorVersion, int minorVersion,
            ScmUpdateContentOption option) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriorityByFileId(user, workspaceName, this, fileId,
                majorVersion, minorVersion, dirService, ScmPrivilegeDefine.UPDATE,
                "update file by id");
        FileContentUpdateDao dao = new FileContentUpdateDao(user.getUsername(), workspaceName,
                fileId, majorVersion, minorVersion, option, listenerMgr);
        BSONObject ret = dao.updateContent(newFileContent);
        audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                "update file by file id=" + fileId);
        return ret;
    }

    @Override
    public BSONObject updateFileContent(ScmUser user, String workspaceName, String fileId,
            String newBreakpointFileContent, int majorVersion, int minorVersion,
            ScmUpdateContentOption option) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriorityByFileId(user, workspaceName, this, fileId,
                majorVersion, minorVersion, dirService, ScmPrivilegeDefine.UPDATE,
                "update file by id");
        FileContentUpdateDao dao = new FileContentUpdateDao(user.getUsername(), workspaceName,
                fileId, majorVersion, minorVersion, option, listenerMgr);
        BSONObject ret = dao.updateContent(newBreakpointFileContent);
        audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                "update file by file id=" + fileId);
        return ret;
    }

    @Override
    public String calcFileMd5(String sessionid, String userDetail, ScmUser user,
            String workspaceName, String fileId, int majorVersion, int minorVersion)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriorityByFileId(user, workspaceName, this, fileId,
                majorVersion, minorVersion, dirService, ScmPrivilegeDefine.UPDATE,
                "calculate file md5");
        BSONObject fileInfo = getFileInfoById(workspaceName, fileId, majorVersion, minorVersion);
        BasicBSONList siteBson = BsonUtils.getArray(fileInfo,
                FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<ScmFileLocation> siteList = new ArrayList<>();
        CommonHelper.getFileLocationList(siteBson, siteList);

        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoChecked(workspaceName);

        String md5 = null;
        if (CommonHelper.isSiteExist(contentModule.getLocalSite(), siteList)) {
            // 在本地读取数据计算MD5
            ScmDataInfo dataInfo = new ScmDataInfo(fileInfo);
            md5 = ScmSystemUtils.calcMd5(ws, dataInfo);
            contentModule.getMetaService().updateFileMd5(ws, fileId, majorVersion, minorVersion,
                    md5);
        }
        else {
            // 发给远程站点，让远程站点执行计算或再转发
            List<Integer> siteIdList = CommonHelper.getFileLocationIdList(siteList);
            SiteInfo siteInfo = ScmStrategyMgr.getInstance().getNearestSite(ws, siteIdList,
                    contentModule.getLocalSite(), fileId);
            String remoteSite = contentModule.getSiteInfo(siteInfo.getId()).getName();
            ContentServerClient client = ContentServerClientFactory
                    .getFeignClientByServiceName(remoteSite);
            BSONObject resp = client.calcMd5(sessionid, userDetail, ws.getName(), fileId,
                    majorVersion, minorVersion);
            md5 = BsonUtils.getStringChecked(resp, FieldName.FIELD_CLFILE_FILE_MD5);
        }
        audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                "calculate file md5, id=" + fileId);
        return md5;
    }

    @Override
    public String generateId(Date fileCreateTime) {
        return ScmIdGenerator.FileId.get(fileCreateTime);
    }

    @Override
    public BSONObject createFileMeta(String ws, ScmUser user, BSONObject fileInfo,
            ScmDataInfoDetail dataInfoDetail, TransactionCallback transactionCallback)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfo(ws);
        BSONObject checkedFileObj = checkFileObj(fileInfo);

        Date fileCreateDate = dataInfoDetail.getDataInfo().getCreateTime();
        String fileId = dataInfoDetail.getDataInfo().getId();

        addExtraField(wsInfo, checkedFileObj, fileId, fileId, fileCreateDate, fileCreateDate,
                user.getUsername(), dataInfoDetail.getSiteId(), 1, 0);

        checkedFileObj.put(FieldName.FIELD_CLFILE_FILE_SIZE, dataInfoDetail.getSize());
        checkedFileObj.put(FieldName.FIELD_CLFILE_FILE_DATA_TYPE,
                dataInfoDetail.getDataInfo().getType());

        listenerMgr.preCreate(wsInfo, checkedFileObj);

        String parentId = (String) checkedFileObj.get(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        ScmMetaService metaservice = ScmContentModule.getInstance().getMetaService();

        OperationCompleteCallback listenerCallback;
        ScmLock rLock = ScmFileOperateUtils.lockDirForCreateFile(wsInfo, parentId);
        try {
            ScmFileOperateUtils.checkDirForCreateFile(wsInfo, parentId);
            metaservice.insertFile(wsInfo, checkedFileObj, transactionCallback);
            listenerCallback = listenerMgr.postCreate(wsInfo, fileId);
        }
        finally {
            if (rLock != null) {
                rLock.unlock();
            }
        }
        listenerCallback.onComplete();
        return checkedFileObj;
    }

    @Override
    public boolean updateFileExternalData(String workspaceName, String fileId, int majorVersion,
            int minorVersion, BSONObject externalData, TransactionContext transactionContext)
            throws ScmServerException {
        logger.debug("update file ext data:ws={}, fileId={}, version={}.{}, ext={}", workspaceName,
                fileId, majorVersion, minorVersion, externalData);
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoChecked(workspaceName);
        ScmLockPath lockPath = ScmLockPathFactory.createFileLockPath(workspaceName, fileId);
        ScmLock writeLock = ScmLockManager.getInstance().acquiresWriteLock(lockPath);
        try {
            return ScmContentModule.getInstance().getMetaService().updateFileExternalData(wsInfo,
                    fileId, majorVersion, minorVersion, externalData, transactionContext);
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void updateFileExternalData(String workspaceName, BSONObject matcher,
            BSONObject externalData) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoChecked(workspaceName);
        ScmContentModule.getInstance().getMetaService().updateFileExternalData(wsInfo, matcher,
                externalData);
    }

    @Override
    public BasicBSONList getFileContentLocations(ScmUser user, BSONObject fileInfo,
            String workspaceName) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriorityById(user, workspaceName, dirService,
                (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID), ScmPrivilegeDefine.READ,
                "get file content locations");
        BasicBSONList result = new BasicBSONList();
        ScmContentModule contentServer = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentServer.getWorkspaceInfoChecked(workspaceName);
        Map<Integer, ScmLocation> wsDataLocations = ws.getDataLocations();
        BasicBSONList siteList = BsonUtils.getArrayChecked(fileInfo,
                FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        Date createTime = new Date(
                CommonHelper.toLongValue(fileInfo.get(FieldName.FIELD_CLFILE_INNER_CREATE_TIME)));
        String dataId = BsonUtils.getStringChecked(fileInfo, FieldName.FIELD_CLFILE_FILE_DATA_ID);

        for (Object siteObj : siteList) {
            BasicBSONObject siteBson = (BasicBSONObject) siteObj;
            Integer siteId = BsonUtils.getIntegerChecked(siteBson,
                    FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID);
            ScmLocation scmLocation = wsDataLocations.get(siteId);
            BSONObject contentLocation = ScmContentLocationResolver
                    .getResolver(scmLocation.getType())
                    .resolve(siteId, ws, contentServer.getAllSiteInfo(), createTime, dataId);
            result.add(contentLocation);
        }
        audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0,
                "get file content locations, file id=" + fileInfo.get(FieldName.FIELD_CLFILE_ID)
                        + ", fileName=" + fileInfo.get(FieldName.FIELD_CLFILE_NAME));
        return result;
    }
}
