package com.sequoiacm.contentserver.dao;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.MetaDirAccessor;
import com.sequoiacm.metasource.MetaRelAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;

public class DireDeletorDao {
    // private static final Logger logger =
    // LoggerFactory.getLogger(DireDeletorDao.class);
    private static final Logger logger = LoggerFactory.getLogger(DireDeletorDao.class);
    private ScmWorkspaceInfo ws;
    private ScmMetaService metaService;
    private String id;
    private String path;

    public DireDeletorDao(String wsName, String dirId, String path) throws ScmServerException {
        ScmContentServer contentServer = ScmContentServer.getInstance();
        this.id = dirId;
        this.path = path;
        this.ws = contentServer.getWorkspaceInfoChecked(wsName);
        this.metaService = contentServer.getMetaService();
    }

    public void delete() throws ScmServerException {
        if (id == null) {
            if (path == null) {
                throw new ScmInvalidArgumentException("missing required field:id=null,path=null");
            }
            BSONObject dir = metaService.getDirByPath(ws.getName(), path);
            if (dir == null) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND, "directory not exists:ws=" + ws.getName() + ",path=" + path);
            }
            id = (String) dir.get(FieldName.FIELD_CLDIR_ID);
        }
        else {
            BSONObject dir = metaService.getDirInfo(ws.getName(), id);
            if (dir == null) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND, "directory not exists:ws=" + ws.getName() + ",id=" + id);
            }
        }

        if (id.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            throw new ScmOperationUnsupportedException("can not delete root directory:id=" + id);
        }

        MetaDirAccessor dirAccessor = metaService.getMetaSource().getDirAccessor(ws.getName());
        MetaRelAccessor relAccessor = metaService.getMetaSource()
                .getRelAccessor(ws.getName(), null);

        BSONObject subFileMatcher = new BasicBSONObject();
        subFileMatcher.put(FieldName.FIELD_CLREL_DIRECTORY_ID, id);

        BSONObject subDirMatcher = new BasicBSONObject();
        subDirMatcher.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, id);

        ScmLockPath lockPath = ScmLockPathFactory.createDirLockPath(ws.getName(), id);
        ScmLock wLock = writeLock(lockPath);
        try {
            if (dirAccessor.count(subDirMatcher) > 0 || relAccessor.count(subFileMatcher) > 0) {
                throw new ScmServerException(ScmError.DIR_NOT_EMPTY, "directory is not empty:id="
                        + id);
            }
            dirAccessor.delete(id);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "Failed to delete dir: " + id, e);
        }
        finally {
            unlock(wLock, lockPath);
        }
    }

    private void unlock(ScmLock wLock, ScmLockPath lockPath) {
        try {
            if (wLock != null) {
                wLock.unlock();
            }
        }
        catch (Exception e) {
            logger.warn("failed to unlock:path={}", lockPath, e);
        }
    }

    private ScmLock writeLock(ScmLockPath path) throws ScmServerException {
        return ScmLockManager.getInstance().acquiresWriteLock(path);
    }

}
