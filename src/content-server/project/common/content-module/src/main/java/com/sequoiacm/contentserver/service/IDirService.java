package com.sequoiacm.contentserver.service;

import com.sequoiacm.contentserver.model.ScmVersion;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.module.FileUploadConf;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.metasource.MetaCursor;
import org.bson.BSONObject;

import java.io.InputStream;
import java.util.Date;

public interface IDirService {
    public BSONObject getDirInfoById(ScmUser user, String wsName, String dirId)
            throws ScmServerException;

    public BSONObject getDirInfoByPath(ScmUser user, String wsName, String dirPath)
            throws ScmServerException;

    public BSONObject getDirInfoByPath(String wsName, String dirPath) throws ScmServerException;

    public String getDirPathById(ScmUser user, String wsName, String dirId)
            throws ScmServerException;

    public String getDirPathById(String wsName, String dirId) throws ScmServerException;

    /*
     * public void getDirList(PrintWriter writer, String wsName, BSONObject
     * condition) throws ScmServerException;
     */
    public MetaCursor getDirList(ScmUser user, String wsName, BSONObject condition,
            BSONObject orderby, long skip, long limit) throws ScmServerException;

    public long reanmeDirById(ScmUser user, String wsName, String dirId, String newName)
            throws ScmServerException;

    public long renameDirByPath(ScmUser user, String wsName, String dirPath, String newName)
            throws ScmServerException;

    public long moveDirById(ScmUser user, String wsName, String dirId, String newParentId,
            String newParentPath) throws ScmServerException;

    public long moveDirByPath(ScmUser user, String wsName, String dirPath, String newParentId,
            String newParentPath) throws ScmServerException;

    public BSONObject createDirByPath(ScmUser user, String wsName, String path)
            throws ScmServerException;

    public BSONObject createDirByPidAndName(ScmUser user, String wsName, String name,
            String parentID) throws ScmServerException;

    public void deleteDir(ScmUser user, String wsName, String id, String path)
            throws ScmServerException;

    public long countDir(ScmUser user, String wsName, BSONObject condition)
            throws ScmServerException;

    public String generateId(Date dirCreateTime) throws ScmServerException;

    FileMeta createFile(ScmUser user, String ws, String parentDirId, FileMeta fileInfo,
            InputStream data, FileUploadConf conf) throws ScmServerException;

    FileMeta createFile(ScmUser user, String ws, String parentDirId, FileMeta fileInfo,
            String breakpointFile, FileUploadConf conf) throws ScmServerException;

    BSONObject getFileInfoByPath(ScmUser user, String workspaceName, String filePath,
            int majorVersion, int minorVersion, boolean acceptDeleteMarker)
            throws ScmServerException;

    FileMeta moveFile(ScmUser user, String ws, String moveToDirId, String fileId,ScmVersion returnVersion)
            throws ScmServerException;

    FileMeta moveFileByPath(ScmUser user, String ws, String moveToDirPath, String fileId, ScmVersion returnVersion)
            throws ScmServerException;
}
