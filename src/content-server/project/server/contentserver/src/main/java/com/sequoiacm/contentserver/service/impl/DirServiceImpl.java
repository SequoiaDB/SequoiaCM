package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.contentserver.dao.DirCreatorDao;
import com.sequoiacm.contentserver.dao.DirOperator;
import com.sequoiacm.contentserver.dao.DirUpdatorDao;
import com.sequoiacm.contentserver.dao.DireDeletorDao;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaDirAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DirServiceImpl implements IDirService {
    private static final Logger logger = LoggerFactory.getLogger(DirServiceImpl.class);

    @Autowired
    private ScmAudit audit;

    @Override
    public BSONObject getDirInfoById(ScmUser user, String wsName, String dirId)
            throws ScmServerException {
        ScmFileServicePriv.getInstance().checkDirPriorityById(user, wsName, this, dirId,
                ScmPrivilegeDefine.READ, "get dir path info by id");
        try {
            ScmContentServer contentserver = ScmContentServer.getInstance();
            ScmWorkspaceInfo ws = contentserver.getWorkspaceInfoChecked(wsName);
            if (!ws.isEnableDirectory()) {
                throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                        "failed to get directory, directory feature is disable:ws=" + ws.getName()
                                + ", id=" + dirId);
            }
            BSONObject destDir = contentserver.getMetaService().getDirInfo(wsName, dirId);
            if (destDir == null) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "directory not exist:id=" + dirId);
            }
            audit.info(ScmAuditType.DIR_DQL, user, wsName, 0,
                    "get dir info by id=" + dirId + ", dir info=" + destDir);
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
            ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
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
        audit.info(ScmAuditType.DIR_DQL, user, wsName, 0, authMessage + ", dir info=" + destDir);
        return destDir;
    }

    public String getDirPathById(String wsName, String dirId) throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
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
                "get dir path by dir id=" + dirId + ", path=" + path);
        return path;
    }

    @Override
    public MetaCursor getDirList(ScmUser user, String wsName, BSONObject condition,
            BSONObject orderby, long skip, long limit) throws ScmServerException {
        audit.info(ScmAuditType.DIR_DQL, user, wsName, 0,
                "list directory, condition=" + condition.toString());
        try {
            ScmContentServer cs = ScmContentServer.getInstance();
            ScmWorkspaceInfo ws = cs.getWorkspaceInfoChecked(wsName);
            if (!ws.isEnableDirectory()) {
                throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                        "failed to list directory, directory feature is disable:ws=" + ws.getName()
                                + ", condition=" + condition);
            }
            MetaDirAccessor dirAccessor = cs.getMetaService().getMetaSource()
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
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
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
            ScmFileServicePriv.getInstance().checkDirPriorityById(user, wsName, this, id,
                    ScmPrivilegeDefine.DELETE, "delete dir by id");
        }

        try {
            DireDeletorDao dao = new DireDeletorDao(wsName, id, path);
            dao.delete();
            audit.info(ScmAuditType.DELETE_DIR, user, wsName, 0,
                    "delete dir: dirPath=" + path + ", id=" + id);
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
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to create directory, directory feature is disable:ws=" + ws.getName()
                            + ", path=" + path);
        }
        try {
            DirCreatorDao dao = new DirCreatorDao(user.getUsername(), wsName);
            BSONObject dirInfo = dao.createDirByPath(path);
            audit.info(ScmAuditType.CREATE_DIR, user, wsName, 0,
                    "create dir by path=" + path + ", newDir=" + dirInfo.toString());
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

        ScmFileServicePriv.getInstance().checkDirPriorityById(user, wsName, this, parentID,
                ScmPrivilegeDefine.CREATE, "create dir by parentIdAndName");

        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
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
                    + parentID + " and Name=" + name + ", newDir=" + dirInfo.toString());
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
        ScmFileServicePriv.getInstance().checkDirPriorityById(user, wsName, this, dirId,
                ScmPrivilegeDefine.DELETE, "delete source when rename dir by id");
        ScmFileServicePriv.getInstance().checkDirPriorityByOldIdAndNewName(user, wsName, this,
                dirId, newName, ScmPrivilegeDefine.CREATE, "create target when rename dir by id");
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
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

        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
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
        ScmFileServicePriv.getInstance().checkDirPriorityById(user, wsName, this, dirId,
                ScmPrivilegeDefine.DELETE, "delete source when move dir by id");
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
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
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
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
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to count directory, directory feature is disable:ws=" + ws.getName()
                            + ", condition=" + condition);
        }
        ScmContentServer contentserver = ScmContentServer.getInstance();
        contentserver.getWorkspaceInfoChecked(wsName);
        long count = contentserver.getMetaService().getDirCount(wsName, condition);
        String message = "count direcotry";
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
}
