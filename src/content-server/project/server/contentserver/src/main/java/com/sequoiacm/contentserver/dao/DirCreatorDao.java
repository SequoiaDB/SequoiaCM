package com.sequoiacm.contentserver.dao;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.contentserver.cache.ScmDirCache;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.dao.DirOperator.DirInfo;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.metasource.ScmMetasourceException;

public class DirCreatorDao {
    private ScmWorkspaceInfo ws;
    private String user;
    private DirOperator dirOperator;

    public DirCreatorDao(String user, String wsName) throws ScmServerException {
        this.user = user;
        this.ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
        this.dirOperator = DirOperator.getInstance();
    }

    public BSONObject createDirByPidAndName(String parentId, String name)
            throws ScmServerException {
        BSONObject dirInfo = new BasicBSONObject();
        dirInfo.put(FieldName.FIELD_CLDIR_NAME, name);
        dirInfo.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, parentId);
        addExtraField(dirInfo);
        insert(dirInfo, null, ScmDirCache.DEFAULT_VERSION);
        return dirInfo;
    }

    public BSONObject createDirByPath(String path) throws ScmServerException {
        String name = ScmSystemUtils.basename(path);
        if (!ScmArgChecker.Directory.checkDirectoryName(name)) {
            throw new ScmInvalidArgumentException("invalid directory name:name=" + name);
        }
        String paerntDirPath = ScmSystemUtils.dirname(path);
        DirInfo parentDir = dirOperator.getDirAndVersionByPath(ws, paerntDirPath);
        if (parentDir == null) {
            throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                    "directory not exist:path=" + paerntDirPath);
        }
        String parentId = (String) parentDir.getDirBson().get(FieldName.FIELD_CLDIR_ID);

        BSONObject dirInfo = new BasicBSONObject();
        dirInfo.put(FieldName.FIELD_CLDIR_NAME, name);
        dirInfo.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, parentId);
        addExtraField(dirInfo);
        insert(dirInfo, path, parentDir.getVersion());
        return dirInfo;
    }

    private void insert(BSONObject dirInfo, String path, long version) throws ScmServerException {
        checkSameName(dirInfo);
        dirOperator.insert(ws, dirInfo, path, version);
    }

    private void checkSameName(BSONObject dirInfo) throws ScmServerException {
        ScmMetaService metaService = ScmContentServer.getInstance().getMetaService();
        String parentID = (String) dirInfo.get(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID);
        String dirName = (String) dirInfo.get(FieldName.FIELD_CLDIR_NAME);

        BSONObject existFileMatcher = new BasicBSONObject();
        existFileMatcher.put(FieldName.FIELD_CLREL_DIRECTORY_ID, parentID);
        existFileMatcher.put(FieldName.FIELD_CLREL_FILENAME, dirName);

        long count = 0;
        try {
            count = metaService.getMetaSource().getRelAccessor(ws.getName(), null)
                    .count(existFileMatcher);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to count file: matcher" + existFileMatcher, e);
        }
        if (count > 0) {
            throw new ScmServerException(ScmError.FILE_EXIST,
                    "a file with the same name exists:parentDirectory=" + parentID + ",fileName="
                            + dirName);
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
