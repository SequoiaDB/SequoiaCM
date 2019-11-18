
package com.sequoiacm.session;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSessionMgr;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @Description:SCM-2249 :: createSessionMgr接口测试
 * @author fanyu
 * @Date:2018年9月21日
 * @version:1.0
 */
public class SessionMgr2249 extends TestScmBase {
	private SiteWrapper site = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		site = ScmInfo.getSite();
	}

	@Test
	private void testDiffSite() throws Exception {
		List<String> urlList = new ArrayList<String>();
		List<SiteWrapper> sites = ScmInfo.getAllSites();
		for (String gateway : gateWayList) {
			for (SiteWrapper site : sites) {
				urlList.add(gateway + "/" + site.getSiteServiceName());
			}
		}
		ScmConfigOption scOpt;
		ScmSessionMgr sessionMgr = null;
		try {
			scOpt = new ScmConfigOption(urlList, TestScmBase.scmUserName, TestScmBase.scmPassword);
			sessionMgr = ScmFactory.Session.createSessionMgr(scOpt, 1000);
		} catch (ScmException e) {
			if (e.getError() != ScmError.INVALID_ARGUMENT) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		} finally {
			if (sessionMgr != null) {
				sessionMgr.close();
			}
		}
	}

	@Test
	private void testUrlIsEmpty() throws Exception {
		List<String> urlList = new ArrayList<String>();
		ScmConfigOption scOpt;
		ScmSessionMgr sessionMgr = null;
		try {
			scOpt = new ScmConfigOption(urlList, TestScmBase.scmUserName, TestScmBase.scmPassword);
			sessionMgr = ScmFactory.Session.createSessionMgr(scOpt, 1000);
			sessionMgr.getSession(SessionType.AUTH_SESSION);
			Assert.fail("exp fail but act success");
		} catch (ScmException e) {
			if (e.getError() != ScmError.INVALID_ARGUMENT) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		} finally {
			if (sessionMgr != null) {
				sessionMgr.close();
			}
		}
	}

	@Test
	private void testUrlIsNull() throws Exception {
		List<String> urlList = null;
		ScmConfigOption scOpt;
		ScmSessionMgr sessionMgr = null;
		try {
			scOpt = new ScmConfigOption(urlList, TestScmBase.scmUserName, TestScmBase.scmPassword);
			sessionMgr = ScmFactory.Session.createSessionMgr(scOpt, 1000);
			sessionMgr.getSession(SessionType.AUTH_SESSION);
			Assert.fail("exp fail but act success");
		} catch (ScmException e) {
			if (e.getError() != ScmError.INVALID_ARGUMENT) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		} finally {
			if (sessionMgr != null) {
				sessionMgr.close();
			}
		}
	}

	@Test
	private void testUrlIsWrong() throws Exception {
		List<String> urlList = new ArrayList<String>();
		for (String gateway : gateWayList) {
			urlList.add(gateway + "/" + site.getSiteServiceName() + "_1");
		}
		ScmConfigOption scOpt;
		ScmSessionMgr sessionMgr = null;
		ScmSession session = null;
		try {
			scOpt = new ScmConfigOption(urlList, TestScmBase.scmUserName, TestScmBase.scmPassword);
			sessionMgr = ScmFactory.Session.createSessionMgr(scOpt, 1000);
			session = sessionMgr.getSession(SessionType.AUTH_SESSION);
			ScmFactory.Site.listSite(session);
			Assert.fail("exp fail but act success");
		} catch (ScmException e) {
			if (e.getError() != ScmError.HTTP_NOT_FOUND) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		} finally {
			if(session != null){
				session.close();
			}
			if (sessionMgr != null) {
				sessionMgr.close();
			}
		}
	}

	@Test
	private void testUserIsWrong() throws Exception {
		List<String> urlList = new ArrayList<String>();
		for (String gateway : gateWayList) {
			urlList.add(gateway + "/" + site.getSiteServiceName());
		}
		ScmConfigOption scOpt;
		ScmSessionMgr sessionMgr = null;
		try {
			scOpt = new ScmConfigOption(urlList, TestScmBase.scmUserName + "_2249", TestScmBase.scmPassword);
			sessionMgr = ScmFactory.Session.createSessionMgr(scOpt, 1000);
			sessionMgr.getSession(SessionType.AUTH_SESSION);
			Assert.fail("exp fail but act success");
		} catch (ScmException e) {
			if (e.getError() != ScmError.HTTP_UNAUTHORIZED) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		} finally {
			if (sessionMgr != null) {
				sessionMgr.close();
			}
		}
	}

	@Test
	private void testPasswdIsWrong() throws Exception {
		List<String> urlList = new ArrayList<String>();
		for (String gateway : gateWayList) {
			urlList.add(gateway + "/" + site.getSiteServiceName());
		}
		ScmConfigOption scOpt;
		ScmSessionMgr sessionMgr = null;
		try {
			scOpt = new ScmConfigOption(urlList, TestScmBase.scmUserName, TestScmBase.scmPassword + "_2249");
			sessionMgr = ScmFactory.Session.createSessionMgr(scOpt, 1000);
			sessionMgr.getSession(SessionType.AUTH_SESSION);
			Assert.fail("exp fail but act success");
		} catch (ScmException e) {
			if (e.getError() != ScmError.HTTP_UNAUTHORIZED) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		} finally {
			if (sessionMgr != null) {
				sessionMgr.close();
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {

	}
}
