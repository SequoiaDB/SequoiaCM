package com.sequoiacm.contentserver.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sequoiacm.contentserver.remote.ContentServerFeignExceptionConverter;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.infrastructure.feign.ScmFeignExceptionUtils;
import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.common.module.TagType;
import com.sequoiacm.contentserver.pipeline.file.module.*;
import com.sequoiacm.contentserver.tag.TagLibMgr;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.InvalidArgumentException;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.ScmContentLocationResolver;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.dao.FileContentUpdateDao;
import com.sequoiacm.contentserver.dao.FileCreatorDao;
import com.sequoiacm.contentserver.dao.FileDeletorDao;
import com.sequoiacm.contentserver.dao.FileInfoUpdaterDao;
import com.sequoiacm.contentserver.dao.FileReaderDao;
import com.sequoiacm.contentserver.dao.FileVersionDeleteDao;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.job.ScmJobCacheFile;
import com.sequoiacm.contentserver.job.ScmJobManager;
import com.sequoiacm.contentserver.job.ScmJobTransferFile;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.FileMetaOperator;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.remote.ContentServerClient;
import com.sequoiacm.contentserver.remote.ContentServerClientFactory;
import com.sequoiacm.contentserver.service.IBatchService;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.contentserver.site.ScmSite;
import com.sequoiacm.contentserver.strategy.ScmStrategyMgr;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.metadata.ScmLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.monitor.FlowRecorder;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.infrastructure.strategy.element.SiteInfo;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.TransactionContext;

