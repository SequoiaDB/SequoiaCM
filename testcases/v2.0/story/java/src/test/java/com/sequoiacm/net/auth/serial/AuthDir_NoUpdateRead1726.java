
package com.sequoiacm.net.auth.serial;

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
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
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
 * @Description:SCM-1726 :: 有目录资源UPDATE/READ权限，对表格中各个接口进行覆盖测试
 * @author fanyu
 * @Date:2018年6月12日
 * @version:1.0
 */
public class AuthDir_NoUpdateRead1726 extends TestScmBase {
    private SiteWrapper branchsite;
    private SiteWrapper rootsite;
    private ScmSession sessionA;
    private ScmSession sessionUR;
    private ScmWorkspace wsA;
    private ScmWorkspace wsUR;
    private String username = "AuthDir_NUpdateRead1726";
    private String rolename = "Role_1726_NUR";
    private String passwd = "1726";
    private ScmUser user;
    private ScmRole role;
    private ScmResource rs;
    private String basepath = "/AuthDir_NUpdateRead1726";
    private String path = basepath + "/1726_A";
    private String path1 = basepath + "/1726_B";
    private String author = "AuthDir_UpdateRead1726";
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

	    wsp = ScmInfo.getWs();
	    List<SiteWrapper> sites = ScmNetUtils.getSortSites(wsp);
	    rootsite = sites.get(1);
	    branchsite = sites.get(0);

