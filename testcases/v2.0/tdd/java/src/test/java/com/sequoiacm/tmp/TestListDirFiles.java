package com.sequoiacm.tmp;

import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmPrivilege;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.privilege.ScmPrivilegeDefine;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestListDirFiles extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestListDirFiles.class);
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
    public void testListDirFiles() throws ScmException, InterruptedException {
        String roleName = "ROLE_TEST";
        String userName = "testListDirFiles";
        String passwd = "testListDirFiles";
        String workspaceName = getWorkspaceName();
        boolean isUserCreated = false;
        boolean isRoleCreated = false;

        String level1 = "/test_parent";
        boolean isLevel1Created = false;

        String level2Src = "src";
        String level2SrcPath = level1 + "/" + level2Src;
        boolean islevel2SrcCreated = false;

        String fileId = null;
        ScmCursor<ScmDirectory> dCursor = null;
        ScmCursor<ScmFileBasicInfo> fCursor = null;

        ScmSession userSession = null;
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

            userSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(url, userName, passwd));
            ScmWorkspace userWS = ScmFactory.Workspace.getWorkspace(workspaceName, userSession);
            path = level1;
            ScmDirectory d = ScmFactory.Directory.getInstance(userWS, path);

            // create file in directory level1
            ScmFile file = ScmFactory.File.createInstance(userWS);
            String fileName = "aa";
            file.setFileName(fileName);
            file.setTitle("title");
            file.setDirectory(d);
            file.save();
            fileId = file.getFileId().get();

            dCursor = d.listDirectories(null);
            while (dCursor.hasNext()) {
                ScmDirectory tmp = dCursor.getNext();
                logger.info("directory:" + tmp);
            }
            dCursor.close();
            dCursor = null;

            fCursor = d.listFiles(new BasicBSONObject(ScmAttributeName.File.FILE_NAME, fileName));
            while (fCursor.hasNext()) {
                ScmFileBasicInfo tmp = fCursor.getNext();
                logger.info("listFiles:" + tmp);
            }

            fCursor.close();
            fCursor = null;

            ScmFile f = d.getSubfile(fileName);
            logger.info("file:" + f);
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

            if (null != fileId) {
                ScmTestTools.deleteScmFile(url, adminUser, adminPasswd, workspaceName, fileId);
            }

            if (islevel2SrcCreated) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, level2SrcPath);
                logger.info("delete path:ws={},path={}", workspaceName, level2SrcPath);
            }

            if (isLevel1Created) {
                TestPrivilegeCommon.deleteDirSilence(adminSS, workspaceName, level1);
                logger.info("delete path:ws={},path={}", workspaceName, level1);
            }

            if (null != fCursor) {
                fCursor.close();
            }

            if (null != dCursor) {
                dCursor.close();
            }

            if (null != userSession) {
                userSession.close();
            }
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmTestTools.releaseSession(adminSS);
    }
}
