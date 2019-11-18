
package com.sequoiacm.auth.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @Description:SCM-1543 :: 并发登录登出
 * @author fanyu
 * @Date:2018年5月18日
 * @version:1.0
 */
public class AuthServer_LoginAndLogOut1543 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper site;
	private ScmSession session;
	private String username1 = "LoginAndLogOut1543_1";
	private String passwd1 = "1543_1";
	private String username2 = "LoginAndLogOut1543_2";
	private String passwd2 = "1543_2";
	private ScmUser user1;
	private ScmUser user2;
	private List<ScmRole> roleList = new CopyOnWriteArrayList<ScmRole>();

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			session = TestScmTools.createSession(site);
			site = ScmInfo.getSite();
			try {
				ScmFactory.User.deleteUser(session, username1);
			} catch (ScmException e) {
				if (e.getError() != ScmError.HTTP_NOT_FOUND) {
					Assert.fail(e.getMessage());
				}
			}
			try {
				ScmFactory.User.deleteUser(session, username2);
			} catch (ScmException e) {
				if (e.getError() != ScmError.HTTP_NOT_FOUND) {
					Assert.fail(e.getMessage());
				}
			}
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		try {
			ScmUserModifier modifier = new ScmUserModifier();
			ScmUser superUser = ScmFactory.User.getUser(session, TestScmBase.scmUserName);
			Collection<ScmRole> superRoles = superUser.getRoles();
			modifier.addRoles(superRoles);
			user1 = ScmFactory.User.createUser(session, username1, ScmUserPasswordType.LOCAL, passwd1);
			ScmFactory.User.alterUser(session, user1, modifier);
			user2 = ScmFactory.User.createUser(session, username2, ScmUserPasswordType.LOCAL, passwd2);
			ScmFactory.User.alterUser(session, user2, modifier);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() {
		LoginAndDoSomething inThread = new LoginAndDoSomething();
		LogoutAndDoSomething outThread = new LogoutAndDoSomething();
		inThread.start(20);
		outThread.start(20);
		boolean inFlag = inThread.isSuccess();
		boolean outFlag = outThread.isSuccess();
		Assert.assertEquals(inFlag, true, inThread.getErrorMsg());
		Assert.assertEquals(outFlag, true, outThread.getErrorMsg());
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.User.deleteUser(session, user1);
				ScmFactory.User.deleteUser(session, user2);
				for(ScmRole role : roleList){
					ScmFactory.Role.deleteRole(session, role);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	private class LoginAndDoSomething extends TestThreadBase {
		@Override
		public void exec() {
			try {
				ScmSession session = TestScmTools.createSession(site, username1, passwd1);
				String roleName = "LoginAndDoSomething" + UUID.randomUUID();
				ScmRole expRole = ScmFactory.Role.createRole(session, roleName, null);
				roleList.add(expRole);
				check(session, expRole);
			} catch (ScmException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}

		private void check(ScmSession session, ScmRole expRole) {
			ScmRole actRole = null;
			try {
				actRole = ScmFactory.Role.getRole(session, expRole.getRoleName());
				// Assert.assertEquals(actRole.getDescription(),
				// expRole.getDescription(), actRole.toString());
				Assert.assertEquals(actRole.getRoleId(), expRole.getRoleId(), actRole.toString());
				Assert.assertEquals(actRole.getRoleName(), expRole.getRoleName(), actRole.toString());
			} catch (ScmException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage() + ",expRole = " + expRole.toString() + ",actRole = " + actRole);
			}
		}
	}

	private class LogoutAndDoSomething extends TestThreadBase {
		@Override
		public void exec() {
			try {
				ScmSession session = TestScmTools.createSession(site, username2, passwd2);
				session.close();
				String roleName = "LogoutAndDoSomething";
				ScmFactory.Role.createRole(session, roleName, null);
			} catch (ScmException e) {
				if (e.getError() != ScmError.SESSION_CLOSED) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}
		}
	}
}