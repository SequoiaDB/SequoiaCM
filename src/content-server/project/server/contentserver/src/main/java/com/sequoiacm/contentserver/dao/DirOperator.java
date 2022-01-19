package com.sequoiacm.contentserver.dao;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.cache.ScmDirCache;
import com.sequoiacm.contentserver.cache.ScmDirCacheInfo;
import com.sequoiacm.contentserver.cache.ScmDirPath;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.exception.ScmLockException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaDirAccessor;
import com.sequoiacm.metasource.MetaRelAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;

public class DirOperator {
    private static final Logger logger = LoggerFactory.getLogger(DirOperator.class);
    private static final int CACHE_DIR_LEVEL = 3;
    private static DirOperator dirOperator = new DirOperator();

    private DirOperator() {
    }

    public static DirOperator getInstance() {
        return dirOperator;
    }

    private boolean enableCache() {
        return PropertiesUtils.enableDirCache();
    }

    private ScmDirCache getCache(ScmWorkspaceInfo ws) {
        return ws.getDirCache();
    }

    public BSONObject getDirByPath(ScmWorkspaceInfo ws, String dirPath) throws ScmServerException {
        DirInfo dirInfo = _getDirByPath(ws, dirPath);
        if (dirInfo != null) {
            return dirInfo.getDirBson();
        }
        return null;
    }

    public DirInfo getDirAndVersionByPath(ScmWorkspaceInfo ws, String dirPath)
            throws ScmServerException {
        DirInfo dirInfo = _getDirByPath(ws, dirPath);
        if (dirInfo != null && dirInfo.getDirBson() == null) {
            return null;
        }
        return dirInfo;
    }

    private DirInfo _getDirByPath(ScmWorkspaceInfo ws, String dirPathStr)
            throws ScmServerException {
        ScmDirPath dirPath = new ScmDirPath(dirPathStr);
        ScmMetaService metaService = ScmContentModule.getInstance().getMetaService();
        // no query cache
        int currentDirLevel = dirPath.getLevel();
        if (!enableCache() || dirPath.isRootDir() || currentDirLevel < CACHE_DIR_LEVEL) {
            return new DirInfo(metaService.getDirByPath(ws.getName(), dirPath),
                    ScmDirCache.DEFAULT_VERSION);
        }
        ScmDirCache dirCache = getCache(ws);
        String parentId = null;
        long firstVersion = ScmDirCache.DEFAULT_VERSION;
        String currentPath = dirPath.getPath();
        // query path in cache
        while (currentDirLevel >= CACHE_DIR_LEVEL) {
            ScmDirCacheInfo cacheDirInfo = dirCache.getDirByPath(currentPath);
            if (cacheDirInfo == null) {
                currentDirLevel--;
                currentPath = dirPath.getPathByLevel(currentDirLevel);
                continue;
            }
            DirInfo dbDirInfo = getDirInfoById(metaService, dirCache.getWsName(),
                    cacheDirInfo.getId());

            if (dbDirInfo.getVersion() == cacheDirInfo.getVersion()) {
                if (dbDirInfo.getDirBson() == null) {
                    return null;
                }

                if (currentDirLevel == dirPath.getLevel()) {
                    String dirName = (String) dbDirInfo.getDirBson()
                            .get(FieldName.FIELD_CLDIR_NAME);
                    // when rename dir,update meta first and update version
                    // later.
                    // expect /a/b/c , actual /a/b/d.
                    if (dirPath.getBaseName().equals(dirName)) {
                        return dbDirInfo;
                    }
                    return null;
                }
                // query parent path in cache,get child dir level and parentId
                parentId = (String) dbDirInfo.getDirBson().get(FieldName.FIELD_CLDIR_ID);
                currentDirLevel = currentDirLevel + 1;
                firstVersion = cacheDirInfo.getVersion();
                break;
            }
            else if (dbDirInfo.getVersion() > cacheDirInfo.getVersion()) {
                dirCache.checkClear(dbDirInfo.getVersion());
                firstVersion = cacheDirInfo.getVersion();
                break;
            }
            // dbversion < cacheVersion
            dirCache.checkClear(dbDirInfo.getVersion());
            throw new ScmSystemException("cache version greater than db version: cacheVersion="
                    + cacheDirInfo.getVersion() + ", dbVersion=" + dbDirInfo.getVersion());
        }

        // not query parent path in cache
        if (parentId == null) {
            parentId = CommonDefine.Directory.SCM_ROOT_DIR_ID;
            // start dir level is 2, not query root dir
            currentDirLevel = 2;
        }
        return getDirFromMetaSource(metaService, dirCache, dirPath, currentDirLevel, parentId,
                firstVersion);
    }

