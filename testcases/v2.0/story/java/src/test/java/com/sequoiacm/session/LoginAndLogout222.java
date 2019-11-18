package com.sequoiacm.session;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Testcase: SCM-222:不停登入登出
 * @author huangxiaoni init
 * @date 2017.4.18
 */

public class LoginAndLogout222 extends TestScmBase {
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testLoginAndLogout() {
		try {
			for (int i = 0; i < 300; i++) {
				ScmSession session = TestScmTools.createSession(site);
				// check result
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
				Assert.assertNotNull(ws);

				session.close();

			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testLogoutThenOper() {
		try {
			ScmSession session = TestScmTools.createSession(site);
			// [bug] sequoiacm-41 重复登出不应报错
			session.close();
			session.close();
			// [bug] sequoiacm-41 登出后操作失败报错合理
			ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			Assert.fail("getWorkspace succeed when session is closed!");
		} catch (ScmException e) {
			if (e.getErrorCode() != ScmError.SESSION_CLOSED.getErrorCode() && !e.getMessage().toString().contains("Session has been Closed")) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
	}

}