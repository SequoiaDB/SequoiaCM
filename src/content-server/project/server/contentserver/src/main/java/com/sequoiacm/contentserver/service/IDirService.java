package com.sequoiacm.contentserver.service;

import org.bson.BSONObject;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.MetaCursor;

public interface IDirService {
    public BSONObject getDirInfoById(String wsName, String dirId) throws ScmServerException;

    public BSONObject getDirInfoByPath(String wsName, String dirPath) throws ScmServerException;

    public String getDirPathById(String wsName, String dirId) throws ScmServerException;

    /*
     * public void getDirList(PrintWriter writer, String wsName, BSONObject
     * condition) throws ScmServerException;
     */
    public MetaCursor getDirList(String wsName, BSONObject condition, BSONObject orderby, long skip,
            long limit) throws ScmServerException;

    public long reanmeDirById(String user, String wsName, String dirId, String newName)
            throws ScmServerException;

    public long renameDirByPath(String user, String wsName, String dirPath, String newName)
            throws ScmServerException;

    public long moveDirById(String user, String wsName, String dirId, String newParentId,
            String newParentPath) throws ScmServerException;

    public long moveDirByPath(String user, String wsName, String dirPath, String newParentId,
            String newParentPath) throws ScmServerException;

    public BSONObject createDirByPath(String user, String wsName, String path)
            throws ScmServerException;

    public BSONObject createDirByPidAndName(String user, String wsName, String name,
            String parentID) throws ScmServerException;

    public void deleteDir(String wsName, String id, String path) throws ScmServerException;

    public long countDir(String wsName, BSONObject condition) throws ScmServerException;

}
