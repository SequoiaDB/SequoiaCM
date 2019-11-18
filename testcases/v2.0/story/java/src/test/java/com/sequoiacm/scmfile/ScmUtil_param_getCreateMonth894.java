package com.sequoiacm.scmfile;

import java.text.SimpleDateFormat;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmUtil;
import com.sequoiacm.client.core.ScmAttributeName;
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
 * @Testcase: SCM-894:getCreateMonth参数校验
 * @author huangxiaoni init
 * @date 2017.8.23
 */

public class ScmUtil_param_getCreateMonth894 extends TestScmBase {
	private boolean runSuccess1 = false;
	private boolean runSuccess2 = false;
	private boolean runSuccess3 = false;

	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;
	private ScmId fileId = null;
	private String fileName = "ScmUtil_param_getCreateMonth894";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName);
			file.setAuthor(fileName);
			fileId = file.save();
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testNomal() {
		try {
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			SimpleDateFormat df = new SimpleDateFormat("yyyyMM");
			Assert.assertEquals(ScmUtil.Id.getCreateMonth(fileId), df.format(file.getCreateTime()));
			Assert.assertEquals(ScmAttributeName.File.CREATE_MONTH, "create_month");

			Assert.assertEquals(ScmUtil.Id.getCreateMonth(new ScmId("0000000000000000012c1947")), "197001");
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
		runSuccess1 = true;
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testFileIdNotExist() {
		try {
			ScmUtil.Id.getCreateMonth(new ScmId("test"));
			Assert.assertFalse(true, "expect result is fail but actual is success.");
		} catch (ScmException e) {
			Assert.assertEquals(e.getError(),ScmError.INVALID_ID, e.getMessage());
		}
		runSuccess2 = true;
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testFileIdIsNull() {
		try {
			ScmUtil.Id.getCreateMonth(null);
			Assert.assertFalse(true, "expect result is fail but actual is success.");
		} catch (ScmException e) {
			Assert.assertEquals(e.getError(), ScmError.INVALID_ARGUMENT, e.getMessage());
		}
		runSuccess3 = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if ((runSuccess1 && runSuccess2 && runSuccess3) || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (null != session) {
				session.close();
			}
		}
	}

}