package com.sequoiacm.auth;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @FileName SCM-1490:普通角色创建用户
 * @Author huangxioni
 * @Date 2018/5/16
 */

public class AuthServer_user1490 extends TestScmBase {
	private static final Logger logger = Logger.getLogger(AuthServer_user1490.class);
	
	private boolean runSuccess = false;
	private SiteWrapper site = null;
	private ScmSession session = null;
	
	private static final String NAME = "auth1490";
	private static final String PASSWORD = NAME;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			session = TestScmTools.createSession(site);
			
			// clean new user and role
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
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws ScmException {
		this.createUserAndRole();
		
		// login ordinary user
		ScmSession ss = TestScmTools.createSession(site, NAME, PASSWORD);
		
		// create user by ordinary user
		try {
			ScmFactory.User.createUser(session, NAME, ScmUserPasswordType.LOCAL, PASSWORD);
			Assert.fail("expect fail but actual succ.");
		} catch (ScmException e) {
			logger.info("create user by ordinary user failed, errorMsg = " + e.getError());
		}
		
		ss.close();
		
		runSuccess = true;
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
	
	private void createUserAndRole() throws ScmException {
		ScmUser scmUser = ScmFactory.User.createUser(session, NAME, ScmUserPasswordType.LOCAL, PASSWORD);
		
		ScmFactory.Role.createRole(session, NAME, "");
//		System.out.println(role.getRoleName());
		
		ScmUserModifier modifier = new ScmUserModifier();
		modifier.addRole(NAME);
		ScmFactory.User.alterUser(session, scmUser, modifier);
	}

}
