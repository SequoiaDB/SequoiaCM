package com.sequoiacm.om.omserver.dao;

import java.util.List;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.module.OmRoleBasicInfo;
import com.sequoiacm.om.omserver.module.OmRoleInfo;

public interface ScmRoleDao {
    public OmRoleInfo getRole(String rolename) throws ScmInternalException;

    public void createRole(String rolename, String description) throws ScmInternalException;

    public void deleteRole(String rolename) throws ScmInternalException;

    public void grantPrivilege(String rolename, String resourceType, String resource,
            String privilegeType) throws ScmInternalException;

    public void revokePrivilege(String rolename, String resourceType, String resource,
            String privilegeType) throws ScmInternalException;

    public List<OmRoleBasicInfo> listRoles(long skip, int limit) throws ScmInternalException;
}
