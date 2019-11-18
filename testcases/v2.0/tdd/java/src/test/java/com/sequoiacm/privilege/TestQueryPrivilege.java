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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestQueryPrivilege extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestQueryPrivilege.class);
    private ScmSession adminSS;
    private String adminUser = "admin";
    private String adminPasswd = "admin";
    private ScmId fileId;
    String url;

    @BeforeClass
    public void setUp() throws ScmException {
        url = getServer2().getUrl();
        logger.info("user={}, passwd={}", adminUser, adminPasswd);
        adminSS = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(url, adminUser, adminPasswd));
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
        String testFuncName = "expectCreateFileFailed" + System.currentTimeMillis();
        ScmSession tmpSession = null;
        ScmWorkspace ws = null;
        tmpSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(url, userName, passwd));
        ws = ScmFactory.Workspace.getWorkspace(workspaceName, tmpSession);

        try {
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName(ScmTestTools.getClassName());
            file.setAuthor("test");
            file.setTitle("sequoiacm");
            file.setMimeType(MimeType.PLAIN);
            file.save();
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
        String testFuncName = "expectCreateFileSuccess" + System.currentTimeMillis();
        ScmSession tmpSession = null;
        ScmWorkspace ws = null;
        tmpSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(url, userName, passwd));
        ws = ScmFactory.Workspace.getWorkspace(workspaceName, tmpSession);

        try {
            ScmFactory.Directory.createInstance(ws, "/a/b");
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName(ScmTestTools.getClassName());
            file.setAuthor("test");
            file.setTitle("sequoiacm");
            file.setMimeType(MimeType.PLAIN);
            file.save();
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

    public void testListResource() throws ScmException {

        String wsName = "ws_default";
        ScmCursor<ScmResource> cursor = ScmFactory.Resource.listResourceByWorkspace(adminSS,
                wsName);
        while (cursor.hasNext()) {
            ScmResource r = cursor.getNext();
            logger.info("type={},resource={}", r.getType(), r.toStringFormat());

            ScmCursor<ScmPrivilege> pCursor = ScmFactory.Privilege.listPrivilegesByResource(adminSS,
                    r);
            while (pCursor.hasNext()) {
                ScmPrivilege p = pCursor.getNext();
                logger.info("privilegeId={}, roleName={}, resource={}, privilege={}", p.getId(),
                        p.getRole().getRoleName(), p.getResource().toStringFormat(),
                        p.getPrivilege());
            }
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
        String dirA1 = "/AuthDir_Create1726";
        String dirA2 = "/AuthDir_Create1726/1726_A";
        String dirA3 = "/AuthDir_Create1726/1726_A/1726_B";
        String dirA = "/AuthDir_Create1726/1726_A/1726_B/1726_C";
        boolean dirACreated = false;
        String dirB = "/AuthDir_Create1726/1726_A/1726_B/1726_C/1726_D";
        boolean dirBCreated = false;
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

            // delete dirA, expect failure
            testCreateUnAuthDir(url, userName, passwd, workspaceName, dirA);

            // grant dirA READ and CREATE
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(workspaceName, adminSS);
            // /AuthDir_Create1726/1726_A/1726_B/1726_C
            ScmFactory.Directory.createInstance(ws, dirA1);
            dirACreated = true;
            ScmFactory.Directory.createInstance(ws, dirA2);
            ScmFactory.Directory.createInstance(ws, dirA3);
            ScmFactory.Directory.createInstance(ws, dirA);
            ScmResource r = ScmResourceFactory.createDirectoryResource(workspaceName, dirA);
            String priv = ScmPrivilegeDefine.join(ScmPrivilegeDefine.READ,
                    ScmPrivilegeDefine.CREATE);
            ScmFactory.Role.grantPrivilege(adminSS, role, r, priv);
            logger.info("grant privilege sucess:role={},resource={},privilege={}", roleName,
                    r.toStringFormat(), priv);

            logger.info("sleep 12 seconds to wait for privilege update");
            Thread.sleep(12 * 1000);
            displayPrivilege(adminSS, role);

            // delete dirA, expect failure
            testDeleteUnAuthDir(url, userName, passwd, workspaceName, dirA);

            // create dirB
            TestPrivilegeCommon.createDir(url, userName, passwd, workspaceName, dirB);
            TestPrivilegeCommon.getDir(url, userName, passwd, workspaceName, dirB);

            // delete dirB, expect failure
            testDeleteUnAuthDir(url, userName, passwd, workspaceName, dirB);

            // grant dirA DELETE
            ScmFactory.Role.grantPrivilege(adminSS, role, r, ScmPrivilegeDefine.DELETE);

            logger.info("grant privilege sucess:role={},resource={},privilege={}", roleName,
                    r.toStringFormat(), ScmPrivilegeDefine.DELETE);
            logger.info("sleep 12 seconds to wait for privilege update");
            Thread.sleep(12 * 1000);
            displayPrivilege(adminSS, role);

            // delete dirB, expect success
            TestPrivilegeCommon.deleteDir(url, userName, passwd, workspaceName, dirB);

            // delete dirA, expect failure
            testDeleteUnAuthDir(url, userName, passwd, workspaceName, dirA2);

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

            if (dirACreated) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, dirA);
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, dirA3);
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, dirA2);
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, dirA1);
                logger.info("delete path:ws={},path={}", workspaceName, dirA);
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
