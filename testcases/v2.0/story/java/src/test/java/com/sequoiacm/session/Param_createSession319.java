package com.sequoiacm.session;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @FileName SCM-319: createSession无效参数校验
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、createSession接口无效参数校验：conf为null； 2、检查执行结果；
 */

public class Param_createSession319 extends TestScmBase {

	@BeforeClass(alwaysRun = true)
	private void setUp() {
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() {
		try {
			ScmFactory.Session.createSession(SessionType.AUTH_SESSION, null);
			Assert.fail("create session shouldn't succeed when option is null");
		} catch (ScmException e) {
			if (e.getErrorCode() != ScmError.INVALID_ARGUMENT.getErrorCode()) { // -108 SCM_COMMON_INVALIDARG
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
	}

}
