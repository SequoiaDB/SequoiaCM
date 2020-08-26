package com.sequoiacm.contentserver.listener;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.exception.ScmServerException;

public interface FileOperationListener {
    public void preCreate(ScmWorkspaceInfo ws, BSONObject file) throws ScmServerException;

    public void postDelete(ScmWorkspaceInfo ws, List<BSONObject> allFileVersions)
            throws ScmServerException;

    public OperationCompleteCallback postCreate(ScmWorkspaceInfo ws, String fileId)
            throws ScmServerException;

    public OperationCompleteCallback postUpdate(ScmWorkspaceInfo ws, BSONObject fileInfo)
            throws ScmServerException;

    public void preUpdateContent(ScmWorkspaceInfo ws, BSONObject newVersionFile)
            throws ScmServerException;

    public OperationCompleteCallback postUpdateContent(ScmWorkspaceInfo ws, String fileId)
            throws ScmServerException;
}
