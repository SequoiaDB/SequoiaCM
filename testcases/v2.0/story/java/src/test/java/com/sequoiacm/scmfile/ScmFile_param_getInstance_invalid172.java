package com.sequoiacm.scmfile;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Testcase: SCM-172:getInstance无效参数校验
 * @author huangxiaoni init
 * @date 2017.4.12
 */

public class ScmFile_param_getInstance_invalid172 extends TestScmBase {
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testFileIdIsNull() {
		try {
			ScmFile file = ScmFactory.File.getInstance(ws, (ScmId)null);
			file.getFileName();
			Assert.assertFalse(true, "expect result is fail but actual is success.");
		} catch (ScmException e) {
			if (e.getError() != ScmError.INVALID_ARGUMENT) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testFileIdNotExist() {
		try {
			ScmId fieldId = new ScmId("0xa1ffb2ffc3ffd4ff56ffe8ff");
			ScmFile file = ScmFactory.File.getInstance(ws, fieldId);
			file.getFileName();
			Assert.assertFalse(true, "expect result is fail but actual is success.");
		} catch (ScmException e) {
			e.printStackTrace();
			if (e.getError() != ScmError.INVALID_ID) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (session != null) {
				session.close();
			}
		} finally {

		}
	}

}