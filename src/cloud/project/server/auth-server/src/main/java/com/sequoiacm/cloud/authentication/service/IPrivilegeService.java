package com.sequoiacm.cloud.authentication.service;

import java.util.List;

import com.sequoiacm.infrastructrue.security.core.ScmPrivMeta;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;
import com.sequoiacm.infrastructrue.security.core.ScmResource;
import com.sequoiacm.infrastructrue.security.core.ScmRole;

public interface IPrivilegeService {

    ScmPrivMeta getMeta();

    void grantPrivilege(String roleType, ScmRole role, String resourceType, String resource,
            String privilege) throws Exception;

    void revokePrivilege(String roleType, ScmRole role, String resourceType, String resource,
            String privilege) throws Exception;

    List<ScmResource> getResourceList();

    List<ScmPrivilege> getPrivilegeList();

    List<ScmPrivilege> getPrivilegeListByRoleName(String roleName);

    void deleteRole(ScmRole role) throws Exception;

    ScmResource getResourceById(String resourceId);

    ScmPrivilege getPrivilegeById(String privilegeId);

    List<ScmPrivilege> getPrivilegeListByRoleId(String roleId);

    List<ScmPrivilege> getPrivilegeListByResourceId(String resourceId);

    List<ScmPrivilege> getPrivilegeListByResource(String resourceType, String resource);

    List<ScmResource> getResourceListByWorkspace(String workspaceName);
}
