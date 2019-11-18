package com.sequoiacm.session;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @FileName SCM-321: addUrl无效参数校验
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、addUrl接口无效参数校验,覆盖： a.主机名ping不同； b.端口错误（非scm端口/不存在）； c.host：null、空串；
 * 2、检查执行结果；
 */

public class Param_addUrl321 extends TestScmBase {
	private static SiteWrapper site = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		site = ScmInfo.getSite();
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testHostNotExist() {
		try {
			ScmConfigOption scOpt = new ScmConfigOption();
			scOpt.addUrl("thishostisunexist"+"/"+site.getSiteServiceName());
			scOpt.setUser(TestScmBase.scmUserName);
			scOpt.setPasswd(TestScmBase.scmPassword);
			ScmFactory.Session.createSession(SessionType.AUTH_SESSION, scOpt);
			Assert.fail("host not exist, expect fail, but success.");
		} catch (ScmException e) {
			if (e.getErrorCode() != ScmError.NETWORK_IO.getErrorCode()) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
	}

}
