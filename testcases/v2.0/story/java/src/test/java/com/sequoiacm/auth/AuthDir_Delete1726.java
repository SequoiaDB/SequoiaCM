
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
 * @Description:SCM-1726 :: 有目录资源Delete权限，对表格中各个接口进行覆盖测试
 * @author fanyu
 * @Date:2018年6月12日
 * @version:1.0
 */
public class AuthDir_Delete1726 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession sessionA;
    private ScmSession sessionD;
    private ScmWorkspace wsA;
    private ScmWorkspace wsD;
    private String username = "AuthDir_Delete1726";
    private String rolename = "Role_1726_D";
    private String passwd = "1726";
    private ScmUser user;
    private ScmRole role;
    private ScmResource rs;
    private String basepath = "/AuthDir_Delete1726";
    private String path = basepath + "/1726_A";
    private String author = "AuthDir_Delete1726";
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
	    Assert.fail(e.getMessage());
	}
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testDeleteFile() throws ScmException {
	String fileName = author + "_" + UUID.randomUUID();
	ScmId fileId = null;
	ScmDirectory dir = null;
	String dirPath = path;
	try {
	    dir = ScmFactory.Directory.getInstance(wsA, dirPath);
	    ScmFile expfile = ScmFactory.File.createInstance(wsA);
	    expfile.setFileName(fileName);
	    expfile.setDirectory(dir);
	    expfile.setContent(filePath);
	    fileId = expfile.save();

	    ScmFactory.File.deleteInstance(wsD, fileId, true);
	    fileId = null;
	} catch (ScmException e) {
	    e.printStackTrace();
	    Assert.fail(e.getMessage());
	} finally {
	    if (fileId != null) {
		ScmFactory.File.deleteInstance(wsA, fileId, true);
	    }
	}
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testDeleteScmDir() throws ScmException {
	String dirpath = path + "/" + author + "_" + UUID.randomUUID();
	ScmDirectory expDir = null;
	try {
	    expDir = ScmFactory.Directory.createInstance(wsA, dirpath);
	    ScmFactory.Directory.deleteInstance(wsD, dirpath);
	    expDir = null;
	} catch (ScmException e) {
	    e.printStackTrace();
	    Assert.fail(e.getMessage());
	} finally {
	    if (expDir != null) {
		ScmFactory.Directory.deleteInstance(wsA, dirpath);
	    }
	}
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
	try {
	    ScmFactory.Role.revokePrivilege(sessionA, role, rs, ScmPrivilegeType.DELETE);
	    ScmFactory.Role.deleteRole(sessionA, role);
	    ScmFactory.User.deleteUser(sessionA, user);
	    deleteDir(wsA, path);
	    TestTools.LocalFile.removeFile(localPath);
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

	    rs = ScmResourceFactory.createDirectoryResource(wsp.getName(), path);
	    deleteDir(wsA, path);
	    createDir(wsA, path);
	    grantPriAndAttachRole(sessionA, rs, user, role, ScmPrivilegeType.DELETE);
		ScmAuthUtils.checkPriority(site, username, passwd, role, wsp.getName());
	    sessionD = TestScmTools.createSession(site, username, passwd);
	    wsD = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionD);
	} catch (ScmException e) {
	    e.printStackTrace();
	    Assert.fail(e.getMessage());
	}
    }
}
