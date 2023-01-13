package com.sequoiacm.om.omserver.dao.impl;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.om.omserver.common.CommonUtil;
import com.sequoiacm.om.omserver.module.OmPrivilegeBasic;
import com.sequoiacm.om.omserver.module.OmPrivilegeDetail;
import com.sequoiacm.om.omserver.module.OmUserInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.apache.commons.lang.StringUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sequoiacm.infrastructrue.security.core.ScmRole.AUTH_MONITOR_ROLE_NAME;

public class ScmRoleDaoImpl implements ScmRoleDao {

    private ScmOmSession session;
    private static final Logger logger = LoggerFactory.getLogger(ScmRoleDaoImpl.class);

    public ScmRoleDaoImpl(ScmOmSession session) {
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
    public List<OmRoleBasicInfo> listRoles(BSONObject condition, long skip, int limit)
            throws ScmInternalException {
        ScmCursor<ScmRole> cursor = null;
        List<OmRoleBasicInfo> roleInfos = null;
        try {
            BSONObject orderBy = new BasicBSONObject().append(FieldName.FIELD_ALL_OBJECTID, 1);
            cursor = ScmFactory.Role.listRoles(session.getConnection(), condition, orderBy, skip,
                    limit);
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

    @Override
    public List<OmPrivilegeDetail> listPrivileges(String roleName) throws ScmInternalException {
        List<OmPrivilegeDetail> privilegeList = new ArrayList<>();
        ScmCursor<ScmPrivilege> cursor = null;
        try {
            ScmSession conn = session.getConnection();
            ScmRole role = ScmFactory.Role.getRole(conn, roleName);
            cursor = ScmFactory.Privilege.listPrivileges(conn, role);
            while (cursor.hasNext()) {
                ScmPrivilege privilege = cursor.getNext();
                ScmResource resource = ScmFactory.Resource.getResourceById(conn,
                        privilege.getResourceId());
                privilegeList.add(transformToOmPrivilegeDetail(roleName, privilege, resource));
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to list privilege by role, roleName=" + roleName + ", errorMsg="
                            + e.getMessage(),
                    e);
        }
        finally {
            CommonUtil.closeResource(cursor);
        }
        return privilegeList;
    }

    private OmPrivilegeDetail transformToOmPrivilegeDetail(String roleName, ScmPrivilege privilege,
            ScmResource resource) {
        OmPrivilegeDetail privilegeDetail = new OmPrivilegeDetail();
        privilegeDetail.setId(privilege.getId());
        privilegeDetail.setResourceType(resource.getType());
        privilegeDetail.setResourceName(resource.toStringFormat());
        List<String> privileges = new ArrayList<>();
        List<ScmPrivilegeType> privilegeTypes = privilege.getPrivilegeTypes();
        for (ScmPrivilegeType privilegeType : privilegeTypes) {
            // SEQUOIACM-1134: 处理 LOW_LEVEL_READ 无法识别问题
            if (StringUtils.equals(AUTH_MONITOR_ROLE_NAME, roleName)
                    && privilegeType.equals(ScmPrivilegeType.UNKNOWN)) {
                privileges.add(ScmPrivilegeDefine.LOW_LEVEL_READ.getName());
                continue;
            }
            privileges.add(privilegeType.getPriv());
        }
        privilegeDetail.setPrivilegeList(privileges);
        return privilegeDetail;
    }

    @Override
    public long countRole(BSONObject condition) throws ScmInternalException {
        try {
            session.getConnection();
            try {
                return ScmFactory.Role.countRole(session.getConnection(), condition);
            }
            catch (ScmException e) {
                // 兼容旧版本服务端（HEAD 请求无法拿到预期的响应头）
                if (e.getError().equals(ScmError.NETWORK_IO)) {
                    logger.warn(
                            "There is no countUser interface, maybe the version of auth-server is lower. "
                                    + "cause by: " + e.getMessage());
                    return listRoles(condition, 0, -1).size();
                }
                throw e;
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to count user, " + e.getMessage(),
                    e);
        }
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
