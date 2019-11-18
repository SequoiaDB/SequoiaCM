package org.sequoiacm.om.omserver.test.basic;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sequoiacm.om.omserver.test.ClientRespChecker;
import org.sequoiacm.om.omserver.test.OmServerTest;
import org.sequoiacm.om.omserver.test.ScmOmClient;
import org.sequoiacm.om.omserver.test.ScmOmTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmWorkspaceResource;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.om.omserver.common.RestParamDefine;
import com.sequoiacm.om.omserver.core.ScmOmPasswordMgr;
import com.sequoiacm.om.omserver.module.OmRoleInfo;
import com.sequoiacm.om.omserver.module.OmUserInfo;

import feign.Response;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OmServerTest.class)
public class TestUserRole {
    @Autowired
    ObjectMapper objMapper;

    @Autowired
    ScmOmClient client;

    @Autowired
    ClientRespChecker respChecker;

    @Autowired
    ScmSession session;

    @Autowired
    ScmOmTestConfig config;

    @Autowired
    ScmOmPasswordMgr passwordMgr;

    @Test
    public void test() throws Exception {
        Response resp = client.login(config.getScmUser(), config.getScmPassword());
        respChecker.check(resp);

        String sessionId = (String) resp.headers().get(RestParamDefine.X_AUTH_TOKEN).toArray()[0];

        final String userName = "TestUser";
        final String roleName = "TestRole";
        clear(userName, roleName);

        client.createUser(sessionId, userName, ScmUserPasswordType.TOKEN.name(),
                passwordMgr.encrypt(userName));
        client.createRole(sessionId, roleName, roleName + roleName);
        client.grantPrivilege(sessionId, roleName, ScmWorkspaceResource.RESOURCE_TYPE,
                config.getWorkspaceName1(), ScmPrivilegeType.READ.name(), "grant");
        client.grantRoles(sessionId, userName, Arrays.asList(roleName), "grant_role");

        OmUserInfo user = client.getUserInfo(sessionId, userName);
        Assert.assertEquals(user.getUserName(), userName);
        Assert.assertEquals(user.isEnable(), true);
        Assert.assertEquals(user.getUserType(), ScmUserPasswordType.TOKEN.name());
        Assert.assertEquals(user.getRoles().get(0).getRoleName(), "ROLE_" + roleName);
        Assert.assertEquals(user.getRoles().get(0).getDescription(), roleName + roleName);

        OmRoleInfo role = client.getRole(sessionId, roleName);
        Assert.assertEquals(role.getDescription(), roleName + roleName);
        Assert.assertEquals(role.getRoleName(), "ROLE_" + roleName);
        Assert.assertEquals(role.getResources().get(0).getPrivilege(),
                ScmPrivilegeType.READ.name());
        Assert.assertEquals(role.getResources().get(0).getType(),
                ScmWorkspaceResource.RESOURCE_TYPE);
        Assert.assertEquals(role.getResources().get(0).getResource(), config.getWorkspaceName1());

        client.revokePrivilege(sessionId, roleName, ScmWorkspaceResource.RESOURCE_TYPE,
                config.getWorkspaceName1(), ScmPrivilegeType.READ.name(), "revoke");
        role = client.getRole(sessionId, roleName);
        Assert.assertEquals(role.getResources().size(), 0);

        client.revokeRoles(sessionId, userName, Arrays.asList(roleName), "revoke_role");
        client.changeUserType(sessionId, userName, ScmUserPasswordType.LOCAL.name(),
                passwordMgr.encrypt(userName), passwordMgr.encrypt(""), "change_user_type");

        ScmFactory.Session
                .createSession(new ScmConfigOption(config.getGatewayAddr(), userName, userName))
                .close();

        client.changePassword(sessionId, userName, passwordMgr.encrypt(userName),
                passwordMgr.encrypt(userName + userName), false, "change_password");
        ScmFactory.Session
                .createSession(
                        new ScmConfigOption(config.getGatewayAddr(), userName, userName + userName))
                .close();

        client.disableUser(sessionId, userName, "disable");

        user = client.getUserInfo(sessionId, userName);
        Assert.assertEquals(user.getRoles().size(), 0);
        Assert.assertEquals(user.isEnable(), false);
        Assert.assertEquals(user.getUserType(), ScmUserPasswordType.LOCAL.name());

        client.deleteUser(sessionId, userName);
        client.deleteRole(sessionId, roleName);

        try {
            ScmUser u = ScmFactory.User.getUser(session, userName);
            Assert.fail("user still exists:" + u.getUsername());
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError().getErrorCode(),
                    ScmError.HTTP_NOT_FOUND.getErrorCode());
        }

        try {
            ScmRole r = ScmFactory.Role.getRole(session, roleName);
            Assert.fail("role still exists:" + r.getRoleName());
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError().getErrorCode(),
                    ScmError.HTTP_NOT_FOUND.getErrorCode());
        }

        client.logout(sessionId);
    }

    private void clear(String userName, String roleName) {
        try {
            ScmFactory.User.deleteUser(session, userName);
        }
        catch (ScmException e) {
        }

        try {
            ScmFactory.Role.deleteRole(session, ScmFactory.Role.getRole(session, roleName));
        }
        catch (ScmException e) {
        }

    }
}
