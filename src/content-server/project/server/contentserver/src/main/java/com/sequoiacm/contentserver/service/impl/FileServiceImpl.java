package com.sequoiacm.contentserver.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.InvalidArgumentException;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.contentserver.common.ScmArgumentChecker;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.common.ServiceDefine;
import com.sequoiacm.contentserver.dao.BreakpointFileConvertor;
import com.sequoiacm.contentserver.dao.FileContentUpdateDao;
import com.sequoiacm.contentserver.dao.FileCreatorDao;
import com.sequoiacm.contentserver.dao.FileDeletorDao;
import com.sequoiacm.contentserver.dao.FileInfoUpdatorDao;
import com.sequoiacm.contentserver.dao.FileReaderDao;
import com.sequoiacm.contentserver.dao.IFileCreatorDao;
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
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.service.IBatchService;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
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
    public BSONObject getFileInfoById(String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        ScmWorkspaceInfo ws = contentServer.getWorkspaceInfoChecked(workspaceName);
        BSONObject fileInfo = contentServer.getMetaService().getFileInfo(ws.getMetaLocation(),
                workspaceName, fileId, majorVersion, minorVersion);
        if (fileInfo == null) {
            throw new ScmFileNotFoundException(
                    "file not exist:workspace=" + workspaceName + ",fileId=" + fileId + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }
        return fileInfo;
    }

    @Override
    public BSONObject getFileInfoByPath(String workspaceName, String filePath, int majorVersion,
            int minorVersion) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        ScmWorkspaceInfo ws = contentServer.getWorkspaceInfoChecked(workspaceName);
        String fileName = ScmSystemUtils.basename(filePath);
        String parentDirPath = ScmSystemUtils.dirname(filePath);
        BSONObject fileInfo = null;
        try {
            BSONObject parentDir = dirService.getDirInfoByPath(workspaceName, parentDirPath);

            String parentDirId = (String) parentDir.get(FieldName.FIELD_CLDIR_ID);
            fileInfo = contentServer.getMetaService().getFileInfo(ws, parentDirId, fileName,
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
        return fileInfo;
    }

    @Override
    public MetaCursor getFileList(String workspaceName, BSONObject condition, int scope,
            BSONObject orderby, long skip, long limit, BSONObject selector) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        ScmWorkspaceInfo ws = contentServer.getWorkspaceInfoChecked(workspaceName);

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
                return contentServer.getMetaService().queryCurrentFile(ws.getMetaLocation(),
                        workspaceName, condition, selector, orderby, skip, limit);
            }
            else {
                ScmArgChecker.File.checkHistoryFileMatcher(condition);
                if (scope == CommonDefine.Scope.SCOPE_HISTORY) {
                    return contentServer.getMetaService().queryHistoryFile(ws.getMetaLocation(),
                            workspaceName, condition, selector, orderby, skip, limit);
                }
                else if (scope == CommonDefine.Scope.SCOPE_ALL) {
                    if (orderby != null || skip != 0 || limit != -1) {
                        throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                                "query all file unsupport orderby/skip/limit");
                    }
                    return contentServer.getMetaService().queryAllFile(ws.getMetaLocation(),
                            workspaceName, condition, selector);
                }
                else {
                    throw new ScmInvalidArgumentException("unknown scope:scope=" + scope);
                }
            }
        }
        catch (InvalidArgumentException e) {
            throw new ScmInvalidArgumentException("Invalid condition: " + condition, e);
        }
    }

    @Override
    public BSONObject uploadFile(String workspaceName, String username, InputStream is,
            BSONObject fileInfo) throws ScmServerException {
        return uploadFile(workspaceName, username, is, fileInfo, null, null, null,
                new ClientUploadConf());
    }

    // when overwrite file, may be to forward request of delete file.
    @Override
    public BSONObject uploadFile(String workspaceName, String username, InputStream is,
            BSONObject fileInfo, String sessionId, String userDetail,
            ScmUserPasswordType passwordType, ClientUploadConf uploadConf)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        ScmWorkspaceInfo wsInfo = contentServer.getWorkspaceInfoChecked(workspaceName);

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
        addExtraField(checkedFileObj, fileId, dataId, fileCreateDate, dataCreateDate, username,
                contentServer.getLocalSite(), 1, 0);

        // checkDirWithSameNameExist(checkedFileObj, workspaceName);

        ScmDataInfo dataInfo = new ScmDataInfo(ENDataType.Normal.getValue(), dataId,
                dataCreateDate);
        listenerMgr.preCreate(wsInfo, checkedFileObj);
        FileCreatorDao dao = new FileCreatorDao(contentServer.getLocalSite(), wsInfo,
                checkedFileObj, dataInfo, uploadConf.isNeedMd5());
        BSONObject finfo = null;
        try {
            dao.write(is);
            finfo = insertFileInfo(dao, checkedFileObj, uploadConf, sessionId, username,
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
        return finfo;
    }

    @Override
    public BSONObject uploadFile(String workspaceName, String username, String breakpointFileName,
            BSONObject fileInfo) throws ScmServerException {
        return uploadFile(workspaceName, username, breakpointFileName, fileInfo, null, null, null,
                new ClientUploadConf());
    }

    @Override
    public BSONObject uploadFile(String workspaceName, String username, String breakpointFileName,
            BSONObject fileInfo, String sessionId, String userDetail,
            ScmUserPasswordType passwordType, ClientUploadConf uploadConf)
            throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        ScmWorkspaceInfo wsInfo = contentServer.getWorkspaceInfoChecked(workspaceName);
        BSONObject ret;
        OperationCompleteCallback callback;
        String fileId;
        ScmLock lock = lockBreakpointFile(workspaceName, breakpointFileName);
        try {
            BreakpointFile breakpointFile = contentServer.getMetaService()
                    .getBreakpointFile(workspaceName, breakpointFileName);
            if (breakpointFile == null) {
                throw new ScmInvalidArgumentException(String.format(
                        "BreakpointFile is not found: /%s/%s", workspaceName, breakpointFileName));
            }

            if (breakpointFile.getSiteId() != contentServer.getLocalSite()) {
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

            addExtraField(checkedFileObj, fileId, dataId, fileCreateDate, dataCreateDate, username,
                    contentServer.getLocalSite(), 1, 0);
            checkDirWithSameNameExist(checkedFileObj, workspaceName);

            checkedFileObj.put(FieldName.FIELD_CLFILE_FILE_SIZE, breakpointFile.getUploadSize());
            checkedFileObj.put(FieldName.FIELD_CLFILE_FILE_DATA_TYPE, ENDataType.Normal.getValue());

            if (breakpointFile.getMd5() != null) {
                checkedFileObj.put(FieldName.FIELD_CLFILE_FILE_MD5, breakpointFile.getMd5());
            }

            listenerMgr.preCreate(wsInfo, checkedFileObj);
            IFileCreatorDao fileDao = new BreakpointFileConvertor(wsInfo, breakpointFile,
                    checkedFileObj);
            ret = insertFileInfo(fileDao, checkedFileObj, uploadConf, sessionId, username,
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
        return ret;
    }

    private BSONObject insertFileInfo(IFileCreatorDao fileDao, BSONObject fileInfo,
            ClientUploadConf uploadConf, String sessionId, String username,
            ScmUserPasswordType passwordType, String userDetail) throws ScmServerException {
        try {
            return fileDao.insert();
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_EXIST && uploadConf.isOverwrite()) {
                String parentDirId = (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID);
                String fileName = (String) fileInfo.get(FieldName.FIELD_CLFILE_NAME);
                ScmContentServer contentService = ScmContentServer.getInstance();
                String existFileId = contentService.getMetaService()
                        .getFileId(fileDao.getWorkspaceName(), parentDirId, fileName);
                if (existFileId != null) {
                    deleteCurrentFile(sessionId, fileDao.getWorkspaceName(), username, passwordType,
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

    private void deleteCurrentFile(String sessionId, String workspaceName, String username,
            ScmUserPasswordType passwordType, String userDetail, String existFileId)
            throws ScmServerException {
        try {
            ScmContentServer contentService = ScmContentServer.getInstance();
            ScmWorkspaceInfo wsInfo = contentService.getWorkspaceInfoChecked(workspaceName);
            BSONObject existFileInfo = contentService.getMetaService()
                    .getCurrentFileInfo(wsInfo.getMetaLocation(), workspaceName, existFileId);
            if (existFileInfo == null) {
                return;
            }

            String batchId = (String) existFileInfo.get(FieldName.FIELD_CLFILE_BATCH_ID);
            if (batchId != null && !batchId.isEmpty()) {
                try {
                    batchService.detachFile(username, workspaceName, batchId, existFileId);
                    audit.info(ScmAuditType.UPDATE_BATCH,
                            ScmUserAuditType.getScmUserAuditType(passwordType.toString()), username,
                            workspaceName, 0, "overwrite file, detach batch's file batchId="
                                    + batchId + ", fileId=" + existFileId);
                }
                catch (ScmServerException e) {
                    if (e.getError() != ScmError.BATCH_NOT_FOUND
                            && e.getError() != ScmError.FILE_NOT_IN_BATCH) {
                        throw e;
                    }
                }
            }

            ScmFileServicePriv.getInstance().checkDirPriorityByFileId(username, workspaceName, this,
                    existFileId, -1, -1, dirService, ScmPrivilegeDefine.DELETE,
                    "overwrite file,delete old file");
            deleteFile(sessionId, userDetail, workspaceName, username, existFileId, -1, -1, true);
            audit.info(ScmAuditType.DELETE_FILE,
                    ScmUserAuditType.getScmUserAuditType(passwordType.toString()), username,
                    workspaceName, 0, "overwrite file, delete old file by file id=" + existFileId);
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
        FileReaderDao dao = new FileReaderDao(sessionId, userDetail,
                ScmContentServer.getInstance().getWorkspaceInfoChecked(workspaceName), fileInfo,
                readFlag);
        return dao;
    }

    @Override
    public void deleteFile(String sessionid, String userDetail, String workspaceName,
            String username, String fileId, int majorVersion, int minorVersion, boolean isPhysical)
            throws ScmServerException {
        try {
            FileDeletorDao dao = new FileDeletorDao();
            ScmWorkspaceInfo wsInfo = ScmContentServer.getInstance()
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
    public long countFiles(String workspaceName, int scope, BSONObject condition)
            throws ScmServerException {
        ScmContentServer contentserver = ScmContentServer.getInstance();
        ScmWorkspaceInfo wsInfo = contentserver.getWorkspaceInfoChecked(workspaceName);

        if (scope == CommonDefine.Scope.SCOPE_CURRENT) {
            return contentserver.getMetaService().getCurrentFileCount(wsInfo.getMetaLocation(),
                    workspaceName, condition);
        }

        try {
            ScmArgChecker.File.checkHistoryFileMatcher(condition);
        }
        catch (InvalidArgumentException e) {
            throw new ScmInvalidArgumentException("Invalid condition: " + condition, e);
        }
        if (scope == CommonDefine.Scope.SCOPE_HISTORY) {
            return contentserver.getMetaService().getHistoryFileCount(wsInfo.getMetaLocation(),
                    workspaceName, condition);
        }
        if (scope == CommonDefine.Scope.SCOPE_ALL) {
            return contentserver.getMetaService().getAllFileCount(wsInfo.getMetaLocation(),
                    workspaceName, condition);
        }
        throw new ScmInvalidArgumentException("unknown scope:scope=" + scope);
    }

    @Override
    public long sumFileSizes(String workspaceName, int scope, BSONObject condition)
            throws ScmServerException {
        ScmContentServer contentserver = ScmContentServer.getInstance();
        ScmWorkspaceInfo wsInfo = contentserver.getWorkspaceInfoChecked(workspaceName);

        if (scope == CommonDefine.Scope.SCOPE_CURRENT) {
            return contentserver.getMetaService().getCurrentFileSizeSum(wsInfo.getMetaLocation(),
                    workspaceName, condition);
        }

        try {
            ScmArgChecker.File.checkHistoryFileMatcher(condition);
        }
        catch (InvalidArgumentException e) {
            throw new ScmInvalidArgumentException("Invalid condition: " + condition, e);
        }
        if (scope == CommonDefine.Scope.SCOPE_HISTORY) {
            return contentserver.getMetaService().getHistoryFileSizeSum(wsInfo.getMetaLocation(),
                    workspaceName, condition);
        }
        if (scope == CommonDefine.Scope.SCOPE_ALL) {
            return contentserver.getMetaService().getAllFileSizeSum(wsInfo.getMetaLocation(),
                    workspaceName, condition);
        }
        throw new ScmInvalidArgumentException("unknown scope:scope=" + scope);
    }

    @Override
    public BSONObject updateFileInfo(String workspaceName, String user, String fileId,
            BSONObject fileInfo, int majorVersion, int minorVersion) throws ScmServerException {
        BSONObject ret;
        OperationCompleteCallback callback = null;
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(workspaceName);
        ScmLockPath lockPath = ScmLockPathFactory.createFileLockPath(ws.getName(), fileId);
        ScmLock writeLock = ScmLockManager.getInstance().acquiresWriteLock(lockPath);
        try {
            FileInfoUpdatorDao dao = new FileInfoUpdatorDao(user, ws, fileId, majorVersion,
                    minorVersion, fileInfo);
            ret = dao.updateInfo();
            callback = listenerMgr.postUpdate(ws, dao.getFileInfoBeforeUpdate());
        }
        finally {
            writeLock.unlock();
        }
        callback.onComplete();
        return ret;
    }

    @Override
    public void asyncTransferFile(String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException {
        ScmContentServer contentserver = ScmContentServer.getInstance();
        ScmWorkspaceInfo wsInfo = contentserver.getWorkspaceInfoChecked(workspaceName);
        int localSiteId = contentserver.getLocalSite();

        // 1. check file
        BSONObject file = ScmContentServer.getInstance().getMetaService().getFileInfo(
                wsInfo.getMetaLocation(), workspaceName, fileId, majorVersion, minorVersion);
        if (null == file) {
            throw new ScmFileNotFoundException("file is not exist:fileId=" + fileId + ",version="
                    + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }

        BasicBSONList sites = (BasicBSONList) file.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<ScmFileLocation> siteList = new ArrayList<>();
        CommonHelper.getFileLocationList(sites, siteList);

        // 2. get remote site && check local site can transfer or not
        int remoteSite = ScmStrategyMgr.getInstance().getTargetSite(wsInfo, localSiteId);

        // 3.check local
        if (!CommonHelper.isSiteExist(localSiteId, siteList)) {
            // local site is not exist
            throw new ScmServerException(ScmError.DATA_NOT_EXIST,
                    "file is not exist in local site:fileId=" + fileId + ",localSiteId="
                            + localSiteId + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }

        // 4. check remote
        if (CommonHelper.isSiteExist(remoteSite, siteList)) {
            // remote site is exist. just response and return
            return;
        }

        String dataId = (String) file.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        ScmJobTransferFile job = new ScmJobTransferFile(wsInfo, fileId, majorVersion, minorVersion,
                dataId, remoteSite);
        ScmJobManager.getInstance().schedule(job, 0);
    }

    @Override
    public void asyncCacheFile(String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException {
        ScmContentServer contentserver = ScmContentServer.getInstance();
        ScmWorkspaceInfo wsInfo = contentserver.getWorkspaceInfoChecked(workspaceName);

        // 1. check cache site
        int localSiteId = contentserver.getLocalSite();
        ScmStrategyMgr.getInstance().checkCacheSite(wsInfo, localSiteId);

        // 2. check file
        BSONObject file = ScmContentServer.getInstance().getMetaService().getFileInfo(
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
            return;
        }

        // 4. get remote site
        int remoteSiteId = ScmStrategyMgr.getInstance().getAsyncCacheRemoteSite(wsInfo, siteIdList,
                localSiteId, fileId);

        String dataId = (String) file.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        ScmJobCacheFile job = new ScmJobCacheFile(wsInfo, fileId, majorVersion, minorVersion,
                dataId, remoteSiteId);
        ScmJobManager.getInstance().schedule(job, 0);
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

    private static void addExtraField(BSONObject obj, String fileId, String dataId,
            Date fileCreateDate, Date dataCreateDate, String userName, int siteId, int majorVersion,
            int minorVersion) {
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

        if (obj.get(FieldName.FIELD_CLFILE_DIRECTORY_ID) == null
                || obj.get(FieldName.FIELD_CLFILE_DIRECTORY_ID).equals("")) {
            obj.put(FieldName.FIELD_CLFILE_DIRECTORY_ID, CommonDefine.Directory.SCM_ROOT_DIR_ID);
        }
    }

    private static void checkDirWithSameNameExist(BSONObject fileInfo, String workspaceName)
            throws ScmServerException {
        String fileName = (String) fileInfo.get(FieldName.FIELD_CLFILE_NAME);
        String parentID = (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID);
        ScmMetaService metaService = ScmContentServer.getInstance().getMetaService();

        BSONObject existDirMatcher = new BasicBSONObject();
        existDirMatcher.put(FieldName.FIELD_CLDIR_NAME, fileName);
        existDirMatcher.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, parentID);
        if (metaService.getDirCount(workspaceName, existDirMatcher) > 0) {
            throw new ScmServerException(ScmError.DIR_EXIST,
                    "a directory with the same name already exists:name=" + fileName
                            + ",parentDirectoryId=" + parentID);
        }
    }

    @Override
    public BSONObject updateFileContent(String workspaceName, String user, String fileId,
            InputStream newFileContent, int majorVersion, int minorVersion,
            ScmUpdateContentOption option) throws ScmServerException {
        FileContentUpdateDao dao = new FileContentUpdateDao(user, workspaceName, fileId,
                majorVersion, minorVersion, option, listenerMgr);
        return dao.updateContent(newFileContent);
    }

    @Override
    public BSONObject updateFileContent(String workspaceName, String user, String fileId,
            String newBreakpointFileContent, int majorVersion, int minorVersion,
            ScmUpdateContentOption option) throws ScmServerException {
        FileContentUpdateDao dao = new FileContentUpdateDao(user, workspaceName, fileId,
                majorVersion, minorVersion, option, listenerMgr);
        return dao.updateContent(newBreakpointFileContent);
    }


    @Override
    public String calcFileMd5(String sessionid, String userDetail, String workspaceName,
            String fileId, int majorVersion, int minorVersion) throws ScmServerException {
        BSONObject fileInfo = getFileInfoById(workspaceName, fileId, majorVersion, minorVersion);
        BasicBSONList siteBson = BsonUtils.getArray(fileInfo,
                FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        List<ScmFileLocation> siteList = new ArrayList<>();
        CommonHelper.getFileLocationList(siteBson, siteList);

        ScmContentServer contentServer = ScmContentServer.getInstance();
        ScmWorkspaceInfo ws = contentServer.getWorkspaceInfoChecked(workspaceName);

        String md5 = null;
        if (CommonHelper.isSiteExist(contentServer.getLocalSite(), siteList)) {
            // 在本地读取数据计算MD5
            ScmDataInfo dataInfo = new ScmDataInfo(fileInfo);
            md5 = ScmSystemUtils.calcMd5(ws, dataInfo);
            contentServer.getMetaService().updateFileMd5(ws, fileId, majorVersion, minorVersion,
                    md5);
        }
        else {
            // 发给远程站点，让远程站点执行计算或再转发
            List<Integer> siteIdList = CommonHelper.getFileLocationIdList(siteList);
            SiteInfo siteInfo = ScmStrategyMgr.getInstance().getNearestSite(ws, siteIdList,
                    contentServer.getLocalSite(), fileId);
            String remoteSite = contentServer.getSiteInfo(siteInfo.getId()).getName();
            ContentServerClient client = ContentServerClientFactory
                    .getFeignClientByServiceName(remoteSite);
            BSONObject resp = client.calcMd5(sessionid, userDetail, ws.getName(), fileId,
                    majorVersion, minorVersion);
            md5 = BsonUtils.getStringChecked(resp, FieldName.FIELD_CLFILE_FILE_MD5);
        }
        return md5;
    }

    @Override
    public boolean updateFileExternalData(String workspaceName, String fileId, int majorVersion,
            int minorVersion, BSONObject externalData) throws ScmServerException {
        logger.debug("update file ext data:ws={}, fileId={}, version={}.{}, ext={}", workspaceName,
                fileId, majorVersion, minorVersion, externalData);
        ScmWorkspaceInfo wsInfo = ScmContentServer.getInstance()
                .getWorkspaceInfoChecked(workspaceName);
        ScmLockPath lockPath = ScmLockPathFactory.createFileLockPath(workspaceName, fileId);
        ScmLock writeLock = ScmLockManager.getInstance().acquiresWriteLock(lockPath);
        try {
            return ScmContentServer.getInstance().getMetaService().updateFileExternalData(wsInfo,
                    fileId, majorVersion, minorVersion, externalData);
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void updateFileExternalData(String workspaceName, BSONObject matcher,
            BSONObject externalData) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentServer.getInstance()
                .getWorkspaceInfoChecked(workspaceName);
        ScmContentServer.getInstance().getMetaService().updateFileExternalData(wsInfo, matcher,
                externalData);
    }
}
