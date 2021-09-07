package com.sequoiacm.contentserver.service;

import org.bson.BSONObject;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.model.ClientWorkspaceUpdator;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.metasource.MetaCursor;

public interface IWorkspaceService {
    BSONObject getWorkspace(String workspaceName) throws ScmServerException;

    /*
     * void getWorkspaceList(PrintWriter writer, BSONObject condition) throws
     * ScmServerException;
     */
    MetaCursor getWorkspaceList(BSONObject condition,BSONObject orderBy, long skip, long limit)
            throws ScmServerException;

    BSONObject createWorkspace(String wsName, BSONObject wsConf, String createUser)
            throws ScmServerException;

    void deleteWorkspace(String sessionId, String token, ScmUser user, String wsName,
            boolean isEnforced) throws ScmServerException;

    BSONObject updateWorkspace(String wsName, ClientWorkspaceUpdator updator)
            throws ScmServerException;

    long countWorkspace(BSONObject condition) throws ScmServerException;
}