    private DirInfo getDirFromMetaSource(ScmMetaService metaService, ScmDirCache dirCache,
            ScmDirPath tragetDirPath, int dirLevel, String parentId, long firstVersion)
            throws ScmServerException {
        BSONObject dirBson = null;
        while (dirLevel <= tragetDirPath.getLevel()) {
            String dirName = tragetDirPath.getNamebyLevel(dirLevel);
            if (firstVersion == ScmDirCache.DEFAULT_VERSION) {
                firstVersion = getDBVersion(metaService, dirCache.getWsName());
            }
            dirBson = metaService.getDirInfo(dirCache.getWsName(), parentId, dirName);

            if (dirBson == null) {
                return null;
            }

            String dirId = (String) dirBson.get(FieldName.FIELD_CLDIR_ID);

            if (dirLevel >= CACHE_DIR_LEVEL) {
                String path = tragetDirPath.getPathByLevel(dirLevel);
                dirCache.put(dirId, path, firstVersion);
            }
            parentId = dirId;
            dirLevel++;
        }
        return new DirInfo(dirBson, firstVersion);
    }

    private long getDBVersion(ScmMetaService metaService, String wsName) throws ScmServerException {
        BSONObject dirBson = metaService.getDirInfo(wsName, CommonDefine.Directory.SCM_ROOT_DIR_ID);
        return (long) dirBson.get(FieldName.FIELD_CLDIR_VERSION);
    }

    public String getPathById(ScmWorkspaceInfo ws, String id) throws ScmServerException {
        if (CommonDefine.Directory.SCM_ROOT_DIR_ID.equals(id)) {
            return CommonDefine.Directory.SCM_ROOT_DIR_NAME;
        }

        ScmMetaService metaService = ScmContentModule.getInstance().getMetaService();
        // no query cache
        if (!enableCache()) {
            return metaService.getPathByDirId(ws.getName(), id);
        }
        ScmDirCache dirCache = ws.getDirCache();

        LinkedList<DirInfo> dirRecordList = new LinkedList<DirInfo>();
        String path = getPathAndRecordDirs(metaService, dirCache, id, dirRecordList);
        return generatePathByDirs(dirCache, path, dirRecordList);
    }

    private String getPathAndRecordDirs(ScmMetaService metaService, ScmDirCache dirCache, String id,
            LinkedList<DirInfo> dirRecordList) throws ScmServerException {
        String path = CommonDefine.Directory.SCM_DIR_SEP;
        while (!id.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            DirInfo dbDirInfo = getDirInfoById(metaService, dirCache.getWsName(), id);
            long dbVersion = dbDirInfo.getVersion();
            BSONObject dbDirBson = dbDirInfo.getDirBson();

            if (dbDirBson == null) {
                dirCache.removeById(id, dbVersion);
                throw new ScmServerException(ScmError.DIR_NOT_FOUND, "get dir info failed:wsName="
                        + dirCache.getWsName() + ", id=" + id + ", version=" + dbVersion);
            }

            ScmDirCacheInfo cacheDirInfo = dirCache.getDirById(id);
            if (cacheDirInfo != null) {
                if (dbVersion <= cacheDirInfo.getVersion()) {
                    path = cacheDirInfo.getPath();
                    break;
                }
                else {
                    dirRecordList.addFirst(dbDirInfo);
                    id = (String) dbDirBson.get(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID);
                    dirCache.checkClear(dbVersion);
                    recordPathDirs(metaService, dirCache, id, dirRecordList);
                    break;
                }
            }
            dirRecordList.addFirst(dbDirInfo);
            id = (String) dbDirBson.get(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID);
        }
        return path;
    }

    private void recordPathDirs(ScmMetaService metaService, ScmDirCache dirCache, String id,
            LinkedList<DirInfo> dirRecordList) throws ScmServerException {
        while (!id.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            DirInfo dbDirInfo = getDirInfoById(metaService, dirCache.getWsName(), id);
            long dbVersion = dbDirInfo.getVersion();
            BSONObject dbDirBson = dbDirInfo.getDirBson();

            if (dbDirBson == null) {
                dirCache.removeById(id, dbDirInfo.getVersion());
                throw new ScmServerException(ScmError.DIR_NOT_FOUND, "get parentPath failed:wsName="
                        + dirCache.getWsName() + ", parentId=" + id + ", version=" + dbVersion);
            }

            dirRecordList.addFirst(dbDirInfo);
            id = (String) dbDirBson.get(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID);
        }
    }

