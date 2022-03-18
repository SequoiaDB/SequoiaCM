package com.sequoiacm.om.omserver.dao.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.element.privilege.*;
import com.sequoiacm.om.omserver.module.*;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.om.omserver.dao.ScmWorkspaceDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;

public class ScmWorkspaceDaoImpl implements ScmWorkspaceDao {

    private ScmOmSession session;
    private Map<String, OmWorkspaceDetail> workspaceCache = new ConcurrentHashMap<>();

    public ScmWorkspaceDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public List<ScmWorkspaceInfo> getWorkspaceList(BSONObject condition, BSONObject orderby,
            long skip, int limit) throws ScmInternalException {
        ScmSession con = session.getConnection();
        ScmCursor<ScmWorkspaceInfo> cursor = null;
        List<ScmWorkspaceInfo> workspaceList = new ArrayList<>();
        try {
            if (null == orderby) {
                orderby = ScmQueryBuilder.start(FieldName.FIELD_CLWORKSPACE_ID).is(1).get();
            }
            cursor = ScmFactory.Workspace.listWorkspace(con, condition, orderby, skip, limit);
            while (cursor.hasNext()) {
                ScmWorkspaceInfo wsInfo = cursor.getNext();
                workspaceList.add(wsInfo);
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get workspace list, " + e.getMessage(), e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return workspaceList;
    }

    public Set<String> getUserAccessibleWorkspaces(String username) throws ScmInternalException {
        Set<String> workspaces = new HashSet<>();
        ScmSession scmSession = session.getConnection();
        try {
            ScmUser user = ScmFactory.User.getUser(scmSession, username);
            Collection<ScmRole> roles = user.getRoles();
            for (ScmRole role : roles) {
                ScmCursor<ScmPrivilege> privilegeCur = null;
                try {
                    privilegeCur = ScmFactory.Privilege.listPrivileges(scmSession, role);
                    while (privilegeCur.hasNext()) {
                        ScmResource resource = privilegeCur.getNext().getResource();
                        if (resource instanceof ScmAllWorkspaceResource) {
                            // return all
                            workspaces.clear();
                            ScmCursor<ScmWorkspaceInfo> cursor = null;
                            try {
                                cursor = ScmFactory.Workspace.listWorkspace(scmSession);
                                while (cursor.hasNext()) {
                                    workspaces.add(cursor.getNext().getName());
                                }
                            }
                            finally {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                            return workspaces;
                        }
                        else if (resource instanceof ScmWorkspaceResource) {
                            workspaces.add(resource.toStringFormat());
                        }
                    }
                }
                finally {
                    if (privilegeCur != null) {
                        privilegeCur.close();
                    }
                }
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }
        return workspaces;
    }

    @Override
    public ScmWorkspace getWorkspaceDetail(String wsName) throws ScmInternalException {
        ScmSession connection = session.getConnection();
        ScmWorkspace ws = null;
        try {
            ws = ScmFactory.Workspace.getWorkspace(wsName, connection);
            return ws;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }

    }

    @Override
    public long getWorkspaceCount(BSONObject condition)
            throws ScmOmServerException, ScmInternalException {
        try {
            return ScmFactory.Workspace.count(session.getConnection(), condition);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get workspace count, " + e.getMessage(), e);
        }
    }

}
