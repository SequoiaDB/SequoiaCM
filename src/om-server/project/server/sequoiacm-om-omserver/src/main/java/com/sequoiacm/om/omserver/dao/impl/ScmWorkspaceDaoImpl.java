package com.sequoiacm.om.omserver.dao.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.BSONObject;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmPrivilege;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.privilege.ScmAllWorkspaceResource;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmWorkspaceResource;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.om.omserver.dao.ScmWorkspaceDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmWorkspaceDetail;
import com.sequoiacm.om.omserver.module.OmWorkspaceInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;

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

    public Set<String> getUserAccessibleWorkspaces(String username,
            ScmPrivilegeType expectPrivilegeType) throws ScmInternalException {
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
                        ScmPrivilege privilege = privilegeCur.getNext();
                        if (expectPrivilegeType != null) {
                            if (!privilege.getPrivilegeTypes().contains(expectPrivilegeType)
                                    && !privilege.getPrivilegeTypes()
                                            .contains(ScmPrivilegeType.ALL)) {
                                continue;
                            }
                        }
                        ScmResource resource = privilege.getResource();
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
    public void updateWorkspace(ScmOmSession session, String wsName, OmWorkspaceInfo wsInfo)
            throws ScmInternalException {
        ScmSession connection = session.getConnection();
        Boolean tagRetrievalEnabled;
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, connection);
            String siteCacheStrategy = wsInfo.getSiteCacheStrategy();
            if (siteCacheStrategy != null) {
                ws.updateSiteCacheStrategy(ScmSiteCacheStrategy.getStrategy(siteCacheStrategy));
            }
            if (wsInfo.isTagRetrievalEnabled() != null) {
                tagRetrievalEnabled = wsInfo.isTagRetrievalEnabled();
                ws.setEnableTagRetrieval(tagRetrievalEnabled);
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }
    }

    @Override
    public void createWorkspace(ScmWorkspaceConf conf) throws ScmInternalException {
        try {
            ScmFactory.Workspace.createWorkspace(session.getConnection(), conf);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }
    }

    @Override
    public void deleteWorkspace(String wsName, boolean isForce) throws ScmInternalException {
        try {
            ScmFactory.Workspace.deleteWorkspace(session.getConnection(), wsName, isForce);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), e.getMessage(), e);
        }
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