@Service
public class FileServiceImpl implements IFileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private FileOperationListenerMgr listenerMgr;

    @Autowired
    private IBatchService batchService;

    @Autowired
    private ScmAudit audit;
    @Autowired
    private BucketInfoManager bucketInfoMgr;

    @Autowired
    private FileCreatorDao fileCreatorDao;

    @Autowired
    private FileContentUpdateDao fileContentUpdateDao;

    @Autowired
    private FileDeletorDao fileDeletorDao;

    @Autowired
    private FileVersionDeleteDao fileVersionDeleteDao;

    @Autowired
    private FileMetaOperator fileMetaOperator;

    @Autowired
    private FileInfoUpdaterDao fileInfoUpdatorDao;

    @Autowired
    private FileMetaFactory fileMetaFactory;

    @Autowired
    private FileInfoCursorTagWrapper fileInfoCursorTagWrapper;

    @Override
    public FileMeta getFileInfoById(ScmUser user, String workspaceName, String fileId,
            int majorVersion, int minorVersion, boolean acceptDeleteMarker)
            throws ScmServerException {
        FileMeta fileInfo = getFileInfoById(workspaceName, fileId, majorVersion, minorVersion,
                acceptDeleteMarker);
        ScmFileServicePriv.getInstance().checkFilePriority(user, workspaceName, fileInfo,
                ScmPrivilegeDefine.READ, "get file by id");
        audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0,
                "get file info by fileId=" + fileId + ", fileName=" + fileInfo.getName());
        return fileInfo;
    }

    @Override
    public FileMeta getFileInfoById(String workspaceName, String fileId, int majorVersion,
            int minorVersion, boolean acceptDeleteMarker) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
        BSONObject fileInfo = contentModule.getMetaService().getFileInfo(ws.getMetaLocation(),
                workspaceName, fileId, majorVersion, minorVersion, acceptDeleteMarker);
        if (fileInfo == null) {
            throw new ScmFileNotFoundException(
                    "file not exist:workspace=" + workspaceName + ",fileId=" + fileId + ",version="
                            + ScmSystemUtils.getVersionStr(majorVersion, minorVersion));
        }

        return fileMetaFactory.createFileMetaByRecord(workspaceName, fileInfo);
    }

    @Override
    public MetaCursor getFileList(String workspaceName, BSONObject condition, int scope,
            BSONObject orderby, long skip, long limit, BSONObject selector,
            boolean isResContainsDeleteMarker) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);

        MetaCursor metaCursor = null;

        if (selector == null) {
            selector = new BasicBSONObject();
            selector.put(FieldName.FIELD_CLFILE_ID, null);
            selector.put(FieldName.FIELD_CLFILE_NAME, null);
            selector.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, null);
            selector.put(FieldName.FIELD_CLFILE_MINOR_VERSION, null);
            selector.put(FieldName.FIELD_CLFILE_FILE_MIME_TYPE, null);
            selector.put(FieldName.FIELD_CLFILE_INNER_USER, null);
            selector.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, null);
            selector.put(FieldName.FIELD_CLFILE_DELETE_MARKER, null);
            selector.put(FieldName.FIELD_CLFILE_VERSION_SERIAL, null);
        }

        if (scope == CommonDefine.Scope.SCOPE_CURRENT) {
            metaCursor = contentModule.getMetaService().queryCurrentFile(ws, condition, selector,
                    orderby, skip, limit, isResContainsDeleteMarker);
        }
        else {
            if (scope == CommonDefine.Scope.SCOPE_HISTORY) {
                metaCursor = contentModule.getMetaService().queryHistoryFile(ws.getMetaLocation(),
                        workspaceName, condition, selector, orderby, skip, limit,
                        isResContainsDeleteMarker);
            }

            else if (scope == CommonDefine.Scope.SCOPE_ALL) {
                if (skip != 0 || limit != -1) {
                    throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                            "query all file unsupported skip/limit");
                }
                metaCursor = contentModule.getMetaService().queryAllFile(ws, condition, selector,
                        orderby, isResContainsDeleteMarker);
            }
            else {
                throw new ScmInvalidArgumentException("unknown scope:scope=" + scope);
            }
        }

        // 若 selector 中包含标签字段，需要将标签 ID 转为标签字符串
        return fileInfoCursorTagWrapper.wrapFileInfoCursor(ws, metaCursor, selector);
    }

    @Override
    public MetaCursor getFileList(ScmUser user, String workspaceName, BSONObject condition,
            int scope, BSONObject orderby, long skip, long limit, BSONObject selector,
            boolean isResContainsDeleteMarker) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.LOW_LEVEL_READ, "list files");
        MetaCursor ret = getFileList(workspaceName, condition, scope, orderby, skip, limit,
                selector, isResContainsDeleteMarker);
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

        ScmFileServicePriv.getInstance().checkDirPriorityById(user, workspaceName,
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
                "list directory's files, dirId=" + dirId + ", matcher=" + matcher.toString());
        boolean isResContainsDeleteMarker = ScmSystemUtils.isDeleteMarkerRequired(scope);
        return getFileList(workspaceName, matcher, CommonDefine.Scope.SCOPE_CURRENT, orderby, skip,
                limit, selector, isResContainsDeleteMarker);
    }

    @Override
    public FileMeta createFile(ScmUser user, String workspace, FileMeta fileMeta,
            FileUploadConf conf, InputStream fileData) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspace,
                conf.getExistStrategy() == FileExistStrategy.OVERWRITE
                        ? ScmPrivilegeDefine.CREATE.getFlag() | ScmPrivilegeDefine.DELETE.getFlag()
                        : ScmPrivilegeDefine.CREATE.getFlag(),
                "create file");
        FileMeta ret = createFile(workspace, fileMeta, conf, fileData);
        audit.info(ScmAuditType.CREATE_FILE, user, workspace, 0,
                "create file , fileId=" + fileMeta.getId() + ", fileName=" + fileMeta.getName());
        return ret;
    }

    @Override
    public FileMeta createFile(String workspace, FileMeta fileMeta, FileUploadConf conf,
            InputStream fileData) throws ScmServerException {
        FileMeta ret = fileCreatorDao.createFile(workspace, fileMeta, conf, fileData);
        FlowRecorder.getInstance().addUploadSize(workspace, ret.getSize());
        return ret;
    }

    @Override
    public FileMeta createFile(ScmUser user, String workspace, FileMeta fileMeta,
            FileUploadConf conf, String breakpointFileName) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspace,
                conf.getExistStrategy() == FileExistStrategy.OVERWRITE
                        ? ScmPrivilegeDefine.CREATE.getFlag() | ScmPrivilegeDefine.DELETE.getFlag()
                        : ScmPrivilegeDefine.CREATE.getFlag(),
                "create file");
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspace, ScmPrivilegeDefine.UPDATE,
                "overwrite file for detach batch");
        FileMeta ret = fileCreatorDao.createFile(workspace, fileMeta, conf, breakpointFileName);
        audit.info(ScmAuditType.CREATE_FILE, user, workspace, 0,
                "create file , fileId=" + fileMeta.getId() + ", fileName=" + fileMeta.getName()
                        + ", breakpointFile=" + breakpointFileName);
        return ret;
    }

    @Override
    public FileMeta createFile(String workspace, FileMeta fileMeta, FileUploadConf conf,
            String breakpointFileName) throws ScmServerException {
        return fileCreatorDao.createFile(workspace, fileMeta, conf, breakpointFileName);
    }

    @Override
    public FileMeta createFile(String workspace, FileMeta fileMeta, FileUploadConf conf,
            TransactionCallback transactionCallback) throws ScmServerException {
        return fileCreatorDao.createFile(workspace, fileMeta, conf, transactionCallback);
    }

    @Override
    public FileReaderDao downloadFile(String sessionId, String userDetail, String workspaceName,
            FileMeta fileInfo, int readFlag) throws ScmServerException {
        return new FileReaderDao(sessionId, userDetail,
                ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(workspaceName),
                fileInfo.toRecordBSON(), readFlag);
    }

    @Override
    public FileReaderDao downloadFile(String sessionId, String userDetail, ScmUser user,
            String workspaceName, FileMeta fileInfo, int readFlag) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkFilePriority(user, workspaceName, fileInfo,
                ScmPrivilegeDefine.READ, "read file");
        FileReaderDao dao = downloadFile(sessionId, userDetail, workspaceName, fileInfo, readFlag);
        audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0,
                "read file, fileId=" + fileInfo.getId() + ", fileName=" + fileInfo.getName());
        return dao;
    }

    public void deleteFile(String sessionid, String userDetail, String workspaceName, String fileId,
            int majorVersion, int minorVersion, boolean isPhysical) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckLocalSite(workspaceName);
        fileDeletorDao.delete(sessionid, userDetail, wsInfo, fileId, isPhysical);
    }

    @Override
    public void deleteFile(String sessionid, String userDetail, ScmUser user, String workspaceName,
            String fileId, int majorVersion, int minorVersion, boolean isPhysical)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkFilePriorityByFileId(user, workspaceName, this,
                fileId, majorVersion, minorVersion, ScmPrivilegeDefine.DELETE,
                "delete file");
        deleteFile(sessionid, userDetail, workspaceName, fileId, majorVersion, minorVersion,
                isPhysical);
        audit.info(ScmAuditType.DELETE_FILE, user, workspaceName, 0,
                "delete file by fileId=" + fileId);
    }

    public long countFiles(ScmUser user, String workspaceName, int scope, BSONObject condition,
            boolean isResContainsDeleteMarker) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, workspaceName,
                ScmPrivilegeDefine.LOW_LEVEL_READ, "count file");
        long ret = countFiles(workspaceName, scope, condition, isResContainsDeleteMarker);
        String message = "count file";
        if (null != condition) {
            message += " by condition=" + condition.toString();
        }
        audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0, message);
        return ret;
    }

    @Override
    public long countFiles(String workspaceName, int scope, BSONObject condition,
            boolean isResContainsDeleteMarker) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);

        if (scope == CommonDefine.Scope.SCOPE_CURRENT) {
            return contentModule.getMetaService().getCurrentFileCount(wsInfo, condition,
                    isResContainsDeleteMarker);
        }

        if (scope == CommonDefine.Scope.SCOPE_HISTORY) {
            return contentModule.getMetaService().getHistoryFileCount(wsInfo.getMetaLocation(),
                    workspaceName, condition, isResContainsDeleteMarker);
        }
        if (scope == CommonDefine.Scope.SCOPE_ALL) {
            return contentModule.getMetaService().getAllFileCount(wsInfo.getMetaLocation(),
                    workspaceName, condition, isResContainsDeleteMarker);
        }
        throw new ScmInvalidArgumentException("unknown scope:scope=" + scope);
    }

    @Override
    public long sumFileSizes(String workspaceName, int scope, BSONObject condition)
            throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);

        if (scope == CommonDefine.Scope.SCOPE_CURRENT) {
            return contentModule.getMetaService().getCurrentFileSizeSum(wsInfo, condition);
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
    public FileMeta updateFileInfo(ScmUser user, String workspaceName, String fileId,
            BSONObject fileInfo, int majorVersion, int minorVersion) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkFilePriorityByFileId(user, workspaceName, this,
                fileId, majorVersion, minorVersion, ScmPrivilegeDefine.UPDATE, "update file by id");
        FileMeta ret = fileInfoUpdatorDao.updateInfo(user.getUsername(), workspaceName, fileId,
                majorVersion, minorVersion, fileInfo);
        audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                "update file by fileId=" + fileId);
        return ret;
    }

    @Override
    public void asyncTransferFile(ScmUser user, String workspaceName, String fileId,
            int majorVersion, int minorVersion, String userTargetSite) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkFilePriorityByFileId(user, workspaceName, this,
                fileId, majorVersion, minorVersion, ScmPrivilegeDefine.UPDATE,
                "async transfer file");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
        int localSiteId = contentModule.getLocalSite();

        // 1. check file
        BSONObject file = ScmContentModule.getInstance().getMetaService().getFileInfo(
                wsInfo.getMetaLocation(), workspaceName, fileId, majorVersion, minorVersion, false);
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
            ScmSite siteInfo = contentModule.getSiteInfo(userTargetSite);
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
                    "async transfer file by fileId=" + fileId);
            return;
        }

        String dataId = (String) file.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        ScmJobTransferFile job = new ScmJobTransferFile(wsInfo, fileId, majorVersion, minorVersion,
                dataId, transferTargetSiteId);
        ScmJobManager.getInstance().executeShortTimeTask(job);
        audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                "async transfer file by fileId=" + fileId);
    }

    @Override
    public void asyncCacheFile(ScmUser user, String workspaceName, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkFilePriorityByFileId(user, workspaceName, this,
                fileId, majorVersion, minorVersion, ScmPrivilegeDefine.UPDATE,
                "async cache file");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);

        // 1. check cache site
        int localSiteId = contentModule.getLocalSite();
        ScmStrategyMgr.getInstance().checkCacheSite(wsInfo, localSiteId);

        // 2. check file
        BSONObject file = ScmContentModule.getInstance().getMetaService().getFileInfo(
                wsInfo.getMetaLocation(), workspaceName, fileId, majorVersion, minorVersion, false);
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
                    "async cache file by fileId=" + fileId);
            return;
        }

        // 4. get remote site
        int remoteSiteId = ScmStrategyMgr.getInstance().getAsyncCacheRemoteSite(wsInfo, siteIdList,
                localSiteId, fileId);

        String dataId = (String) file.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        ScmJobCacheFile job = new ScmJobCacheFile(wsInfo, fileId, majorVersion, minorVersion,
                dataId, remoteSiteId);
        ScmJobManager.getInstance().executeShortTimeTask(job);
        audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                "async cache file by fileId=" + fileId);
    }

    private static ScmLock lockBreakpointFile(String workspaceName, String breakpointFileName)
            throws ScmServerException {
        ScmLockPath lockPath = ScmLockPathFactory.createBPLockPath(workspaceName,
                breakpointFileName);
        return ScmLockManager.getInstance().acquiresLock(lockPath);
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
    public FileMeta updateFileContent(ScmUser user, String workspaceName, String fileId,
            InputStream newFileContent, int majorVersion, int minorVersion,
            ScmUpdateContentOption option) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkFilePriorityByFileId(user, workspaceName, this,
                fileId, majorVersion, minorVersion, ScmPrivilegeDefine.UPDATE,
                "update file by id");
        FileMeta fileMeta = fileContentUpdateDao.updateContent(user.getUsername(), workspaceName,
                fileId, majorVersion, minorVersion, option, newFileContent);
        audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                "update file by fileId=" + fileId);
        return fileMeta;
    }

    @Override
    public FileMeta updateFileContent(ScmUser user, String workspaceName, String fileId,
            String newBreakpointFileContent, int majorVersion, int minorVersion,
            ScmUpdateContentOption option) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkFilePriorityByFileId(user, workspaceName, this,
                fileId, majorVersion, minorVersion, ScmPrivilegeDefine.UPDATE,
                "update file by id");
        FileMeta fileMeta = fileContentUpdateDao.updateContent(user.getUsername(), workspaceName,
                fileId, majorVersion, minorVersion, option, newBreakpointFileContent);
        audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                "update file by fileId=" + fileId);
        return fileMeta;
    }

    @Override
    public String calcFileMd5(String sessionid, String userDetail, ScmUser user,
            String workspaceName, String fileId, int majorVersion, int minorVersion)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkFilePriorityByFileId(user, workspaceName, this,
                fileId, majorVersion, minorVersion, ScmPrivilegeDefine.UPDATE,
                "calculate file md5");
        FileMeta fileInfo = getFileInfoById(workspaceName, fileId, majorVersion, minorVersion,
                false);
        BasicBSONList siteBson = fileInfo.getSiteList();
        List<ScmFileLocation> siteList = new ArrayList<>();
        CommonHelper.getFileLocationList(siteBson, siteList);
        Map<Integer, ScmFileLocation> fileLocationMap = CommonHelper.getFileLocationList(siteBson);

        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);

        String md5 = null;
        ScmFileLocation localFileLocation = fileLocationMap.get(contentModule.getLocalSite());
        if (localFileLocation != null) {
            // 在本地读取数据计算MD5
            ScmDataInfo dataInfo = ScmDataInfo.forOpenExistData(fileInfo.toRecordBSON(),
                    localFileLocation.getWsVersion(), localFileLocation.getTableName());
            md5 = ScmSystemUtils.calcMd5(ws, dataInfo);
            updateFileMd5(user.getUsername(), ws, fileId, majorVersion, minorVersion,
                    dataInfo.getId(), md5);
        }
        else {
            // 发给远程站点，让远程站点执行计算或再转发
            List<Integer> siteIdList = CommonHelper.getFileLocationIdList(siteList);
            SiteInfo siteInfo = ScmStrategyMgr.getInstance().getNearestSite(ws, siteIdList,
                    contentModule.getLocalSite(), fileId);
            String remoteSite = contentModule.getSiteInfo(siteInfo.getId()).getName();
            ContentServerClient client = ContentServerClientFactory
                    .getFeignClientByServiceName(remoteSite);
            BSONObject resp = client.calcFileMd5KeepAlive(sessionid, userDetail, ws.getName(),
                    fileId, majorVersion, minorVersion);
            try {
                ScmFeignExceptionUtils.handleException(resp);
            }
            catch (ScmFeignException e) {
                throw new ContentServerFeignExceptionConverter().convert(e);
            }
            md5 = BsonUtils.getStringChecked(resp, FieldName.FIELD_CLFILE_FILE_MD5);
        }
        audit.info(ScmAuditType.UPDATE_FILE, user, workspaceName, 0,
                "calculate file md5, fileId=" + fileId);
        return md5;
    }

    public void updateFileMd5(String user, ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion, String dataId, String md5) throws ScmServerException {
        ScmLockPath lockPath = ScmLockPathFactory.createFileLockPath(wsInfo.getName(), fileId);
        ScmLock writeLock = ScmLockManager.getInstance().acquiresWriteLock(lockPath);
        try {
            FileMeta fileInfo = getFileInfoById(wsInfo.getName(), fileId, majorVersion,
                    minorVersion, false);

            // 锁内确认文件数据没有发生变化（桶内 -2.0 版本文件可以被重复覆盖，而文件的 ID、版本号不会发生变化，所以这里查出来的文件比对以下 data Id）
            if (!fileInfo.getDataId().equals(dataId)) {
                // 文件已经被另外一个线程覆盖了，直接返回
                return;
            }

            List<FileMetaUpdater> fileMetaUpdaters = new ArrayList<>();
            fileMetaUpdaters.add(FileMetaDefaultUpdater.versionFieldUpdater(
                    FieldName.FIELD_CLFILE_FILE_MD5, md5, majorVersion, minorVersion));

            // 当文件已经存在ETAG时，不能同时更新文件的ETAG，因为会破坏 S3 对象 ETAG 的定义（描述对象内容，相当于摘要）
            BSONObject newFileInfo = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_MD5, md5);
            String etag = fileInfo.getEtag();
            if (etag == null || etag.isEmpty()) {
                fileMetaUpdaters.add(
                        FileMetaDefaultUpdater.versionFieldUpdater(FieldName.FIELD_CLFILE_FILE_ETAG,
                                SignUtil.toHex(md5), majorVersion, minorVersion));
            }

            fileMetaOperator.updateFileMeta(wsInfo.getName(), fileId, fileMetaUpdaters, null, null,
                    null, new ScmVersion(majorVersion, minorVersion));
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("update md5 failed:fileId=" + fileId + ",majorVersion="
                    + majorVersion + ",minorVersion=" + minorVersion + ",md5=" + md5, e);
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public String generateId(Date fileCreateTime) {
        return ScmIdGenerator.FileId.get(fileCreateTime);
    }

    @Override
    public boolean updateFileExternalData(String workspaceName, String fileId, int majorVersion,
            int minorVersion, BSONObject externalData, TransactionContext transactionContext)
            throws ScmServerException {
        logger.debug("update file ext data:ws={}, fileId={}, version={}.{}, ext={}", workspaceName,
                fileId, majorVersion, minorVersion, externalData);
        ScmLockPath lockPath = ScmLockPathFactory.createFileLockPath(workspaceName, fileId);
        ScmLock writeLock = ScmLockManager.getInstance().acquiresWriteLock(lockPath);
        try {
            List<FileMetaUpdater> fileMetaUpdaters = new ArrayList<>();
            for (String key : externalData.keySet()) {
                fileMetaUpdaters.add(FileMetaDefaultUpdater.versionFieldUpdater(
                        FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + "." + key,
                        externalData.get(key), majorVersion, minorVersion));
            }
            fileMetaOperator.updateFileMeta(workspaceName, fileId, fileMetaUpdaters, null, null,
                    null, new ScmVersion(majorVersion, minorVersion));
            return true;
        }
        catch (ScmServerException e) {
            if (e.getError() == ScmError.FILE_NOT_FOUND) {
                return false;
            }
            throw e;
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void updateFileExternalData(String workspaceName, BSONObject matcher,
            BSONObject externalData) throws ScmServerException {
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckLocalSite(workspaceName);
        ScmContentModule.getInstance().getMetaService().updateFileExternalData(wsInfo, matcher,
                externalData);
    }

    @Override
    public BasicBSONList getFileContentLocations(ScmUser user, FileMeta fileInfo,
            String workspaceName) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkFilePriority(user, workspaceName, fileInfo,
                ScmPrivilegeDefine.READ, "get file content locations");
        BasicBSONList result = new BasicBSONList();
        ScmContentModule contentServer = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentServer.getWorkspaceInfoCheckLocalSite(workspaceName);
        BasicBSONList siteList = fileInfo.getSiteList();
        Date createTime = new Date(fileInfo.getCreateTime());
        String dataId = fileInfo.getDataId();
        Map<Integer, ScmFileLocation> fileLocationMap = CommonHelper.getFileLocationList(siteList);

        for (ScmFileLocation fileLocation : fileLocationMap.values()) {
            ScmLocation scmLocation = ws.getSiteDataLocation(fileLocation.getSiteId(),
                    fileLocation.getWsVersion());
            BSONObject contentLocation = ScmContentLocationResolver
                    .getResolver(scmLocation.getType()).resolve(fileLocation.getSiteId(), ws,
                            contentServer.getAllSiteInfo(), createTime, dataId,
                            fileLocation.getWsVersion(), fileLocation.getTableName());
            result.add(contentLocation);
        }
        audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0,
                "get file content locations, fileId=" + fileInfo.getId() + ", fileName="
                        + fileInfo.getName());
        return result;
    }

    @Override
    public FileMeta deleteVersion(ScmUser user, String ws, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException {
        FileMeta ret = fileVersionDeleteDao.delete(ws, fileId, majorVersion, minorVersion);
        audit.info(ScmAuditType.DELETE_FILE, user, ws, 0, "delete file version: wsName=" + ws
                + ", fileId=" + fileId + ", version=" + majorVersion + "." + minorVersion);
        return ret;
    }
}

@Component
class FileInfoCursorTagWrapper {
    @Autowired
    private TagLibMgr tagLibMgr;

    public MetaCursor wrapFileInfoCursor(ScmWorkspaceInfo ws, MetaCursor fileInfoCursor,
            BSONObject selector) throws ScmServerException {
        try {
            if (!ws.newVersionTag()) {
                return fileInfoCursor;
            }

            if (selector != null && !selector.containsField(FieldName.FIELD_CLFILE_TAGS)
                    && !selector.containsField(FieldName.FIELD_CLFILE_CUSTOM_TAG)) {
                return fileInfoCursor;
            }

            return new FileInfoCursorTagWrapped(tagLibMgr, fileInfoCursor, ws);
        }
        catch (Exception e) {
            fileInfoCursor.close();
            throw e;
        }
    }
}

class FileInfoCursorTagWrapped implements MetaCursor {
    private TagLibMgr tagLibMgr;

    private MetaCursor fileInfoCursor;

    private ScmWorkspaceInfo ws;

    public FileInfoCursorTagWrapped(TagLibMgr tagLibMgr, MetaCursor fileInfoCursor,
            ScmWorkspaceInfo ws) {
        this.tagLibMgr = tagLibMgr;
        this.fileInfoCursor = fileInfoCursor;
        this.ws = ws;
    }

    @Override
    public boolean hasNext() throws ScmMetasourceException {
        return fileInfoCursor.hasNext();
    }

    @Override
    public BSONObject getNext() throws ScmMetasourceException {
        BSONObject ret = fileInfoCursor.getNext();
        if (ret == null) {
            return null;
        }

        List<Long> tagsIdList = BsonUtils.getLongArray(ret, FieldName.FIELD_CLFILE_TAGS);
        if (tagsIdList == null) {
            tagsIdList = Collections.emptyList();
        }
        List<Long> customTagIdList = BsonUtils.getLongArray(ret, FieldName.FIELD_CLFILE_CUSTOM_TAG);
        if (customTagIdList == null) {
            customTagIdList = Collections.emptyList();
        }

        List<Long> allTagIdList = new ArrayList<>();
        allTagIdList.addAll(customTagIdList);
        allTagIdList.addAll(tagsIdList);

        List<TagInfo> tagInfoList = null;
        try {
            tagInfoList = tagLibMgr.getTagInfoById(ws, allTagIdList);
        }
        catch (ScmServerException e) {
            throw new ScmMetasourceException(
                    "get tag info failed: " + allTagIdList + ", ws=" + ws.getName(), e);
        }
        TreeMap<String, String> customTag = new TreeMap<>();
        BasicBSONList tags = new BasicBSONList();
        for (TagInfo tagInfo : tagInfoList) {
            if (tagInfo.getTagType() == TagType.CUSTOM_TAG) {
                customTag.put(tagInfo.getTagName().getTagKey(), tagInfo.getTagName().getTagValue());
            }
            else if (tagInfo.getTagType() == TagType.TAGS) {
                tags.add(tagInfo.getTagName().getTag());
            }
            else {
                throw new ScmMetasourceException(
                        "unknown tag type: " + tagInfo + ", ws=" + ws.getName() + " ,file=" + ret);
            }
        }
        ret.put(FieldName.FIELD_CLFILE_TAGS, tags);
        ret.put(FieldName.FIELD_CLFILE_CUSTOM_TAG, new BasicBSONObject(customTag));
        return ret;
    }

    @Override
    public void close() {
        fileInfoCursor.close();
    }
}