    // generate all path and write cache
    private String generatePathByDirs(ScmDirCache dirCache, String dirPath, List<DirInfo> dirList)
            throws ScmServerException {
        int dirLevel = ScmDirPath.getLevelByPath(dirPath);
        StringBuilder newpath = new StringBuilder(dirPath);
        for (int i = 0; i < dirList.size(); i++) {
            DirInfo dirInfo = dirList.get(i);
            String dirName = (String) dirInfo.getDirBson().get(FieldName.FIELD_CLDIR_NAME);
            newpath.append(dirName);
            newpath.append(CommonDefine.Directory.SCM_DIR_SEP);
            dirLevel++;
            if (dirLevel >= CACHE_DIR_LEVEL) {
                String dirId = (String) dirInfo.getDirBson().get(FieldName.FIELD_CLDIR_ID);
                dirCache.put(dirId, newpath.toString(), dirInfo.getVersion());
            }
        }
        return newpath.toString();
    }

    public void rename(ScmWorkspaceInfo ws, String id, BSONObject updator)
            throws ScmServerException {
        try {
            ScmMetaService metaService = ScmContentModule.getInstance().getMetaService();
            String newName = (String) updator.get(FieldName.FIELD_CLDIR_NAME);
            long newVersion = metaService.updateDir(ws.getName(), id, updator);
            renameDirCache(getCache(ws), id, newName, newVersion);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to rename: dirId=" + id + ", updater=" + updator, e);
        }
    }

    private void renameDirCache(ScmDirCache dirCache, String id, String newName, long version)
            throws ScmServerException {
        if (enableCache()) {
            dirCache.renameDir(id, newName, version);
        }
    }

    public void move(ScmWorkspaceInfo ws, String destParentDirID, String id, BSONObject updator)
            throws ScmServerException {
        BSONObject parentDirMatcher = new BasicBSONObject();
        parentDirMatcher.put(FieldName.FIELD_CLDIR_ID, destParentDirID);

        ScmLockPath parentLockPath = ScmLockPathFactory.createDirLockPath(ws.getName(),
                destParentDirID);
        ScmLockPath globalDirLockPath = ScmLockPathFactory.createGlobalDirLockPath(ws.getName());
        ScmLock parentRLock = null;
        ScmLock globalDirWLock = null;

        try {
            ScmMetaService metaService = ScmContentModule.getInstance().getMetaService();
            if (!destParentDirID.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
                // this lock for check move option
                globalDirWLock = writeLock(globalDirLockPath);

                parentRLock = readLock(parentLockPath);
            }

            MetaDirAccessor dirAccessor = metaService.getMetaSource().getDirAccessor(ws.getName());
            if (dirAccessor.count(parentDirMatcher) <= 0) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "parent directory not exist:parentDirId=" + destParentDirID);
            }

            checkMoveOp(id, destParentDirID, dirAccessor);

