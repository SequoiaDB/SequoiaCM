package com.sequoiacm.contentserver.service.impl;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.contentserver.dao.DirCreatorDao;
import com.sequoiacm.contentserver.dao.DirOperator;
import com.sequoiacm.contentserver.dao.DirUpdatorDao;
import com.sequoiacm.contentserver.dao.DireDeletorDao;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.service.IDirService;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaDirAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;

@Service
public class DirServiceImpl implements IDirService {
    private static final Logger logger = LoggerFactory.getLogger(DirServiceImpl.class);

    @Override
    public BSONObject getDirInfoById(String wsName, String dirId) throws ScmServerException {
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

    @Override
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
    public String getDirPathById(String wsName, String dirId) throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to get directory path, directory feature is disable:ws=" + ws.getName()
                            + ", id=" + dirId);
        }
        try {
            String path = DirOperator.getInstance().getPathById(ws, dirId);
            return path;
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
    public MetaCursor getDirList(String wsName, BSONObject condition, BSONObject orderby, long skip,
            long limit) throws ScmServerException {
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
    public void deleteDir(String wsName, String id, String path) throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to delete directory, directory feature is disable:ws=" + ws.getName()
                            + ", id=" + id + ", path=" + path);
        }
        try {
            DireDeletorDao dao = new DireDeletorDao(wsName, id, path);
            dao.delete();
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
    public BSONObject createDirByPath(String user, String wsName, String path)
            throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to create directory, directory feature is disable:ws=" + ws.getName()
                            + ", path=" + path);
        }
        try {
            DirCreatorDao dao = new DirCreatorDao(user, wsName);
            return dao.createDirByPath(path);
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
    public BSONObject createDirByPidAndName(String user, String wsName, String name,
            String parentID) throws ScmServerException {
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

            DirCreatorDao dao = new DirCreatorDao(user, wsName);
            return dao.createDirByPidAndName(parentID, name);
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
    public long reanmeDirById(String user, String wsName, String dirId, String newName)
            throws ScmServerException {
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
            DirUpdatorDao dao = new DirUpdatorDao(user, wsName, updator);
            return dao.updateById(dirId);
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
    public long renameDirByPath(String user, String wsName, String dirPath, String newName)
            throws ScmServerException {
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
            DirUpdatorDao dao = new DirUpdatorDao(user, wsName, updator);
            return dao.updateByPath(dirPath);
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
    public long moveDirById(String user, String wsName, String dirId, String newParentId,
            String newParentPath) throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to move directory, directory feature is disable:ws=" + ws.getName()
                            + ", dirId=" + dirId + ", newParentPath=" + newParentPath
                            + ", newParentId=" + newParentId);
        }
        try {
            DirUpdatorDao dao = createDirUpdatorDao(wsName, newParentId, newParentPath, user);
            return dao.updateById(dirId);
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
    public long moveDirByPath(String user, String wsName, String dirPath, String newParentId,
            String newParentPath) throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to move directory, directory feature is disable:ws=" + ws.getName()
                            + ", dir=" + dirPath + ", newParentPath=" + newParentPath
                            + ", newParentId=" + newParentId);
        }
        try {
            DirUpdatorDao dao = createDirUpdatorDao(wsName, newParentId, newParentPath, user);
            return dao.updateByPath(dirPath);
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
    public long countDir(String wsName, BSONObject condition) throws ScmServerException {
        ScmWorkspaceInfo ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
        if (!ws.isEnableDirectory()) {
            throw new ScmServerException(ScmError.DIR_FEATURE_DISABLE,
                    "failed to count directory, directory feature is disable:ws=" + ws.getName()
                            + ", condition=" + condition);
        }
        ScmContentServer contentserver = ScmContentServer.getInstance();
        contentserver.getWorkspaceInfoChecked(wsName);
        return contentserver.getMetaService().getDirCount(wsName, condition);
    }
}
