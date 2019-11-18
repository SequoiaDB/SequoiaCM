
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
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description:SCM-1747:根目录有权限，在多级的子目录下操作业务
 * @author fanyu
 * @Date:2018年6月8日
 * @version:1.0
 */
public class AuthWs_RootDirHasPriv1747 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site;
    private ScmSession sessionA;
    private ScmWorkspace wsA;
    private String username = "AuthWs_RootDirHasPriv1747";
    private String rolename = "1747_0";
    private ScmPrivilegeType[] privileges = { ScmPrivilegeType.DELETE, ScmPrivilegeType.CREATE, ScmPrivilegeType.READ,
	    ScmPrivilegeType.UPDATE };
    private String passwd = "1747";
    private ScmUser user = null;
    private ScmRole role = null;
    private WsWrapper wsp;
    private String path = "/1747_A/1747_B/1747_C";
    private ScmResource rs = null;
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

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
	String testpath = path;
	String dirpath = path + "/1747_dir_new_0";
	String newDirname = "1747_dir_new_1";
	String newDirPath = path + "/1747_dir_new_1";
	ScmSession session = null;
	ScmWorkspace ws = null;
	String fileName = "1747_1";
	String newFileName = "1747_2";
	ScmId fileId = null;
	try {
	    session = TestScmTools.createSession(site, username, passwd);
	    ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
	    ScmDirectory dir = ScmFactory.Directory.getInstance(wsA, testpath);

	    // create file C+R
	    ScmFile file = ScmFactory.File.createInstance(ws);
	    file.setAuthor(fileName);
	    file.setFileName(fileName);
	    file.setDirectory(dir);
	    fileId = file.save();

	    // update file R+U
	    ScmFile updateFile = ScmFactory.File.getInstanceByPath(ws, path + "/" + fileName);
	    updateFile.setFileName(newFileName);

	    // read file R
	    ScmFile readFile = ScmFactory.File.getInstance(ws, fileId);
	    Assert.assertEquals(readFile.getFileName(), newFileName);

	    // read file
	    ScmFile readFile1 = dir.getSubfile(newFileName);
	    Assert.assertEquals(readFile1.getFileName(), newFileName);

	    // create dir
	    ScmFactory.Directory.createInstance(ws, dirpath);

	    // get dir
	    ScmDirectory actdir = ScmFactory.Directory.getInstance(ws, dirpath);

	    // rename dir
	    actdir.rename(newDirname);

	    // check
	    ScmDirectory actdir1 = ScmFactory.Directory.getInstance(ws, newDirPath);
	    Assert.assertEquals(actdir1.getName(), newDirname);
	    actdir1.delete();
	} catch (ScmException e) {
	    e.printStackTrace();
	    Assert.fail(e.getMessage());
	} finally {
	    if (fileId != null) {
		try {
		    ScmFactory.File.deleteInstance(ws, fileId, true);
		} catch (ScmException e) {
		    e.printStackTrace();
		    Assert.fail(e.getMessage());
		}
	    }
	    if (session != null) {
		session.close();
	    }
	}
	runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
	if (runSuccess || TestScmBase.forceClear) {
	    try {
		try {
		    for (ScmPrivilegeType privilege : privileges) {
			ScmFactory.Role.revokePrivilege(sessionA, role, rs, privilege);
		    }
		    ScmFactory.Role.deleteRole(sessionA, role);
		    ScmFactory.User.deleteUser(sessionA, user);
		    deleteDir(wsA, path);
		    TestTools.LocalFile.removeFile(localPath);
		} catch (ScmException e) {
		    e.printStackTrace();
		    Assert.fail(e.getMessage());
		}
	    } finally {
		if (sessionA != null) {
		    sessionA.close();
		}
	    }
	}
    }

    private void grantPriAndAttachRole(ScmSession session, ScmResource rs, ScmUser user, ScmRole role,
	    ScmPrivilegeType privileges) throws ScmException {
	ScmUserModifier modifier = new ScmUserModifier();
	ScmFactory.Role.grantPrivilege(sessionA, role, rs, privileges);
	modifier.addRole(role);
	ScmFactory.User.alterUser(sessionA, user, modifier);
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
	List<String> pathList = new ArrayList<>();
	for (int i = 1; i < arry.length; i++) {
	    ele = ele + arry[i];
	    pathList.add(ele);
	    ele = ele + "/";
	}
	return pathList;
    }

    private void cleanEnv() {
	deleteDir(wsA, path);
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
	createDir(wsA, path);
	user = ScmFactory.User.createUser(sessionA, username, ScmUserPasswordType.LOCAL, passwd);
	role = ScmFactory.Role.createRole(sessionA, rolename, null);
	rs = ScmResourceFactory.createDirectoryResource(wsp.getName(), "/");
	for (ScmPrivilegeType privilege : privileges) {
	    grantPriAndAttachRole(sessionA, rs, user, role, privilege);
	}
	String pri = "";
	for (int i = 0; i < privileges.length; i++) {
	    if (i < privileges.length - 1) {
		pri += privileges[i].toString() + "|";
	    } else if (i == privileges.length - 1) {
		pri += privileges[i].toString();
	    }
	}
		ScmAuthUtils.checkPriority(site, username, passwd, role, wsp);
    }
}
