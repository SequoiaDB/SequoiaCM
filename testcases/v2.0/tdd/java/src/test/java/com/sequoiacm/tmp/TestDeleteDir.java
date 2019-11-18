package com.sequoiacm.tmp;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmPrivilege;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.privilege.ScmPrivilegeDefine;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestDeleteDir extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestDeleteDir.class);
    private ScmSession adminSS;
    private String adminUser = "admin";
    private String adminPasswd = "admin";
    String url;

    @BeforeClass
    public void setUp() throws ScmException {
        url = getServer2().getUrl();
        logger.info("user={}, passwd={}", adminUser, adminPasswd);
        adminSS = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                url, adminUser, adminPasswd));
    }

    private void displayPrivilege(ScmSession session, ScmRole role) throws ScmException {
        // display role's privileges
        ScmCursor<ScmPrivilege> ps = ScmFactory.Privilege.listPrivileges(session, role);
        try {
            while (ps.hasNext()) {
                ScmPrivilege p = ps.getNext();
                ScmResource tmpResource = p.getResource();
                ScmRole tmpRole = p.getRole();
                logger.info("id={}, type={}, resource={}, privilege={}, roleName={}", p.getId(),
                        tmpResource.getType(), tmpResource.toStringFormat(), p.getPrivilege(),
                        tmpRole.getRoleName());
            }
        }
        finally {
            ps.close();
        }
    }

    @Test
    public void testDeleteDir() throws ScmException, InterruptedException {
        String roleName = "ROLE_TEST";
        String userName = "testDeleteDir";
        String passwd = "testDeleteDir";
        String workspaceName = getWorkspaceName();
        boolean isUserCreated = false;
        boolean isRoleCreated = false;

        String dirA = "/a";
        boolean isDirACreated = false;

        String dirA123 = "/a_123";
        boolean isDirA123Created = false;
        try {
            // create user
            ScmUser user = TestPrivilegeCommon.createUser(adminSS, userName, passwd);
            isUserCreated = true;
            logger.info("create user sucess:user={}", userName);

            // create role
            ScmRole role = TestPrivilegeCommon.createRole(adminSS, roleName);
            isRoleCreated = true;
            user = TestPrivilegeCommon.associateRole(adminSS, user, role);
            logger.info("create role sucess:role={},user={}", roleName, userName);

            // grant dirA READ and CREATE
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, adminSS);
            ScmFactory.Directory.createInstance(ws, dirA);
            isDirACreated = true;
            ScmFactory.Directory.createInstance(ws, dirA123);
            isDirA123Created = true;

            ScmResource r = ScmResourceFactory.createDirectoryResource(workspaceName, dirA);
            String priv = ScmPrivilegeDefine.join(ScmPrivilegeDefine.DELETE);
            ScmFactory.Role.grantPrivilege(adminSS, role, r, priv);
            logger.info("grant privilege sucess:role={},resource={},privilege={}", roleName,
                    r.toStringFormat(), priv);

            r = ScmResourceFactory.createWorkspaceResource(workspaceName);
            priv = ScmPrivilegeDefine.join(ScmPrivilegeDefine.CREATE, ScmPrivilegeDefine.UPDATE,
                    ScmPrivilegeDefine.READ);
            ScmFactory.Role.grantPrivilege(adminSS, role, r, priv);
            logger.info("grant privilege sucess:role={},resource={},privilege={}", roleName,
                    r.toStringFormat(), priv);

            logger.info("sleep 12 seconds to wait for privilege update");
            Thread.sleep(12 * 1000);
            displayPrivilege(adminSS, role);

            boolean deleteFailed = false;
            try {
                TestPrivilegeCommon.deleteDir(url, userName, passwd, workspaceName, dirA123);
            }
            catch (ScmException e) {
                logger.error("", e);
                deleteFailed = true;
            }

            Assert.assertTrue(deleteFailed);
        }
        finally {
            if (isRoleCreated) {
                TestPrivilegeCommon.deleteRole(adminSS, roleName);
                logger.info("delete role:role={}", roleName);
            }

            if (isUserCreated) {
                TestPrivilegeCommon.deleteUser(adminSS, userName);
                logger.info("delete user:user={}", userName);
            }

            if (isDirACreated) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, dirA);
                logger.info("delete path:ws={},path={}", workspaceName, dirA);
            }

            if (isDirA123Created) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, dirA123);
                logger.info("delete path:ws={},path={}", workspaceName, dirA123);
            }
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmTestTools.releaseSession(adminSS);
    }
}
