package com.sequoiacm.contentserver.service;

import org.bson.BSONObject;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.model.ClientWorkspaceUpdator;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.metasource.MetaCursor;

public interface IWorkspaceService {
    BSONObject getWorkspace(ScmUser user, String workspaceName) throws ScmServerException;

    /*
     * void getWorkspaceList(PrintWriter writer, BSONObject condition) throws
     * ScmServerException;
     */
    MetaCursor getWorkspaceList(ScmUser user, BSONObject condition, BSONObject orderBy, long skip,
            long limit) throws ScmServerException;

    BSONObject createWorkspace(ScmUser user, String wsName, BSONObject wsConf, String createUser)
            throws ScmServerException;

    void deleteWorkspace(String sessionId, String token, ScmUser user, String wsName,
            boolean isEnforced) throws ScmServerException;

    BSONObject updateWorkspace(ScmUser user, String wsName, ClientWorkspaceUpdator updator)
            throws ScmServerException;

    long countWorkspace(ScmUser user, BSONObject condition) throws ScmServerException;

    BSONObject disabledTagRetrieval(ScmUser user, String ws) throws ScmServerException;

    BSONObject enableTagRetrieval(ScmUser user, String ws) throws ScmServerException;
}
