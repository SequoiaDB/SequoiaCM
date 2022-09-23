package com.sequoiacm.om.omserver.dao;

import java.util.List;
import java.util.Set;

import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmWorkspaceInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

public interface ScmWorkspaceDao {
    public List<ScmWorkspaceInfo> getWorkspaceList(BSONObject filter, BSONObject orderby, long skip,
            int limit) throws ScmInternalException, ScmOmServerException;

    public ScmWorkspace getWorkspaceDetail(String wsName)
            throws ScmInternalException, ScmOmServerException;

    long getWorkspaceCount(BSONObject condition) throws ScmOmServerException, ScmInternalException;

    public Set<String> getUserAccessibleWorkspaces(String username) throws ScmInternalException;

    void updateWorkspace(ScmOmSession session, String wsName, OmWorkspaceInfo wsInfo)
            throws ScmInternalException;
}
