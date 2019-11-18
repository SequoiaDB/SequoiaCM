package com.sequoiacm.auth;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

/**
 * @FileName  SCM-1573:修改用户，添加admin角色和普通角色，并删除角色和用户
 * 			  SCM-1575:修改用户，删除当前admin用户的AUTH_ADMIN角色和普通角色
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1573_1575 extends TestScmBase {
	private static final Logger logger = Logger.getLogger(AuthServer_user1573_1575.class);
	private boolean runSuccess = false;
	private int failTimes = 0;
	
	private SiteWrapper site = null;
	private ScmSession session = null;
	
	private static final String NAME = "auth1573";
	private static final String PASSWORD = NAME;
	private ScmRole authAdminRole = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException {
		site = ScmInfo.getSite();
		session = TestScmTools.createSession(site);
		
		// clean new user
		try {
			ScmFactory.User.deleteUser(session, NAME);
		}  catch (ScmException e) {
			logger.info("clean users in setUp, errorMsg = [" + e.getError() + "]");
		}
		try {
			ScmFactory.Role.deleteRole(session, NAME);
		}  catch (ScmException e) {
			logger.info("clean roles in setUp, errorMsg = [" + e.getError() + "]");
		}
		
		// get AUTH_ADMIN role
		ScmUser adminUser = ScmFactory.User.getUser(session, TestScmBase.scmUserName);
		authAdminRole = adminUser.getRoles().iterator().next();

		ScmFactory.User.createUser(session, NAME, ScmUserPasswordType.LOCAL, PASSWORD);
		ScmFactory.Role.createRole(session, NAME, "");
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws ScmException {
		test_addRoles();
		test_delCurrentAdminRole();
		test_delRoles();
		runSuccess = true;
	}

	private void test_addRoles() throws ScmException {
		// alter user, add roles
		ScmUser scmUser = ScmFactory.User.getUser(session, NAME);
		ScmUserModifier modifier = new ScmUserModifier();
		modifier.addRole(authAdminRole);
		modifier.addRole(NAME);
		ScmFactory.User.alterUser(session, scmUser, modifier);
		
		// check results
		scmUser = ScmFactory.User.getUser(session, NAME);
		Collection<ScmRole> roles = scmUser.getRoles();
		Assert.assertEquals(roles.size(), 2);
		
		Iterator<ScmRole> itRoles = roles.iterator();
		List<String> usernames = new ArrayList<>();
		while (itRoles.hasNext()) {
			ScmRole role = itRoles.next();
			usernames.add(role.getRoleName());
		}
		Collections.sort(usernames);
		Assert.assertEquals(usernames.get(0), authAdminRole.getRoleName());
		Assert.assertEquals(usernames.get(1), "ROLE_" + NAME);
	}

	private void test_delCurrentAdminRole() throws ScmException {	
		ScmSession ss = TestScmTools.createSession(site, NAME, PASSWORD);
		// alter user, delete current AUTH_ADMIN role		
		ScmUser scmUser = ScmFactory.User.getUser(ss, NAME);
		ScmUserModifier modifier = new ScmUserModifier();
		modifier.delRole(authAdminRole);
		try {
			ScmFactory.User.alterUser(ss, scmUser, modifier);
			Assert.fail("expect failed but actual succ.");
		} catch (ScmException e) {
			logger.info("delete current AUTH_ADMIN role, errorMsg = [" + e.getError() + "]");
		}
		
		// check results
		scmUser = ScmFactory.User.getUser(ss, NAME);
		Collection<ScmRole> roles = scmUser.getRoles();
		Assert.assertEquals(roles.size(), 2);
	}

	private void test_delRoles() throws ScmException {		
		// alter user, delete roles
		ScmUser scmUser = ScmFactory.User.getUser(session, NAME);
		ScmRole role = ScmFactory.Role.getRole(session, NAME);
		ScmUserModifier modifier = new ScmUserModifier();
		modifier.delRole(authAdminRole);
		modifier.delRole(role);
		ScmFactory.User.alterUser(session, scmUser, modifier);
		
		// check results
		scmUser = ScmFactory.User.getUser(session, NAME);
		Collection<ScmRole> roles = scmUser.getRoles();
		Assert.assertEquals(roles.size(), 0);
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.User.deleteUser(session, NAME);
				ScmFactory.Role.deleteRole(session, NAME);
			}
		}finally{
			if(null != session){
				session.close();
			}
		}
	}
}
