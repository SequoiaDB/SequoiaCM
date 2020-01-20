package com.sequoiacm.contentserver.dao;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.metasource.MetaRelAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;

public class DirUpdatorDao {
    private ScmContentServer contentServer = ScmContentServer.getInstance();
    private ScmWorkspaceInfo ws;
    private BSONObject dirRec;
    private String user;
    private long updateTime;
    private BSONObject updator;
    private DirOperator dirOperator;

    public DirUpdatorDao(String user, String wsName, BSONObject newDirInfo)
            throws ScmServerException {
        this.user = user;
        this.ws = ScmContentServer.getInstance().getWorkspaceInfoChecked(wsName);
        this.updator = newDirInfo;
        this.dirOperator = DirOperator.getInstance();
    }

    public long updateById(String dirId) throws ScmServerException {
        if (dirId.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            throw new ScmInvalidArgumentException("can not update root directory:id=" + dirId);
        }
        dirRec = contentServer.getMetaService().getDirInfo(ws.getName(), dirId);
        if (dirRec == null) {
            throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                    "directory not exist:directoryId=" + dirId);
        }
        return update();
    }

    public long updateByPath(String dirPath) throws ScmServerException {
        dirRec = dirOperator.getDirByPath(ws, dirPath);
        if (dirRec == null) {
            throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                    "directory not exist:path=" + dirPath);
        }
        if (CommonDefine.Directory.SCM_ROOT_DIR_ID.equals(dirRec.get(FieldName.FIELD_CLDIR_ID))) {
            throw new ScmInvalidArgumentException("can not update root directory:path=" + dirPath);
        }
        return update();
    }

    private long update() throws ScmServerException {
        if (updator.keySet().size() != 1) {
            throw new ScmInvalidArgumentException(
                    "invlid argument,updates only one properties at a time:updator=" + updator);
        }

        if (updator.containsField(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID)) {
            String toDirId = (String) ScmMetaSourceHelper.checkExistString(updator,
                    FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID);
            moveTo(toDirId);
        }
        else if (updator.containsField(CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_PATH)) {
            String moveToDirPath = (String) ScmMetaSourceHelper.checkExistString(updator,
                    CommonDefine.Directory.SCM_REST_ARG_PARENT_DIR_PATH);
            BSONObject parentDir = dirOperator.getDirByPath(ws, moveToDirPath);
            if (parentDir == null) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "directory not exist:path=" + moveToDirPath);
            }
            String moveToDirId = (String) parentDir.get(FieldName.FIELD_CLDIR_ID);
            moveTo(moveToDirId);
        }
        else if (updator.containsField(FieldName.FIELD_CLDIR_NAME)) {
            String newName = (String) ScmMetaSourceHelper.checkExistString(updator,
                    FieldName.FIELD_CLDIR_NAME);
            if (!ScmArgChecker.Directory.checkDirectoryName(newName)) {
                throw new ScmInvalidArgumentException("invalid arg:newDirectoryName=" + newName);
            }
            rename(newName);
        }
        else {
            // unsupport other properties now
            throw new ScmInvalidArgumentException("invlid arg,unknown properties:arg=" + updator);

            // update(updatorArgBSON);
        }
        return updateTime;
    }

    private void rename(String newName) throws ScmServerException {
        ScmMetaService metaService = contentServer.getMetaService();
        String dirParentId = (String) dirRec.get(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID);

        checkFileNotExist(metaService.getMetaSource().getRelAccessor(ws.getName(), null), newName,
                dirParentId);

        BSONObject updator = new BasicBSONObject();
        updator.put(FieldName.FIELD_CLDIR_NAME, newName);
        addExtraField(updator);
        String id = (String) dirRec.get(FieldName.FIELD_CLDIR_ID);
        dirOperator.rename(ws, id, updator);
    }

    private void moveTo(String destParentDirID) throws ScmServerException {
        String id = (String) dirRec.get(FieldName.FIELD_CLDIR_ID);
        if (id.equals(destParentDirID)) {
            throw new ScmServerException(ScmError.DIR_MOVE_TO_SUBDIR,
                    "can not move dir to a subdir of itself:dirId=" + id + ",moveToDirId="
                            + destParentDirID);
        }

        ScmMetaService metaService = contentServer.getMetaService();
        checkFileNotExist(metaService.getMetaSource().getRelAccessor(ws.getName(), null),
                (String) dirRec.get(FieldName.FIELD_CLDIR_NAME), destParentDirID);

        BSONObject updator = new BasicBSONObject();
        updator.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, destParentDirID);
        addExtraField(updator);
        dirOperator.move(ws, destParentDirID, id, updator);
    }

    private void checkFileNotExist(MetaRelAccessor relAccessor, String name, String parentDirID)
            throws ScmServerException {
        // TODO: check file is not duplicate(unreliable)
        BSONObject existFileMatcher = new BasicBSONObject();
        existFileMatcher.put(FieldName.FIELD_CLREL_DIRECTORY_ID, parentDirID);
        existFileMatcher.put(FieldName.FIELD_CLREL_FILENAME, name);
        long count = 0;
        try {
            count = relAccessor.count(existFileMatcher);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "Failed to count file: matcher" + existFileMatcher, e);
        }
        if (count > 0) {
            throw new ScmServerException(ScmError.FILE_EXIST,
                    "a file with the same name exists:parentDir=" + parentDirID + ",name=" + name);
        }
    }

    private void addExtraField(BSONObject updator) {
        updateTime = System.currentTimeMillis();
        updator.put(FieldName.FIELD_CLDIR_UPDATE_TIME, updateTime);
        updator.put(FieldName.FIELD_CLDIR_UPDATE_USER, user);
    }

}
