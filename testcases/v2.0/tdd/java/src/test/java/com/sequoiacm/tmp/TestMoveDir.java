package com.sequoiacm.tmp;

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

public class TestMoveDir extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestMoveDir.class);
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
    public void testMoveDir() throws ScmException, InterruptedException {
        String roleName = "ROLE_TEST";
        String userName = "testMoveDir1";
        String passwd = "testMoveDir";
        String workspaceName = getWorkspaceName();
        boolean isUserCreated = false;
        boolean isRoleCreated = false;

        String level1 = "/test_parent";
        boolean isLevel1Created = false;

        String level2Src = "src";
        String level2SrcPath = level1 + "/" + level2Src;
        boolean islevel2SrcCreated = false;

        String level2Target = "target";
        String level2TargetPath = level1 + "/" + level2Target;
        boolean islevel2TargetCreated = false;

        String level3Name = "name";
        String level3SrcPath = level2SrcPath + "/" + level3Name;
        String level3TargetPath = level2TargetPath + "/" + level3Name;
        boolean isLevel3SrcCreated = false;
        boolean isLevel3TargetCreated = false;

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
            String path = level1;
            ScmFactory.Directory.createInstance(ws, path);
            isLevel1Created = true;
            ScmResource r = ScmResourceFactory.createDirectoryResource(workspaceName, path);
            String priv = ScmPrivilegeDefine.join(ScmPrivilegeDefine.READ,
                    ScmPrivilegeDefine.CREATE, ScmPrivilegeDefine.DELETE);
            ScmFactory.Role.grantPrivilege(adminSS, role, r, priv);
            logger.info("grant privilege sucess:role={},resource={},privilege={}", roleName,
                    r.toStringFormat(), priv);

            logger.info("sleep 12 seconds to wait for privilege update");
            Thread.sleep(12 * 1000);
            displayPrivilege(adminSS, role);

            // create level2Src
            path = level1 + "/" + level2Src;
            TestPrivilegeCommon.createDir(url, userName, passwd, workspaceName, path);
            islevel2SrcCreated = true;

            // create level2Target
            path = level1 + "/" + level2Target;
            TestPrivilegeCommon.createDir(url, userName, passwd, workspaceName, path);
            islevel2TargetCreated = true;

            // create level3
            path = level1 + "/" + level2Src + "/" + level3Name;
            TestPrivilegeCommon.createDir(url, userName, passwd, workspaceName, path);
            isLevel3SrcCreated = true;

            String targetPath = level1 + "/" + level2Target;
            TestPrivilegeCommon.moveDir(url, userName, passwd, workspaceName, path, targetPath);
            isLevel3SrcCreated = false;
            isLevel3TargetCreated = true;
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

            if (isLevel3TargetCreated) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, level3TargetPath);
                logger.info("delete path:ws={},path={}", workspaceName, level3TargetPath);
            }

            if (isLevel3SrcCreated) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, level3SrcPath);
                logger.info("delete path:ws={},path={}", workspaceName, level3SrcPath);
            }

            if (islevel2SrcCreated) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, level2SrcPath);
                logger.info("delete path:ws={},path={}", workspaceName, level2SrcPath);
            }

            if (islevel2TargetCreated) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, level2TargetPath);
                logger.info("delete path:ws={},path={}", workspaceName, level2TargetPath);
            }

            if (isLevel1Created) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, level1);
                logger.info("delete path:ws={},path={}", workspaceName, level1);
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
