
package com.sequoiacm.auth;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-1743 :: 重复对角色授权
 * @author fanyu
 * @Date:2018年6月8日
 * @version:1.0
 */
public class AuthWs_ReapteGrantPriv1743 extends TestScmBase {
    private SiteWrapper site;
    private ScmSession sessionA;
    private ScmWorkspace wsA;
    private String[] usernameArr = { "AuthWs_ReapteGrantPriv1743_0", "AuthWs_ReapteGrantPriv1743_1" };
    private String[] rolenameArr = { "1743_0", "1743_1" };
    private String passwd = "1740";
    private List<ScmUser> userList = new ArrayList<ScmUser>();
    private List<ScmRole> roleList = new ArrayList<ScmRole>();
    private WsWrapper wsp;
    private int fileSize = 0;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws InterruptedException, IOException {
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
    private void test1() throws Exception {
	ScmSession session = null;
	String fileName = "AuthWs_ReapteGrantPriv1743_0";
	ScmId fileId = null;
	ScmResource rs = null;
	try {
	    rs = ScmResourceFactory.createWorkspaceResource(wsp.getName());
	    grantPriAndAttachRole(session, rs, userList.get(0), roleList.get(0), ScmPrivilegeType.ALL);
	    grantPriAndAttachRole(session, rs, userList.get(0), roleList.get(0), ScmPrivilegeType.ALL);
	    ScmAuthUtils.checkPriority(site, usernameArr[0], passwd, roleList.get(0), wsp);
	    // Thread.sleep(20000);
	    session = TestScmTools.createSession(site, usernameArr[0], passwd);
	    ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
	    fileId = ScmFileUtils.create(ws, fileName, filePath);
	} catch (ScmException e) {
	    e.printStackTrace();
	    Assert.fail(e.getMessage());
	} finally {
	    ScmFactory.Role.revokePrivilege(sessionA, roleList.get(0), rs, ScmPrivilegeType.ALL);
	    if (fileId != null) {
		ScmFactory.File.deleteInstance(wsA, fileId, true);
	    }
	    if (session != null) {
		session.close();
	    }
	}
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test2() throws Exception {
	ScmSession session = null;
	String fileName = "AuthWs_ReapteGrantPriv1743_1";
	ScmId fileId = null;
	ScmResource rs = null;
	try {
	    rs = ScmResourceFactory.createWorkspaceResource(wsp.getName());
	    grantPriAndAttachRole(session, rs, userList.get(1), roleList.get(1), ScmPrivilegeType.CREATE);
	    grantPriAndAttachRole(session, rs, userList.get(1), roleList.get(1), ScmPrivilegeType.READ);
	    // Thread.sleep(20000);
	    ScmAuthUtils.checkPriority(site, usernameArr[1], passwd, roleList.get(1), wsp);
	    session = TestScmTools.createSession(site, usernameArr[1], passwd);
	    ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
	    fileId = ScmFileUtils.create(ws, fileName, filePath);
	} catch (ScmException e) {
	    e.printStackTrace();
	    Assert.fail(e.getMessage());
	} finally {
	    ScmFactory.Role.revokePrivilege(sessionA, roleList.get(1), rs, ScmPrivilegeType.CREATE);
	    ScmFactory.Role.revokePrivilege(sessionA, roleList.get(1), rs, ScmPrivilegeType.READ);
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
	    for (int i = 0; i < usernameArr.length; i++)
		try {
		    ScmFactory.Role.deleteRole(sessionA, roleList.get(i));
		    ScmFactory.User.deleteUser(sessionA, userList.get(i));
		} catch (Exception e) {
		    e.printStackTrace();
		    Assert.fail(e.getMessage());
		}
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

    private void cleanEnv() {
	for (String rolename : rolenameArr) {
	    try {
		ScmFactory.Role.deleteRole(sessionA, rolename);
	    } catch (ScmException e) {
		if (e.getError() != ScmError.HTTP_NOT_FOUND) {
		    e.printStackTrace();
		    Assert.fail(e.getMessage());
		}
	    }
	}
	for (String username : usernameArr) {
	    try {
		ScmFactory.User.deleteUser(sessionA, username);
	    } catch (ScmException e) {
		if (e.getError() != ScmError.HTTP_NOT_FOUND) {
		    e.printStackTrace();
		    Assert.fail(e.getMessage());
		}
	    }
	}
    }

    private void prepare() {
	for (int i = 0; i < usernameArr.length; i++) {
	    try {
		ScmUser user = ScmFactory.User.createUser(sessionA, usernameArr[i], ScmUserPasswordType.LOCAL, passwd);
		ScmRole role = ScmFactory.Role.createRole(sessionA, rolenameArr[i], null);
		userList.add(user);
		roleList.add(role);
	    } catch (ScmException e) {
		e.printStackTrace();
		Assert.fail(e.getMessage());
	    }
	}
    }
}
