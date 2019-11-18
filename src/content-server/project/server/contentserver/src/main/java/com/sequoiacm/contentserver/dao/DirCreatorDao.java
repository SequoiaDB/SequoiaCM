package com.sequoiacm.contentserver.dao;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmLockException;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.ScmMetasourceException;

public class DirCreatorDao {
    private static final Logger logger = LoggerFactory.getLogger(DirCreatorDao.class);
    private ScmWorkspaceInfo ws;
    private String user;

    public DirCreatorDao(String user, String wsName) throws ScmServerException {
        this.user = user;
        this.ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
    }

    public BSONObject createDirByPidAndName(String parentId, String name) throws ScmServerException {
        BSONObject dirInfo = new BasicBSONObject();
        dirInfo.put(FieldName.FIELD_CLDIR_NAME, name);
        dirInfo.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, parentId);
        addExtraField(dirInfo);
        insertDir(dirInfo);
        return dirInfo;
    }

    private void insertDir(BSONObject dirInfo) throws ScmServerException {
        ScmMetaService metaService = ScmContentServer.getInstance().getMetaService();
        String parentID = (String) dirInfo.get(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID);
        String dirName = (String) dirInfo.get(FieldName.FIELD_CLDIR_NAME);

        BSONObject parentDirMatcher = new BasicBSONObject();
        parentDirMatcher.put(FieldName.FIELD_CLDIR_ID, parentID);

        BSONObject existFileMatcher = new BasicBSONObject();
        existFileMatcher.put(FieldName.FIELD_CLREL_DIRECTORY_ID, parentID);
        existFileMatcher.put(FieldName.FIELD_CLREL_FILENAME, dirName);

        long count = 0;
        try {
            count = ScmContentServer.getInstance().getMetaService().getMetaSource()
                    .getRelAccessor(ws.getName(), null).count(existFileMatcher);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "Failed to count file: matcher"
                    + existFileMatcher, e);
        }
        if (count > 0) {
            throw new ScmServerException(ScmError.FILE_EXIST,
                    "a file with the same name exists:parentDirectory=" + parentID + ",fileName="
                            + dirName);
        }

        ScmLockPath lockPath = ScmLockPathFactory.createDirLockPath(ws.getName(), parentID);
        ScmLock rLock = null;

        if (!parentID.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            rLock = readLock(lockPath);
        }
        try {
            if (metaService.getDirCount(ws.getName(), parentDirMatcher) <= 0) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "parent directory not exist:id=" + parentID);
            }
            metaService.insertDir(ws.getName(), dirInfo);
        }
        finally {
            if (!parentID.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
                unlock(rLock, lockPath);
            }
        }
    }

    private void unlock(ScmLock rLock, ScmLockPath path) {
        try {
            if (rLock != null) {
                rLock.unlock();
            }
        }
        catch (Exception e) {
            logger.error("failed to unlock:path={}", path, e);
        }
    }

    private ScmLock readLock(ScmLockPath lockPath) throws ScmServerException {
        try {
            return ScmLockManager.getInstance().acquiresReadLock(lockPath);
        }
        catch (Exception e) {
            throw new ScmLockException("failed to lock:path=" + lockPath, e);
        }
    }

    private void addExtraField(BSONObject dirInfo) throws ScmSystemException {
        String id = ScmIdGenerator.DirectoryId.get();
        long timestamp = System.currentTimeMillis();
        dirInfo.put(FieldName.FIELD_CLDIR_ID, id);
        dirInfo.put(FieldName.FIELD_CLDIR_USER, user);
        dirInfo.put(FieldName.FIELD_CLDIR_UPDATE_USER, user);

        dirInfo.put(FieldName.FIELD_CLDIR_CREATE_TIME, timestamp);
        dirInfo.put(FieldName.FIELD_CLDIR_UPDATE_TIME, timestamp);
    }

}