            long newVersion = metaService.updateDir(ws.getName(), id, updator);
            unlock(parentRLock, parentLockPath);
            parentRLock = null;
            unlock(globalDirWLock, globalDirLockPath);
            globalDirWLock = null;
            moveDirCache(getCache(ws), id, destParentDirID, newVersion);
        }
        catch (ScmServerException e) {
            throw e;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to move dir, metasource error:dirId=" + id + ",moveToDirId="
                            + destParentDirID,
                    e);
        }
        finally {
            unlock(parentRLock, parentLockPath);
            unlock(globalDirWLock, globalDirLockPath);
        }
    }

    private void checkMoveOp(String myDirId, String moveToDirId, MetaDirAccessor dirAccessor)
            throws ScmServerException {
        BSONObject matcher = new BasicBSONObject();
        while (!moveToDirId.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            matcher.put(FieldName.FIELD_CLDIR_ID, moveToDirId);
            BSONObject dir = ScmMetaSourceHelper.queryOne(dirAccessor, matcher);
            if (dir == null) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "dest path is not exist:dirId=" + moveToDirId);
            }
            if (myDirId.equals(dir.get(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID))) {
                throw new ScmServerException(ScmError.DIR_MOVE_TO_SUBDIR,
                        "can not move dir to a subdir of itself:dirId=" + myDirId + ",moveToDirId="
                                + moveToDirId);
            }
            moveToDirId = (String) dir.get(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID);
        }

    }

    private void moveDirCache(ScmDirCache dirCache, String id, String targetId, long version)
            throws ScmServerException {
        if (enableCache()) {
            dirCache.moveDir(id, targetId, version);
        }
    }

    public void delete(ScmWorkspaceInfo ws, String id, String path) throws ScmServerException {
        ScmMetaService metaService = ScmContentModule.getInstance().getMetaService();
        MetaRelAccessor relAccessor = metaService.getMetaSource().getRelAccessor(ws.getName(),
                null);

        BSONObject subFileMatcher = new BasicBSONObject();
        subFileMatcher.put(FieldName.FIELD_CLREL_DIRECTORY_ID, id);

        BSONObject subDirMatcher = new BasicBSONObject();
        subDirMatcher.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, id);

        ScmLockPath lockPath = ScmLockPathFactory.createDirLockPath(ws.getName(), id);
        ScmLock wLock = writeLock(lockPath);
        try {
            MetaDirAccessor dirAccessor = metaService.getMetaSource().getDirAccessor(ws.getName());
            if (dirAccessor.count(subDirMatcher) > 0 || relAccessor.count(subFileMatcher) > 0) {
                throw new ScmServerException(ScmError.DIR_NOT_EMPTY,
                        "directory is not empty:id=" + id);
            }
            long version = metaService.deleteDir(ws.getName(), id);
            unlock(wLock, lockPath);
            wLock = null;
            deleteDirCache(getCache(ws), id, path, version);
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

    private void deleteDirCache(ScmDirCache dirCache, String id, String path, long version) {
        if (enableCache()) {
            dirCache.deleteDir(id, path, version);
        }
    }

    public void insert(ScmWorkspaceInfo ws, BSONObject dirInfo, String path, long version)
            throws ScmServerException {
        String parentID = (String) dirInfo.get(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID);
        BSONObject parentDirMatcher = new BasicBSONObject();
        parentDirMatcher.put(FieldName.FIELD_CLDIR_ID, parentID);
        ScmLockPath lockPath = ScmLockPathFactory.createDirLockPath(ws.getName(), parentID);
        ScmLock rLock = null;
        ScmMetaService metaService = ScmContentModule.getInstance().getMetaService();
        if (!parentID.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            rLock = readLock(lockPath);
        }
        try {
            if (metaService.getDirCount(ws.getName(), parentDirMatcher) <= 0) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "parent directory not exist:id=" + parentID);
            }
            metaService.insertDir(ws.getName(), dirInfo);
            unlock(rLock, lockPath);
            rLock = null;
            String id = (String) dirInfo.get(FieldName.FIELD_CLDIR_ID);
            insertDirCache(getCache(ws), id, path, version);
        }
        finally {
            unlock(rLock, lockPath);
        }
    }

    private void insertDirCache(ScmDirCache dirCache, String id, String path, long version) {
        if (enableCache() && path != null && ScmDirPath.getLevelByPath(path) >= CACHE_DIR_LEVEL) {
            dirCache.put(id, path, version);
        }
    }

    private ScmLock writeLock(ScmLockPath path) throws ScmServerException {
        return ScmLockManager.getInstance().acquiresWriteLock(path);
    }

    private ScmLock readLock(ScmLockPath lockPath) throws ScmServerException {
        try {
            return ScmLockManager.getInstance().acquiresReadLock(lockPath);
        }
        catch (Exception e) {
            throw new ScmLockException("failed to lock:path=" + lockPath, e);
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

    private DirInfo getDirInfoById(ScmMetaService metaService, String wsName, String id)
            throws ScmServerException {
        // id:{$in : [ 000000000000000000000000 , targetId ]}
        List<String> idList = new ArrayList<String>();
        idList.add(CommonDefine.Directory.SCM_ROOT_DIR_ID);
        idList.add(id);
        BSONObject inMather = new BasicBSONObject();
        inMather.put(SequoiadbHelper.SEQUOIADB_MATCHER_IN, idList);
        BSONObject filter = new BasicBSONObject();
        filter.put(FieldName.FIELD_CLDIR_ID, inMather);
        MetaCursor dirCursor = null;
        try {
            dirCursor = metaService.queryDirInfo(wsName, filter);
            long version = ScmDirCache.DEFAULT_VERSION;
            BSONObject dirBson = null;
            while (dirCursor.hasNext()) {
                BSONObject tmpBson = dirCursor.getNext();
                String dirId = (String) tmpBson.get(FieldName.FIELD_CLDIR_ID);
                if (dirId.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
                    version = (long) tmpBson.get(FieldName.FIELD_CLDIR_VERSION);
                }
                else {
                    dirBson = tmpBson;
                }
            }
            if (version == ScmDirCache.DEFAULT_VERSION) {
                throw new ScmSystemException("version error, dbVersion=" + version);
            }
            return new DirInfo(dirBson, version);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "get dirInfo failed:workspace=" + wsName + ",filter=" + filter, e);
        }
        finally {
            if (dirCursor != null) {
                dirCursor.close();
            }
        }
    }

    class DirInfo {
        private BSONObject dirBson;
        private long version;

        public DirInfo(BSONObject dirBson, long version) {
            this.dirBson = dirBson;
            this.version = version;
        }

        public BSONObject getDirBson() {
            return dirBson;
        }

        public long getVersion() {
            return version;
        }
    }
}
