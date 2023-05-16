package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.DirCreatorDao;
import com.sequoiacm.contentserver.dao.DirOperator;
import com.sequoiacm.contentserver.dao.DirUpdatorDao;
import com.sequoiacm.contentserver.dao.DireDeletorDao;
import com.sequoiacm.contentserver.exception.ScmFileNotFoundException;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.listener.OperationCompleteCallback;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.FileMetaOperator;
import com.sequoiacm.contentserver.pipeline.file.module.FileExistStrategy;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaDefaultUpdater;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaFactory;
import com.sequoiacm.contentserver.pipeline.file.module.FileUploadConf;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaResult;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaDirAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.Date;

@Service
public class DirServiceImpl implements IDirService {
    private static final Logger logger = LoggerFactory.getLogger(DirServiceImpl.class);

    @Autowired
    private ScmAudit audit;

    @Autowired
    private IFileService fileService;

    @Autowired
    private FileMetaOperator fileMetaOperator;

    @Autowired
    private FileOperationListenerMgr listenerMgr;

    @Autowired
    private FileMetaFactory fileMetaFactory;

    @Override
    public BSONObject getDirInfoById(ScmUser user, String wsName, String dirId)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriorityById(user, wsName, dirId,
                ScmPrivilegeDefine.READ, "get dir path info by id");
        try {
            ScmContentModule contentModule = ScmContentModule.getInstance();
            ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoCheckLocalSite(wsName);
            if (!ws.isEnableDirectory()) {
                throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                        "failed to get directory, directory feature is disable:ws=" + ws.getName()
                                + ", id=" + dirId);
            }
            BSONObject destDir = contentModule.getMetaService().getDirInfo(wsName, dirId);
            if (destDir == null) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "directory not exist:id=" + dirId);
            }
            audit.info(ScmAuditType.DIR_DQL, user, wsName, 0,
                    "get dir info by dirId=" + dirId + ", dirInfo=" + destDir);
            return destDir;
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "get directory failed:wsName=" + wsName + ",directoryId=" + dirId, e);
        }
    }

    public BSONObject getDirInfoByPath(String wsName, String dirPath) throws ScmServerException {
        try {
            ScmWorkspaceInfo ws = ScmContentModule.getInstance()
                    .getWorkspaceInfoCheckLocalSite(wsName);
            if (!ws.isEnableDirectory()) {
                throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                        "failed to get directory, directory feature is disable:ws=" + ws.getName()
                                + ", path=" + dirPath);
            }
            BSONObject destDir = DirOperator.getInstance().getDirByPath(ws, dirPath);
            if (destDir == null) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "directory not exist:path=" + dirPath);
            }
            return destDir;
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "get directory failed:wsName=" + wsName + ",directoryPath=" + dirPath, e);
        }
    }

    @Override
    public BSONObject getDirInfoByPath(ScmUser user, String wsName, String dirPath)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriority(user, wsName, dirPath,
                ScmPrivilegeDefine.READ, "get dir path info by path");
        BSONObject destDir = getDirInfoByPath(wsName, dirPath);
        String authMessage = "get dir info  by path= " + dirPath;
        audit.info(ScmAuditType.DIR_DQL, user, wsName, 0, authMessage + ", dirInfo=" + destDir);
        return destDir;
    }

    public String getDirPathById(String wsName, String dirId) throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to get directory path, directory feature is disable:ws=" + ws.getName()
                            + ", id=" + dirId);
        }
        try {
            return DirOperator.getInstance().getPathById(ws, dirId);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "get directory path failed:wsName=" + wsName + ",directoryId=" + dirId, e);
        }
    }

    @Override
    public String getDirPathById(ScmUser user, String wsName, String dirId)
            throws ScmServerException {
        String path = getDirPathById(wsName, dirId);
        ScmFileServicePriv.getInstance().checkDirPriority(user, wsName, path,
                ScmPrivilegeDefine.READ, "get dir path by id");
        audit.info(ScmAuditType.DIR_DQL, user, wsName, 0,
                "get dir path by dirId=" + dirId + ", dirPath=" + path);
        return path;
    }

    @Override
    public MetaCursor getDirList(ScmUser user, String wsName, BSONObject condition,
            BSONObject orderby, long skip, long limit) throws ScmServerException {
        audit.info(ScmAuditType.DIR_DQL, user, wsName, 0,
                "list directory, condition=" + condition.toString());
        try {
            ScmContentModule contentModule = ScmContentModule.getInstance();
            ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoCheckLocalSite(wsName);
            if (!ws.isEnableDirectory()) {
                throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                        "failed to list directory, directory feature is disable:ws=" + ws.getName()
                                + ", condition=" + condition);
            }
            MetaDirAccessor dirAccessor = contentModule.getMetaService().getMetaSource()
                    .getDirAccessor(wsName);
            return dirAccessor.query(condition, null, orderby, skip, limit);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "list directory failed:ws=" + wsName + ",condition=" + condition, e);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "list directory failed:ws=" + wsName + ",condition=" + condition, e);
        }
    }

    @Override
    public void deleteDir(ScmUser user, String wsName, String id, String path)
            throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to delete directory, directory feature is disable:ws=" + ws.getName()
                            + ", id=" + id + ", path=" + path);
        }
        if (path != null) {
            ScmFileServicePriv.getInstance().checkDirPriority(user, wsName, path,
                    ScmPrivilegeDefine.DELETE, "delete dir by path");
        }
        else {
            ScmFileServicePriv.getInstance().checkDirPriorityById(user, wsName, id,
                    ScmPrivilegeDefine.DELETE, "delete dir by id");
        }

        try {
            DireDeletorDao dao = new DireDeletorDao(wsName, id, path);
            dao.delete();
            audit.info(ScmAuditType.DELETE_DIR, user, wsName, 0,
                    "delete dir: dirPath=" + path + ", dirId=" + id);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            String msg = "delete diretory failed:id=" + id + ",path=" + path + ",error=" + e;
            throw new ScmSystemException(msg, e);
        }
    }

    @Override
    public BSONObject createDirByPath(ScmUser user, String wsName, String path)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriority(user, wsName, path,
                ScmPrivilegeDefine.CREATE, "create dir by path");
        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to create directory, directory feature is disable:ws=" + ws.getName()
                            + ", path=" + path);
        }
        try {
            DirCreatorDao dao = new DirCreatorDao(user.getUsername(), wsName);
            BSONObject dirInfo = dao.createDirByPath(path);
            audit.info(ScmAuditType.CREATE_DIR, user, wsName, 0,
                    "create dir by dirPath=" + path + ", newDir=" + dirInfo.toString());
            return dirInfo;
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            String msg = "create diretory failed:path=" + path + ",error=" + e;
            throw new ScmSystemException(msg, e);
        }
    }

    @Override
    public BSONObject createDirByPidAndName(ScmUser user, String wsName, String name,
            String parentID) throws ScmServerException {

        ScmFileServicePriv.getInstance().checkDirPriorityById(user, wsName, parentID,
                ScmPrivilegeDefine.CREATE, "create dir by parentIdAndName");

        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to create directory, directory feature is disable:ws=" + ws.getName()
                            + ", name=" + name + ", parentId=" + parentID);
        }
        try {
            if (!ScmArgChecker.Directory.checkDirectoryName(name)) {
                throw new ScmInvalidArgumentException("invalid directory name:name=" + name);
            }

            DirCreatorDao dao = new DirCreatorDao(user.getUsername(), wsName);
            BSONObject dirInfo = dao.createDirByPidAndName(parentID, name);
            audit.info(ScmAuditType.CREATE_DIR, user, wsName, 0, "create dir by parentId="
                    + parentID + " and dirName=" + name + ", newDir=" + dirInfo.toString());
            return dirInfo;
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            String msg = "create diretory failed:name=" + name + ",parentId=" + parentID + ",error="
                    + e;
            throw new ScmSystemException(msg, e);
        }
    }

    @Override
    public long reanmeDirById(ScmUser user, String wsName, String dirId, String newName)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriorityById(user, wsName, dirId,
                ScmPrivilegeDefine.DELETE, "delete source when rename dir by id");
        ScmFileServicePriv.getInstance().checkDirPriorityByOldIdAndNewName(user, wsName, this,
                dirId, newName, ScmPrivilegeDefine.CREATE, "create target when rename dir by id");
        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to rename directory, directory feature is disable:ws=" + ws.getName()
                            + ", id=" + dirId + ", newName=" + newName);
        }
        try {
            if (!ScmArgChecker.Directory.checkDirectoryName(newName)) {
                throw new ScmInvalidArgumentException("invalid directory name:name=" + newName);
            }

            BSONObject updator = new BasicBSONObject();
            updator.put(FieldName.FIELD_CLDIR_NAME, newName);
            DirUpdatorDao dao = new DirUpdatorDao(user.getUsername(), wsName, updator);
            long time = dao.updateById(dirId);
            audit.info(ScmAuditType.UPDATE_DIR, user, wsName, 0,
                    "rename dir by dirId=" + dirId + ", update_time=" + time);
            return time;
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            String msg = "rename diretory failed:dirId=" + dirId + ",newName=" + newName + ",error="
                    + e;
            throw new ScmSystemException(msg, e);
        }
    }

    @Override
    public long renameDirByPath(ScmUser user, String wsName, String dirPath, String newName)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriority(user, wsName, dirPath,
                ScmPrivilegeDefine.DELETE, "delete source when rename dir by path");
        ScmFileServicePriv.getInstance().checkDirPriorityByOldDirAndNewName(user, wsName, dirPath,
                newName, ScmPrivilegeDefine.CREATE, "create target when rename dir by path");

        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to rename directory, directory feature is disable:ws=" + ws.getName()
                            + ", dir=" + dirPath + ", newName=" + newName);
        }
        try {
            if (!ScmArgChecker.Directory.checkDirectoryName(newName)) {
                throw new ScmInvalidArgumentException("invalid directory name:name=" + newName);
            }

            BSONObject updator = new BasicBSONObject();
            updator.put(FieldName.FIELD_CLDIR_NAME, newName);
            DirUpdatorDao dao = new DirUpdatorDao(user.getUsername(), wsName, updator);
            long time = dao.updateByPath(dirPath);
            audit.info(ScmAuditType.UPDATE_DIR, user, wsName, 0,
                    "rename dir by dirPath=" + dirPath + ", update_time=" + time);
            return time;
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            String msg = "rename diretory failed:dirPath=" + dirPath + ",newName=" + newName
                    + ",error=" + e;
            throw new ScmSystemException(msg, e);
        }
    }

    @Override
    public long moveDirById(ScmUser user, String wsName, String dirId, String newParentId,
            String newParentPath) throws ScmServerException {
        if (null != newParentId) {
            ScmFileServicePriv.getInstance().checkDirPriorityByOldIdAndNewParentId(user, wsName,
                    this, dirId, newParentId, ScmPrivilegeDefine.CREATE,
                    "create target when move dir by id");
        }
        else {
            ScmFileServicePriv.getInstance().checkDirPriorityByOldIdAndNewParentDir(user, wsName,
                    this, dirId, newParentPath, ScmPrivilegeDefine.CREATE,
                    "create target when move dir by path");
        }
        ScmFileServicePriv.getInstance().checkDirPriorityById(user, wsName, dirId,
                ScmPrivilegeDefine.DELETE, "delete source when move dir by id");
        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to move directory, directory feature is disable:ws=" + ws.getName()
                            + ", dirId=" + dirId + ", newParentPath=" + newParentPath
                            + ", newParentId=" + newParentId);
        }
        try {
            DirUpdatorDao dao = createDirUpdatorDao(wsName, newParentId, newParentPath,
                    user.getUsername());
            long time = dao.updateById(dirId);
            audit.info(ScmAuditType.UPDATE_DIR, user, wsName, 0,
                    "move dir by dirId=" + dirId + ", update_time=" + time + ", parentId="
                            + newParentId + ", parentPath=" + newParentPath);
            return time;
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            String msg = "move diretory failed:dirId=" + dirId + ",newParentId=" + newParentId
                    + ",newParentPath=" + newParentPath + ",error=" + e;
            throw new ScmSystemException(msg, e);
        }
    }

    @Override
    public long moveDirByPath(ScmUser user, String wsName, String dirPath, String newParentId,
            String newParentPath) throws ScmServerException {
        if (newParentId != null) {
            ScmFileServicePriv.getInstance().checkDirPriorityByOldDirAndNewParentId(user, wsName,
                    this, dirPath, newParentId, ScmPrivilegeDefine.CREATE,
                    "create target when move dir by id");
        }
        else {
            ScmFileServicePriv.getInstance().checkDirPriorityByOldDirAndNewParentDir(user, wsName,
                    dirPath, newParentPath, ScmPrivilegeDefine.CREATE,
                    "create target when move dir by path");
        }

        ScmFileServicePriv.getInstance().checkDirPriority(user, wsName, dirPath,
                ScmPrivilegeDefine.DELETE, "delete source when move dir by path");
        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to move directory, directory feature is disable:ws=" + ws.getName()
                            + ", dir=" + dirPath + ", newParentPath=" + newParentPath
                            + ", newParentId=" + newParentId);
        }
        try {
            DirUpdatorDao dao = createDirUpdatorDao(wsName, newParentId, newParentPath,
                    user.getUsername());
            long time = dao.updateByPath(dirPath);
            audit.info(ScmAuditType.UPDATE_DIR, user, wsName, 0,
                    "move dir by dirPath=" + dirPath + ", update_time=" + time + ", parentId="
                            + newParentId + ", parentPath=" + newParentPath);
            return time;
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (Exception e) {
            String msg = "move diretory failed:dirPath=" + dirPath + ",newParentId=" + newParentId
                    + ",newParentPath=" + newParentPath + ",error=" + e;
            throw new ScmSystemException(msg, e);
        }
    }

    private DirUpdatorDao createDirUpdatorDao(String wsName, String newParentId,
            String newParentPath, String user) throws ScmServerException {
        BSONObject updator = new BasicBSONObject();
        if (newParentId != null) {
            updator.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, newParentId);
        }
        else if (newParentPath != null) {
            updator.put(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_PATH, newParentPath);
        }
        else {
            throw new ScmInvalidArgumentException("newParentId=null,newParentPath=null");
        }
        DirUpdatorDao dao = new DirUpdatorDao(user, wsName, updator);
        return dao;
    }

    @Override
    public long countDir(ScmUser user, String wsName, BSONObject condition)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, wsName,
                ScmPrivilegeDefine.LOW_LEVEL_READ, "count directory");
        ScmWorkspaceInfo ws = ScmContentModule.getInstance().getWorkspaceInfoCheckLocalSite(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to count directory, directory feature is disable:ws=" + ws.getName()
                            + ", condition=" + condition);
        }
        ScmContentModule contentModule = ScmContentModule.getInstance();
        contentModule.getWorkspaceInfoCheckLocalSite(wsName);
        long count = contentModule.getMetaService().getDirCount(wsName, condition);
        String message = "count directory";
        if (null != condition) {
            message += " by condition=" + condition.toString();
        }
        audit.info(ScmAuditType.DIR_DQL, user, wsName, 0, message);
        return count;
    }

    @Override
    public String generateId(Date dirCreateTime) throws ScmServerException {
        return ScmIdGenerator.DirectoryId.get();
    }

    @Override
    public FileMeta createFile(ScmUser user, String ws, String parentDirId, FileMeta fileInfo,
            InputStream data, FileUploadConf conf) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(ws);
        if (!wsInfo.isEnableDirectory()) {
            throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                    "directory feature is disabled: workspace=" + ws);
        }

        checkPrivForCreateFile(user, ws, parentDirId, conf);
        fileInfo.setDirId(parentDirId);
        FileMeta fileMeta = fileService.createFile(ws, fileInfo, conf, data);
        audit.info(ScmAuditType.CREATE_FILE, user, ws, 0,
                "create file , fileId=" + fileMeta.getId() + ", fileName=" + fileMeta.getName());
        return fileMeta;
    }

    private void checkPrivForCreateFile(ScmUser user, String ws, String parentDirId,
            FileUploadConf conf) throws ScmServerException {
        if (conf.getExistStrategy() == FileExistStrategy.OVERWRITE) {
            ScmFileServicePriv.getInstance().checkDirPriorityById(user, ws, parentDirId,
                    ScmPrivilegeDefine.CREATE.getFlag() | ScmPrivilegeDefine.DELETE.getFlag()
                            | ScmPrivilegeDefine.UPDATE.getFlag(),
                    "overwrite file for delete and create");
        }
        else {
            ScmFileServicePriv.getInstance().checkDirPriorityById(user, ws, parentDirId,
                    ScmPrivilegeDefine.CREATE, "create file");
        }
    }

    @Override
    public FileMeta createFile(ScmUser user, String ws, String parentDirId, FileMeta fileInfo,
            String breakpointFile, FileUploadConf conf) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo wsInfo = contentModule.getWorkspaceInfoCheckExist(ws);
        if (!wsInfo.isEnableDirectory()) {
            throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                    "directory feature is disabled: workspace=" + ws);
        }
        checkPrivForCreateFile(user, ws, parentDirId, conf);
        fileInfo.setDirId(parentDirId);
        FileMeta fileMeta = fileService.createFile(ws, fileInfo, conf, breakpointFile);
        audit.info(ScmAuditType.CREATE_FILE, user, ws, 0, "create file , fileId=" + fileMeta.getId()
                + ", fileName=" + fileMeta.getName() + ", breakpointFile=" + breakpointFile);
        return fileMeta;
    }

    @Override
    public FileMeta getFileInfoByPath(ScmUser user, String workspaceName, String filePath,
            int majorVersion, int minorVersion, boolean acceptDeleteMarker)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriority(user, workspaceName, filePath,
                ScmPrivilegeDefine.READ, "get file by path");
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoCheckLocalSite(workspaceName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to get file, directory is disable:ws=" + workspaceName + ", filePath="
                            + filePath);
        }
        String fileName = ScmSystemUtils.basename(filePath);
        String parentDirPath = ScmSystemUtils.dirname(filePath);
        BSONObject fileInfo = null;
        try {
            BSONObject parentDir = getDirInfoByPath(workspaceName, parentDirPath);
            String parentDirId = (String) parentDir.get(FieldName.FIELD_CLDIR_ID);
            fileInfo = contentModule.getMetaService().getFileInfo(ws, parentDirId, fileName,
                    majorVersion, minorVersion, acceptDeleteMarker);
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

        FileMeta fileMeta = fileMetaFactory.createFileMetaByRecord(ws.getName(), fileInfo);
        audit.info(ScmAuditType.FILE_DQL, user, workspaceName, 0,
                "get file by filePath=" + filePath + ", fileName=" + fileMeta.getName());
        return fileMeta;
    }

    @Override
    public FileMeta moveFile(ScmUser user, String ws, String moveToDirId, String fileId,
            ScmVersion returnVersion) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkFilePriorityByFileId(user, ws, fileService, fileId,
                -1, -1, ScmPrivilegeDefine.UPDATE, "move file");

        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance().getWorkspaceInfoCheckExist(ws);

        if (!wsInfo.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to move file, directory is disable:ws=" + ws + ", fileId=" + fileId);
        }

        ScmLock dirLock = null;
        ScmLock fileLock = null;
        UpdateFileMetaResult ret;
        OperationCompleteCallback callback;
        try {
            ScmLockPath fileLockPath = ScmLockPathFactory.createFileLockPath(ws, fileId);
            fileLock = ScmLockManager.getInstance().acquiresWriteLock(fileLockPath);
            if (!moveToDirId.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
                ScmLockPath dirLockPath = ScmLockPathFactory.createDirLockPath(ws, moveToDirId);
                dirLock = readLock(dirLockPath);
            }

            BSONObject currentLatestVersionBSON = ScmContentModule.getInstance().getMetaService()
                    .getFileInfo(wsInfo.getMetaLocation(), wsInfo.getName(), fileId, -1, -1);
            if (currentLatestVersionBSON == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "file not found: ws=" + wsInfo.getName() + ", fileId=" + fileId);
            }
            checkExistDir(ws, BsonUtils.getStringChecked(currentLatestVersionBSON,
                    FieldName.FIELD_CLFILE_NAME), moveToDirId);

            BSONObject parentDirMatcher = new BasicBSONObject();
            parentDirMatcher.put(FieldName.FIELD_CLDIR_ID, moveToDirId);

            if (ScmContentModule.getInstance().getMetaService().getDirCount(ws,
                    parentDirMatcher) <= 0) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "parent directory not exist:id=" + moveToDirId);
            }
            FileMeta fileMeta = fileMetaFactory.createFileMetaByRecord(ws,
                    currentLatestVersionBSON);
            ret = fileMetaOperator.updateFileMeta(ws, fileId,
                    Collections.singletonList(FileMetaDefaultUpdater
                            .globalFieldUpdater(FieldName.FIELD_CLFILE_DIRECTORY_ID, moveToDirId)),
                    user.getUsername(), new Date(), fileMeta, returnVersion);
            callback = listenerMgr.postUpdate(wsInfo, fileMeta, ret.getLatestVersionAfterUpdate());
        }
        finally {
            unlock(dirLock);
            unlock(fileLock);
        }
        audit.info(ScmAuditType.FILE_DML, user, ws, 0,
                "move file fileId=" + fileId + ", moveToDir=" + moveToDirId);
        callback.onComplete();
        return ret.getSpecifiedReturnVersion();
    }

    private void unlock(ScmLock lock) {
        if (lock != null) {
            lock.unlock();
        }
    }

    private void checkExistDir(String ws, String name, String parentDirId)
            throws ScmServerException {
        BSONObject existDirMatcher = new BasicBSONObject();
        existDirMatcher.put(FieldName.FIELD_CLDIR_NAME, name);
        existDirMatcher.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, parentDirId);
        long dirCount = ScmContentModule.getInstance().getMetaService().getDirCount(ws,
                existDirMatcher);
        if (dirCount > 0) {
            throw new ScmServerException(ScmError.DIR_EXIST,
                    "a directory with the same name exists:name=" + name + ",parentDirectoryId="
                            + parentDirId);
        }
    }

    private ScmLock readLock(ScmLockPath lockPath) throws ScmServerException {
        return ScmLockManager.getInstance().acquiresReadLock(lockPath);
    }

    @Override
    public FileMeta moveFileByPath(ScmUser user, String ws, String moveToDirPath, String fileId,
            ScmVersion returnVersion) throws ScmServerException {
        BSONObject dirId = getDirInfoByPath(ws, moveToDirPath);
        return moveFile(user, ws, BsonUtils.getStringChecked(dirId, FieldName.FIELD_CLDIR_ID),
                fileId, returnVersion);
    }
}
