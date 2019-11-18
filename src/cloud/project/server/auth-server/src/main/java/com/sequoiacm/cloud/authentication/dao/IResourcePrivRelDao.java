package com.sequoiacm.cloud.authentication.dao;

import java.util.List;

import com.sequoiacm.infrastructrue.security.core.ITransaction;
import com.sequoiacm.infrastructrue.security.core.ScmPrivilege;

public interface IResourcePrivRelDao {
    void insertPrivilege(ScmPrivilege privilege);

    void insertPrivilege(ScmPrivilege privilege, ITransaction t);

    void deletePrivilege(ScmPrivilege privilege);

    void deletePrivilege(ScmPrivilege priv, ITransaction t);

    public String generatePrivilegeId();

    ScmPrivilege getPrivilege(String roleType, String roleId, String resourceId);

    void updatePrivilegeValue(String id, String Privilege);

    void updatePrivilegeValue(String id, String Privilege, ITransaction t);

    ScmPrivilege getPrivilegeById(String privilegeId);

    List<ScmPrivilege> listPrivileges();

    List<ScmPrivilege> listPrivilegesByRoleId(String roleId);

    List<ScmPrivilege> listPrivilegesByResourceId(String resourceId);
}
