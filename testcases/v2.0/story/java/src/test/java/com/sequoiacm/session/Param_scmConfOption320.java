package com.sequoiacm.session;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @FileName SCM-320: ScmConfigOption无效参数校验
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、ScmConfigOption接口无效参数校验,覆盖： a.主机名ping不同； b.端口错误（非scm端口/不存在）； c.host：null；
 * 2、检查执行结果；
 */

public class Param_scmConfOption320 extends TestScmBase {
	private static SiteWrapper site = null;
	private static NodeWrapper node = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		site = ScmInfo.getSite();
		node = site.getNode();
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testHostNotExist() {
		try {
			ScmConfigOption scOpt = new ScmConfigOption("thishostisunexist"+":"+node.getPort());
			scOpt.setUser(TestScmBase.scmUserName);
			scOpt.setPasswd(TestScmBase.scmPassword);
			ScmFactory.Session.createSession(SessionType.AUTH_SESSION, scOpt);
			Assert.fail("host not exist, expect fail, but success. urls=" + scOpt.getUrls());
		} catch (ScmException e) {
			if (e.getErrorCode() != ScmError.NETWORK_IO.getErrorCode()) { // SCM_NETWORK_CONNECT_FAILED
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" },enabled=false)
	private void testPortNotExist() {
		try {
			int port = node.getPort()+1234;
			ScmConfigOption scOpt = new ScmConfigOption(node.getHost()+":"+port);
			scOpt.setUser(TestScmBase.scmUserName);
			scOpt.setPasswd(TestScmBase.scmPassword);
			ScmFactory.Session.createSession(SessionType.AUTH_SESSION, scOpt);
			Assert.fail("port not exist, expect fail, but success. urls=" + scOpt.getUrls());
		} catch (ScmException e) {
			if (e.getErrorCode() != ScmError.NETWORK_IO.getErrorCode()) { // SCM_NETWORK_CONNECT_FAILED
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	// host is empty string, default is localHost
	// if localHost is control host, expect fail; if localHost is scmHost,
	// expect success
	// wait TestScmBase modify to list
	@Test(groups = { "oneSite", "twoSite", "fourSite" },enabled=false)
	private void testHostIsEmptyStr() {
		ScmConfigOption scOpt = null;
		ScmSession session = null;
		try {
			scOpt = new ScmConfigOption(""+":"+ node.getPort());
			scOpt.setUser(TestScmBase.scmUserName);
			scOpt.setPasswd(TestScmBase.scmPassword);
			session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, scOpt);
			String ssId = session.getSessionId();
			Assert.assertNotNull(ssId, "host is empty string, default is localhost, urls=" + scOpt.getUrls());
		} catch (ScmException e) {
			if (e.getErrorCode() != ScmError.NETWORK_IO.getErrorCode()) { // SCM_NETWORK_CONNECT_FAILED
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
	}

}
