package com.sequoiacm.auth;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmPrivilege;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1739:删除角色
 * @Author huangxioni
 * @Date 2018/6/7
 */

public class AuthWs_role1739 extends TestScmBase {
    private static final Logger logger = Logger.getLogger(AuthWs_role1739.class);
    private boolean runSuccess = false;

    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private static final String NAME = "authws1739";
    private static final String PASSWORD = NAME;
    private static final String DIR_PATH = "/" + NAME;
    private ScmRole role = null;

    @BeforeClass
    private void setUp() throws ScmException {
	site = ScmInfo.getSite();
	wsp = ScmInfo.getWs();
	session = TestScmTools.createSession(site);
	ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
	// clean
	cleanEnv();
	// prepare user
	this.createUserAndRole();
	// prepare director
	ScmFactory.Directory.createInstance(ws, DIR_PATH);
    }

    @Test
    private void test() throws ScmException, InterruptedException {
	ScmPrivilegeType privilege = ScmPrivilegeType.ALL;
	// grantPrivilege
	ScmResource resource = ScmResourceFactory.createDirectoryResource(wsp.getName(), DIR_PATH);
	ScmFactory.Role.grantPrivilege(session, role, resource, privilege);

	ScmCursor<ScmPrivilege> priCursor = ScmFactory.Privilege.listPrivileges(session, role);
	String resourceId = null;
	while (priCursor.hasNext()) {
	    ScmPrivilege info = priCursor.getNext();
	    if (info.getRole().equals(role)) {
		resourceId = info.getResourceId();
		break;
	    }
	}

	// delete role
	ScmFactory.Role.deleteRole(session, NAME);

	// check results
	// check privileges
	priCursor = ScmFactory.Privilege.listPrivileges(session, role);
	while (priCursor.hasNext()) {
	    ScmPrivilege info = priCursor.getNext();
	    Assert.assertNotEquals(info.getRoleId(), role.getRoleId());
	    Assert.assertNotEquals(info.getResource(), resource);
	}
	// check resource
	try {
	    ScmFactory.Resource.getResourceById(session, resourceId);
	    Assert.fail("expect failed but actual succ.");
	} catch (ScmException e) {
	    logger.info("get not exist resource, errorMsg = [" + e.getError() + "]");
	}

	runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
	try {
	    if (runSuccess || TestScmBase.forceClear) {
		ScmFactory.User.deleteUser(session, NAME);
		try {
		    ScmFactory.Role.deleteRole(session, NAME);
		} catch (ScmException e) {
		    logger.info("delete not eixst role in tear down, errorMsg = " + e.getError());
		}
		ScmFactory.Directory.deleteInstance(ws, DIR_PATH);
	    }
	} finally {
	    if (null != session) {
		session.close();
	    }
	}
    }

    private void createUserAndRole() throws ScmException {
	ScmUser scmUser = ScmFactory.User.createUser(session, NAME, ScmUserPasswordType.LOCAL, PASSWORD);
	role = ScmFactory.Role.createRole(session, NAME, "");
	ScmUserModifier modifier = new ScmUserModifier();
	modifier.addRole(role);
	ScmFactory.User.alterUser(session, scmUser, modifier);
    }

    private void cleanEnv() {
	// clean users and roles
	try {
	    ScmFactory.User.deleteUser(session, NAME);
	} catch (ScmException e) {
	    logger.info("clean users in setUp, errorMsg = [" + e.getError() + "]");
	}
	try {
	    ScmFactory.Role.deleteRole(session, NAME);
	} catch (ScmException e) {
	    logger.info("clean roles in setUp, errorMsg = [" + e.getError() + "]");
	}

	// clean director
	try {
	    ScmFactory.Directory.deleteInstance(ws, DIR_PATH);
	} catch (ScmException e) {
	    logger.info("dir does not exist, errorMsg = [" + e.getError() + "]");
	}
    }
}
