package com.sequoiacm.privilege;

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

public class TestRenameDir extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestRenameDir.class);
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
    public void testOpNoAuthDir() throws ScmException, InterruptedException {
        String roleName = "ROLE_TEST";
        String userName = "TestQueryPrivilege";
        String passwd = "TestQueryPrivilege";
        String workspaceName = getWorkspaceName();
        boolean isUserCreated = false;
        boolean isRoleCreated = false;
        String parentDir = "/AuthDir_Create1726";
        boolean parentDirCreated = false;
        String dirB = "/AuthDir_Create1726/1726_B";
        boolean dirBCreated = false;

        boolean newNameCreated = false;
        String newName = "newName";

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
            ScmFactory.Directory.createInstance(ws, parentDir);
            parentDirCreated = true;
            ScmResource r = ScmResourceFactory.createDirectoryResource(workspaceName, parentDir);
            String priv = ScmPrivilegeDefine.join(ScmPrivilegeDefine.READ,
                    ScmPrivilegeDefine.CREATE, ScmPrivilegeDefine.DELETE);
            ScmFactory.Role.grantPrivilege(adminSS, role, r, priv);
            logger.info("grant privilege sucess:role={},resource={},privilege={}", roleName,
                    r.toStringFormat(), priv);

            logger.info("sleep 12 seconds to wait for privilege update");
            Thread.sleep(12 * 1000);
            displayPrivilege(adminSS, role);

            // create dirB
            TestPrivilegeCommon.createDir(url, userName, passwd, workspaceName, dirB);
            dirBCreated = true;

            TestPrivilegeCommon.rename(url, userName, passwd, workspaceName, dirB, newName);
            newNameCreated = true;
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

            if (dirBCreated) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, dirB);
                logger.info("delete path:ws={},path={}", workspaceName, dirB);
            }

            if (newNameCreated) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, parentDir + "/"
                        + newName);
            }

            if (parentDirCreated) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, parentDir);
                logger.info("delete path:ws={},path={}", workspaceName, parentDir);
            }
        }
    }

    private void testCreateUnAuthDir(String url, String userName, String passwd,
            String workspaceName, String path) {

        try {
            TestPrivilegeCommon.createDir(url, userName, passwd, workspaceName, path);
        }
        catch (Exception e) {
            return;
        }

        Assert.assertTrue(false, "can't create path without auth:userName=" + userName
                + ",workspace=" + workspaceName + ",path=" + path);
    }

    private void testDeleteUnAuthDir(String url, String userName, String passwd,
            String workspaceName, String path) {
        try {
            TestPrivilegeCommon.deleteDir(url, userName, passwd, workspaceName, path);
        }
        catch (Exception e) {
            return;
        }

        Assert.assertTrue(false, "can't delete path without auth:userName=" + userName
                + ",workspace=" + workspaceName + ",path=" + path);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmTestTools.releaseSession(adminSS);
    }
}
