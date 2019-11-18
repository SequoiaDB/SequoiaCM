package com.sequoiacm.om.omserver.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmPrivilege;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.om.omserver.dao.ScmRoleDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmResourceInfo;
import com.sequoiacm.om.omserver.module.OmRoleBasicInfo;
import com.sequoiacm.om.omserver.module.OmRoleInfo;
import com.sequoiacm.om.omserver.session.ScmOmSessionImpl;

public class ScmRoleDaoImpl implements ScmRoleDao {

    private ScmOmSessionImpl session;

    public ScmRoleDaoImpl(ScmOmSessionImpl session) {
        this.session = session;
    }

    @Override
    public OmRoleInfo getRole(String rolename) throws ScmInternalException {
        ScmCursor<ScmPrivilege> privilegeCursor = null;
        try {
            ScmRole role = ScmFactory.Role.getRole(session.getConnection(), rolename);
            privilegeCursor = ScmFactory.Privilege.listPrivileges(session.getConnection(), role);
            List<OmResourceInfo> resourcesInfo = new ArrayList<>();
            while (privilegeCursor.hasNext()) {
                ScmPrivilege privilege = privilegeCursor.getNext();
                resourcesInfo.add(transformResource(privilege));
            }
            return transformRole(role, resourcesInfo);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get role info, " + e.getMessage(), e);
        }
        finally {
            if (privilegeCursor != null) {
                privilegeCursor.close();
            }
        }
    }

    @Override
    public void createRole(String rolename, String description) throws ScmInternalException {
        try {
            ScmFactory.Role.createRole(session.getConnection(), rolename, description);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to create role, " + e.getMessage(),
                    e);
        }

    }

    @Override
    public void deleteRole(String rolename) throws ScmInternalException {
        try {
            ScmFactory.Role.deleteRole(session.getConnection(), rolename);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "delete role failed, " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void grantPrivilege(String rolename, String resourceType, String resource,
            String privilegeType) throws ScmInternalException {
        try {
            ScmRole role = ScmFactory.Role.getRole(session.getConnection(), rolename);
            ScmResource scmResource = ScmResourceFactory.createResource(resourceType, resource);
            ScmPrivilegeType scmPrivilegeType = ScmPrivilegeType.valueOf(privilegeType);
            ScmFactory.Role.grantPrivilege(session.getConnection(), role, scmResource,
                    scmPrivilegeType);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to grant privilege, " + e.getMessage(), e);
        }
    }

    @Override
    public void revokePrivilege(String rolename, String resourceType, String resource,
            String privilegeType) throws ScmInternalException {
        try {
            ScmRole role = ScmFactory.Role.getRole(session.getConnection(), rolename);
            ScmResource scmResource = ScmResourceFactory.createResource(resourceType, resource);
            ScmPrivilegeType scmPrivilegeType = ScmPrivilegeType.valueOf(privilegeType);
            ScmFactory.Role.revokePrivilege(session.getConnection(), role, scmResource,
                    scmPrivilegeType);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to revoke privilege, " + e.getMessage(), e);
        }
    }

    @Override
    public List<OmRoleBasicInfo> listRoles(long skip, int limit) throws ScmInternalException {
        ScmCursor<ScmRole> cursor = null;
        List<OmRoleBasicInfo> roleInfos = null;
        try {
            BSONObject orderBy = new BasicBSONObject().append(FieldName.FIELD_ALL_OBJECTID, 1);
            cursor = ScmFactory.Role.listRoles(session.getConnection(), orderBy, skip, limit);
            roleInfos = new ArrayList<OmRoleBasicInfo>();
            while (cursor.hasNext()) {
                ScmRole role = cursor.getNext();
                roleInfos.add(transformRoleBasic(role));
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to list roles, " + e.getMessage(),
                    e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return roleInfos;
    }

    private OmRoleInfo transformRole(ScmRole role, List<OmResourceInfo> resources) {
        String roleId = role.getRoleId();
        String roleName = role.getRoleName();
        String description = role.getDescription();
        OmRoleInfo roleInfo = new OmRoleInfo(roleId, roleName, description, resources);
        return roleInfo;
    }

    private OmResourceInfo transformResource(ScmPrivilege privilege) throws ScmException {
        ScmResource resource = privilege.getResource();
        return new OmResourceInfo(resource.getType(), resource.toStringFormat(),
                privilege.getPrivilegeType().toString());
    }

    private OmRoleBasicInfo transformRoleBasic(ScmRole role) {
        String roleId = role.getRoleId();
        String roleName = role.getRoleName();
        String description = role.getDescription();
        OmRoleBasicInfo roleInfo = new OmRoleBasicInfo(roleId, roleName, description);
        return roleInfo;
    }
}