	    sessionA = TestScmTools.createSession(rootsite);
	    wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
	    cleanEnv();
	    prepare();
	} catch (ScmException e) {
	    e.printStackTrace();
	}
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testUpdateFile() throws ScmException {
	String fileName = author + "_" + UUID.randomUUID();
	String newfileName = author + "_" + UUID.randomUUID();
	ScmId fileId = null;
	ScmDirectory dir = null;
	String dirPath = path;
	try {
	    // getdir
	    dir = ScmFactory.Directory.getInstance(wsA, dirPath);
	    ScmFile expfile = ScmFactory.File.createInstance(wsA);
	    expfile.setFileName(fileName);
	    expfile.setDirectory(dir);
	    fileId = expfile.save();

	    ScmFile actfile = ScmFactory.File.getInstance(wsUR, fileId);
	    actfile.setFileName(newfileName);
	    Assert.fail("the user does not have priority to do something");
	} catch (ScmException e) {
	    if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
		e.printStackTrace();
		Assert.fail(e.getMessage());
	    }
	} finally {
	    if (fileId != null) {
		ScmFactory.File.deleteInstance(wsA, fileId, true);
	    }
	}
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testMoveFile() throws ScmException {
	String dirpath = path;
	String newpath = path1;
	String fileName = author + UUID.randomUUID();
	ScmId fileId = null;
	try {
	    ScmDirectory dir = ScmFactory.Directory.getInstance(wsA, dirpath);
	    ScmFile expfile = ScmFactory.File.createInstance(wsA);
	    expfile.setFileName(fileName);
	    expfile.setAuthor(author);
	    expfile.setDirectory(dir);
	    fileId = expfile.save();

	    // move file
	    ScmFile file = ScmFactory.File.getInstanceByPath(wsUR, dirpath + "/" + fileName);
	    ScmDirectory newDir = ScmFactory.Directory.getInstance(wsUR, newpath);
	    file.setDirectory(newDir);
	    Assert.fail("the user does not have priority to do something");
	} catch (ScmException e) {
	    if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
		e.printStackTrace();
		Assert.fail(e.getMessage());
	    }
	} finally {
	    if (fileId != null) {
		ScmFactory.File.deleteInstance(wsA, fileId, true);
	    }
	}
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAsynCaheFile() throws ScmException {
	String fileName = author + "_" + UUID.randomUUID();
	ScmId fileId = null;
	ScmDirectory dir = null;
	String dirPath = path;
	try {
	    // get dir
	    dir = ScmFactory.Directory.getInstance(wsA, dirPath);
	    ScmFile expfile = ScmFactory.File.createInstance(wsA);
	    expfile.setFileName(fileName);
	    expfile.setDirectory(dir);
	    fileId = expfile.save();

	    ScmFactory.File.asyncCache(wsUR, fileId);
	    Assert.fail("the user does not have priority to do something");
	} catch (ScmException e) {
	    if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
		e.printStackTrace();
		Assert.fail(e.getMessage());
	    }
	} finally {
	    if (fileId != null) {
		ScmFactory.File.deleteInstance(wsA, fileId, true);
	    }
	}
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAsynCaheFileByVersion() throws ScmException {
	String fileName = author + "_" + UUID.randomUUID();
	ScmId fileId = null;
	ScmDirectory dir = null;
	String dirPath = path;
	try {
	    // get dir
	    dir = ScmFactory.Directory.getInstance(wsA, dirPath);
	    ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
	    ScmFile expfile = ScmFactory.File.createInstance(ws);
	    expfile.setFileName(fileName);
	    expfile.setDirectory(dir);
	    expfile.setContent(filePath);
	    fileId = expfile.save();

	    ScmFactory.File.asyncCache(wsUR, fileId, 1, 0);
	    Assert.fail("the user does not have priority to do something");
	} catch (ScmException e) {
	    if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
		e.printStackTrace();
		Assert.fail(e.getMessage());
	    }
	} finally {
	    if (fileId != null) {
		ScmFactory.File.deleteInstance(wsA, fileId, true);
	    }
	}
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAsynTransfer() throws ScmException {
	ScmSession session = null;
	String fileName = author + "_" + UUID.randomUUID();
	ScmId fileId = null;
	ScmDirectory dir = null;
	String dirPath = path;
	try {
	    session = TestScmTools.createSession(branchsite);
	    ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
	    // get dir
	    dir = ScmFactory.Directory.getInstance(ws, dirPath);
	    ScmFile expfile = ScmFactory.File.createInstance(ws);
	    expfile.setFileName(fileName);
	    expfile.setDirectory(dir);
	    expfile.setContent(filePath);
	    fileId = expfile.save();

	    ScmFactory.File.asyncTransfer(wsUR, fileId);
	    Assert.fail("the user does not have priority to do something");
	} catch (ScmException e) {
	    if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
		e.printStackTrace();
		Assert.fail(e.getMessage());
	    }
	} finally {
	    if (fileId != null) {
		ScmFactory.File.deleteInstance(wsA, fileId, true);
	    }
	    if (session != null) {
		session.close();
	    }
	}
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAsynTransferByVersion() throws ScmException {
	ScmSession session = null;
	String fileName = author + "_" + UUID.randomUUID();
	ScmId fileId = null;
	ScmDirectory dir = null;
	String dirPath = path;
	try {
	    session = TestScmTools.createSession(branchsite);
	    ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
	    // get dir
	    dir = ScmFactory.Directory.getInstance(ws, dirPath);
	    ScmFile expfile = ScmFactory.File.createInstance(ws);
	    expfile.setFileName(fileName);
	    expfile.setDirectory(dir);
	    expfile.setContent(filePath);
	    fileId = expfile.save();

	    ScmFactory.File.asyncTransfer(wsUR, fileId, 1, 0);
	    Assert.fail("the user does not have priority to do something");
	} catch (ScmException e) {
	    if (e.getError() != ScmError.OPERATION_UNAUTHORIZED) {
		e.printStackTrace();
		Assert.fail(e.getMessage());
	    }
	} finally {
	    if (fileId != null) {
		ScmFactory.File.deleteInstance(wsA, fileId, true);
	    }
	    if (session != null) {
		session.close();
	    }
	}
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
	try {
	    ScmFactory.Role.revokePrivilege(sessionA, role, rs, ScmPrivilegeType.READ);
	    ScmFactory.Role.revokePrivilege(sessionA, role, rs, ScmPrivilegeType.UPDATE);
	    ScmFactory.Role.deleteRole(sessionA, role);
	    ScmFactory.User.deleteUser(sessionA, user);
	    deleteDir(wsA, path);
	    deleteDir(wsA, path1);
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

    private void cleanEnv() throws ScmException {
	BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
	ScmFileUtils.cleanFile(wsp, cond);
	deleteDir(wsA, path);
	deleteDir(wsA, path1);
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

	    createDir(wsA, path);
	    createDir(wsA, path1);
	    grantPriAndAttachRole(sessionA, rs, user, role, ScmPrivilegeType.READ);
	    grantPriAndAttachRole(sessionA, rs, user, role, ScmPrivilegeType.CREATE);
	    grantPriAndAttachRole(sessionA, rs, user, role, ScmPrivilegeType.DELETE);
		ScmAuthUtils.checkPriority(rootsite, username, passwd, role, wsp);
	    sessionUR = TestScmTools.createSession(branchsite, username, passwd);
	    wsUR = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionUR);
	} catch (ScmException e) {
	    e.printStackTrace();
	    Assert.fail(e.getMessage());
	}
    }
}
