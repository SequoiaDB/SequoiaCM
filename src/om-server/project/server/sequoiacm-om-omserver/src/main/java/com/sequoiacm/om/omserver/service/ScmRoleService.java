
package com.sequoiacm.om.omserver.service;

import java.util.List;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmPrivilegeDetail;
import com.sequoiacm.om.omserver.module.OmRoleBasicInfo;
import com.sequoiacm.om.omserver.module.OmRoleInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;

public interface ScmRoleService {
    public OmRoleInfo getRole(ScmOmSession session, String rolename)
            throws ScmInternalException, ScmOmServerException;

    public void createRole(ScmOmSession session, String rolename, String description)
            throws ScmInternalException, ScmOmServerException;

    public void deleteRole(ScmOmSession session, String rolename)
            throws ScmInternalException, ScmOmServerException;

    public void grantPrivilege(ScmOmSession session, String rolename, String resourceType,
            String resource, String privilegeType)
            throws ScmInternalException, ScmOmServerException;

    public void revokePrivilege(ScmOmSession session, String rolename, String resourceType,
            String resource, String privilegeType)
            throws ScmInternalException, ScmOmServerException;

    public List<OmRoleBasicInfo> listRoles(ScmOmSession session, BSONObject condition, long skip,
            int limit) throws ScmInternalException, ScmOmServerException;

    long getRoleCount(ScmOmSession session, BSONObject condition) throws ScmInternalException;

    List<OmPrivilegeDetail> listPrivileges(ScmOmSession session, String roleName)
            throws ScmInternalException;
}
