
package com.sequoiacm.auth;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author fanyu
 * @Description:SCM-1730:有工作区和目录的权限，对表格中的各个接口进行覆盖测试
 * @Date:2018年6月13日
 * @version:1.0
 */
public class AuthWsDir_CreateRead1730 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession sessionA;
    private ScmSession sessionCRUD;
    private ScmWorkspace wsA;
    private ScmWorkspace wsCRUD;
    private String username = "AuthWsDir_CreateRead1730";
    private String rolename = "ROLE_1730_CR";
    private String passwd = "1730";
    private ScmUser user;
    private ScmRole role;
    private ScmResource dirrs;
    private ScmResource wsrs;
    private String basepath = "/AuthWsDir_CreateRead1730";
    private String path = basepath + "/1730_A/1730_B/1730_C";
    private String author = "AuthWsDir_CreateRead1730";
    private WsWrapper wsp;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        try {
            localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
            filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
            TestTools.LocalFile.removeFile(localPath);
            TestTools.LocalFile.createDir(localPath.toString());
            TestTools.LocalFile.createFile(filePath, fileSize);

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            sessionA = TestScmTools.createSession(site);
            wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
            cleanEnv();
            prepare();
        } catch (ScmException e) {
            e.printStackTrace();
        }
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void testCreateDir() {
        String subpath = path + "/1726_D";
        try {
            ScmDirectory expdir = ScmFactory.Directory.createInstance(wsCRUD, subpath);
            ScmDirectory actdir = ScmFactory.Directory.getInstance(wsCRUD, subpath);
            Assert.assertEquals(expdir.getPath(), actdir.getPath());
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void testCreateFileInDir() throws ScmException {
        ScmId fileId = null;
        String fileName = author + "_" + UUID.randomUUID();
        String subpath = path + "/1726_E";
        try {
            // create dir
            ScmDirectory actdir = ScmFactory.Directory.createInstance(wsCRUD, subpath);

            // CreateFileInDir
            ScmFile file = ScmFactory.File.createInstance(wsCRUD);
            file.setAuthor(author);
            file.setFileName(fileName);
            file.setDirectory(actdir);
            fileId = file.save();

            // check
            ScmDirectory dir = ScmFactory.Directory.getInstance(wsCRUD, subpath);
            ScmFile actfile = dir.getSubfile(fileName);
            Assert.assertEquals(actfile.getDirectory().getPath(), subpath + "/");
            ScmFactory.File.deleteInstance(wsA, fileId, true);
            dir.delete();
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmFactory.Role.revokePrivilege(sessionA, role, wsrs, ScmPrivilegeType.CREATE);
            ScmFactory.Role.revokePrivilege(sessionA, role, wsrs, ScmPrivilegeType.READ);
            ScmFactory.Role.revokePrivilege(sessionA, role, dirrs, ScmPrivilegeType.UPDATE);
            ScmFactory.Role.revokePrivilege(sessionA, role, dirrs, ScmPrivilegeType.DELETE);
            ScmFactory.Role.deleteRole(sessionA, role);
            ScmFactory.User.deleteUser(sessionA, user);
            deleteDir(wsA, path + "/1726_D");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (sessionA != null) {
                sessionA.close();
            }
        }
    }

    private void grantPriAndAttachRole(ScmSession session, ScmResource rs, ScmUser user, ScmRole role,
                                       ScmPrivilegeType privileges) {
        try {
            ScmUserModifier modifier = new ScmUserModifier();
            ScmFactory.Role.grantPrivilege(sessionA, role, rs, privileges);
            modifier.addRole(role);
            ScmFactory.User.alterUser(sessionA, user, modifier);
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private ScmDirectory createDir(ScmWorkspace ws, String dirPath) throws ScmException {
        List<String> pathList = getSubPaths(dirPath);
        for (String path : pathList) {
            try {
                ScmFactory.Directory.createInstance(ws, path);
            } catch (ScmException e) {
                if (e.getError() != ScmError.DIR_EXIST) {
                    e.printStackTrace();
                    Assert.fail(e.getMessage());
                }
            }
        }
        return ScmFactory.Directory.getInstance(ws, pathList.get(pathList.size() - 1));
    }

    private void deleteDir(ScmWorkspace ws, String dirPath) {
        List<String> pathList = getSubPaths(dirPath);
        for (int i = pathList.size() - 1; i >= 0; i--) {
            try {
                ScmFactory.Directory.deleteInstance(ws, pathList.get(i));
            } catch (ScmException e) {
                if (e.getError() != ScmError.DIR_NOT_FOUND && e.getError() != ScmError.DIR_NOT_EMPTY) {
                    e.printStackTrace();
                    Assert.fail(e.getMessage());
                }
            }
        }
    }

    private List<String> getSubPaths(String path) {
        String ele = "/";
        String[] arry = path.split("/");
        List<String> pathList = new ArrayList<String>();
        for (int i = 1; i < arry.length; i++) {
            ele = ele + arry[i];
            pathList.add(ele);
            ele = ele + "/";
        }
        return pathList;
    }

    private void cleanEnv() throws ScmException {
        BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
        ScmFileUtils.cleanFile(wsp, cond);
        try {
            ScmFactory.Role.deleteRole(sessionA, rolename);
        } catch (ScmException e) {
            if (e.getError() != ScmError.HTTP_NOT_FOUND) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
        try {
            ScmFactory.User.deleteUser(sessionA, username);
        } catch (ScmException e) {
            if (e.getError() != ScmError.HTTP_NOT_FOUND) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
    }

    private void prepare() throws Exception {
        try {
            user = ScmFactory.User.createUser(sessionA, username, ScmUserPasswordType.LOCAL, passwd);
            role = ScmFactory.Role.createRole(sessionA, rolename, null);

            dirrs = ScmResourceFactory.createDirectoryResource(wsp.getName(), path);
            System.out.println("dirrs = " + dirrs.getType() + ",dirrs = " + dirrs.toStringFormat());
            wsrs = ScmResourceFactory.createWorkspaceResource(wsp.getName());
            deleteDir(wsA, path + "/1726_D");
            deleteDir(wsA, path + "/1726_E");
            createDir(wsA, path);

            grantPriAndAttachRole(sessionA, dirrs, user, role, ScmPrivilegeType.UPDATE);
            grantPriAndAttachRole(sessionA, dirrs, user, role, ScmPrivilegeType.DELETE);

            grantPriAndAttachRole(sessionA, wsrs, user, role, ScmPrivilegeType.CREATE);
            grantPriAndAttachRole(sessionA, wsrs, user, role, ScmPrivilegeType.READ);

            ScmAuthUtils.checkPriority(site, username, passwd, role, wsp);

            sessionCRUD = TestScmTools.createSession(site, username, passwd);
            wsCRUD = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionCRUD);
        } catch (ScmException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
}
