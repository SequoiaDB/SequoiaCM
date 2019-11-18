package com.sequoiacm.client.privilege;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmPrivilege;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeDefine;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.element.privilege.ScmWorkspaceResource;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.ScmTestBase;
import com.sequoiacm.client.util.ScmTestTools;

public class TestQueryPrivilege extends ScmTestBase {

    private final static Logger logger = LoggerFactory.getLogger(TestQueryPrivilege.class);
    private ScmSession adminSS;
    private String adminUser = "admin";
    private String adminPasswd = "admin";
    private ScmId fileId;

    @BeforeClass
    public void setUp() throws ScmException {
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

    private void expectCreateFileFailed(String userName, String passwd, String workspaceName)
            throws ScmException {
        String testFuncName = "expectCreateFileFailed";
        ScmSession tmpSession = null;
        ScmWorkspace ws = null;
        tmpSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(url, userName, passwd));
        ws = ScmFactory.Workspace.getWorkspace(workspaceName, tmpSession);

        try {
            ScmFile file = ScmTestTools.createFile(tmpSession, workspaceName, testFuncName,
                    testFuncName);
            fileId = file.getFileId();
            Assert.fail("do not have priority, can't create");
        }
        catch (Exception e) {
            // do nothing
        }
        finally {
            if (null != fileId) {
                ScmFactory.File.deleteInstance(ws, fileId, true);
            }

            ScmTestTools.releaseSession(tmpSession);
        }
    }

    private void expectCreateFileSuccess(String userName, String passwd, String workspaceName)
            throws ScmException {
        String testFuncName = "expectCreateFileSuccess";
        ScmSession tmpSession = null;
        ScmWorkspace ws = null;
        tmpSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(url, userName, passwd));
        ws = ScmFactory.Workspace.getWorkspace(workspaceName, tmpSession);

        try {
            ScmFile file = ScmTestTools.createFile(tmpSession, workspaceName, testFuncName,
                    testFuncName);
            fileId = file.getFileId();
        }
        finally {
            if (null != fileId) {
                ScmFactory.File.deleteInstance(ws, fileId, true);
            }

            ScmTestTools.releaseSession(tmpSession);
        }
    }

    public void testDeleteRole() throws ScmException {
        ScmSession tmpSession = null;
        tmpSession = ScmFactory.Session.createSession(SessionType.NOT_AUTH_SESSION,
                new ScmConfigOption(url, "", ""));
        ScmCursor<ScmUser> cursor = ScmFactory.User.listUsers(tmpSession);
        while (cursor.hasNext()) {
            ScmUser u = cursor.getNext();
            logger.info("userId={},userName={}", u.getUsername(), u.getUsername());
        }

        //
        // ScmFactory.Role.deleteRole(adminSS, "ROLE_ws_default");
        // ScmFactory.User.deleteUser(adminSS, "scmUser");
        // ScmRole role = ScmFactory.Role.getRole(adminSS, "ROLE_ws_default");
        //
        // ScmResource r =
        // ScmResourceFactory.createResource(ScmWorkspaceResource.RESOURCE_TYPE,
        // "ws_default");
        //
        // ScmFactory.Role.revokePrivilege(adminSS, role, r,
        // ScmPrivilegeDefine.READ);
    }

    @Test
    public void testListResource() throws ScmException {

        String wsName = "ws_default";
        ScmCursor<ScmResource> cursor = ScmFactory.Resource
                .listResourceByWorkspace(adminSS, wsName);
        while (cursor.hasNext()) {
            ScmResource r = cursor.getNext();
            logger.info("type={},resource={}", r.getType(), r.toStringFormat());

            ScmCursor<ScmPrivilege> pCursor = ScmFactory.Privilege.listPrivilegesByResource(
                    adminSS, r);
            while (pCursor.hasNext()) {
                ScmPrivilege p = pCursor.getNext();
                logger.info("privilegeId={}, roleName={}, resource={}, privilege={}", p.getId(), p
                        .getRole().getRoleName(), p.getResource().toStringFormat(), p
                        .getPrivilege());
            }
        }
    }

    public void testCreateRole() throws ScmException, InterruptedException {
        String roleName = "ROLE_TEST";
        String userName = "TestQueryPrivilege";
        String passwd = "TestQueryPrivilege";
        String workspaceName = ScmTestBase.workspaceName;
        boolean isUserCreated = false;
        boolean isRoleCreated = false;

        try {
            // create user
            ScmUser user = TestPrivilegeCommon.createUser(adminSS, userName, passwd);
            isUserCreated = true;
            logger.info("create user sucess:user={}", userName);

            // expectCreateFileFailed
            logger.info("creating file before grant");
            expectCreateFileFailed(userName, passwd, workspaceName);

            // create role
            ScmRole role = TestPrivilegeCommon.createRole(adminSS, roleName);
            isRoleCreated = true;
            user = TestPrivilegeCommon.associateRole(adminSS, user, role);
            logger.info("create role sucess:role={},user={}", roleName, userName);

            ScmResource r = ScmResourceFactory.createResource(ScmWorkspaceResource.RESOURCE_TYPE,
                    workspaceName);
            String priv = ScmPrivilegeDefine.join(ScmPrivilegeDefine.ALL);
            ScmFactory.Role.grantPrivilege(adminSS, role, r, priv);
            logger.info("grant privilege sucess:role={},resource={},privilege={}", roleName,
                    r.toStringFormat(), priv);

            logger.info("sleep 8 seconds to wait for privilege update");
            Thread.sleep(8 * 1000);
            displayPrivilege(adminSS, role);

            // expectCreateFileSuccess
            logger.info("try to create file:user={}", userName);
            expectCreateFileSuccess(userName, passwd, workspaceName);
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
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmTestTools.releaseSession(adminSS);
    }
}
