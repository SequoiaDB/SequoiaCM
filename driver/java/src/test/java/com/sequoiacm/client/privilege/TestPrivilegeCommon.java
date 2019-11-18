package com.sequoiacm.client.privilege;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;

public class TestPrivilegeCommon {
    public static ScmUser createUser(ScmSession ss, String userName, String passwd)
            throws ScmException {
        return ScmFactory.User.createUser(ss, userName, ScmUserPasswordType.LOCAL, passwd);
    }

    public static void deleteUser(ScmSession ss, String userName) throws ScmException {
        ScmFactory.User.deleteUser(ss, userName);
    }

    public static ScmRole createRole(ScmSession ss, String roleName) throws ScmException {
        return ScmFactory.Role.createRole(ss, roleName, "");
    }

    public static ScmUser associateRole(ScmSession ss, ScmUser user, ScmRole role)
            throws ScmException {
        ScmUserModifier m = new ScmUserModifier();
        m.addRole(role);
        return ScmFactory.User.alterUser(ss, user, m);
    }

    public static void deleteRole(ScmSession ss, String roleName) throws ScmException {
        ScmFactory.Role.deleteRole(ss, roleName);
    }

    public static void grantPrivilege(ScmSession ss, ScmRole role, String resourceType,
            String resource, String privilege) throws ScmException {
        ScmResource r = ScmResourceFactory.createResource(resourceType, resource);
        ScmFactory.Role.grantPrivilege(ss, role, r, privilege);
    };

    public static void revokePrivilege(ScmSession ss, ScmRole role, String resourceType,
            String resource, String privilege) throws ScmException {
        ScmResource r = ScmResourceFactory.createResource(resourceType, resource);
        ScmFactory.Role.revokePrivilege(ss, role, r, privilege);
    }
}
