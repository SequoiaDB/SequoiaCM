package com.sequoiacm.scmfile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Testcase: SCM-179:setTitle参数校验
 * @author huangxiaoni init
 * @date 2017.4.12
 */

public class ScmFile_param_setTitle179 extends TestScmBase {
	private boolean runSuccess1 = false;
	private boolean runSuccess2 = false;
	private boolean runSuccess3 = false;

	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private String fileName = "scmfile179";
	private List<ScmId> fileIdList = new ArrayList<>();

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

	@Test(groups = { "oneSite", "twoSite", "fourSite" }) // normal test
	private void testTitleIsLong() {
		try {
			int strLeagth = 2048;
			String str = TestTools.getRandomString(strLeagth);

			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName+"_"+UUID.randomUUID());
			file.setTitle(str);
			ScmId fileId = file.save();
			fileIdList.add(fileId);

			// check results
			ScmFile file2 = ScmFactory.File.getInstance(ws, fileId);
			Assert.assertEquals(file2.getTitle(), str);
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
		runSuccess1 = true;
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testTitleIsEmptyStr() {
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName+"_"+UUID.randomUUID());
			file.setTitle("");
			ScmId fileId = file.save();
			fileIdList.add(fileId);

			// check results
			ScmFile file2 = ScmFactory.File.getInstance(ws, fileId);
			Assert.assertEquals(file2.getTitle(), "");
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
		runSuccess2 = true;
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testTitleIsNull() {
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName+"_"+UUID.randomUUID());
			file.setTitle(null);
			ScmId fileId = file.save();
			fileIdList.add(fileId);

			// check results
			ScmFile file2 = ScmFactory.File.getInstance(ws, fileId);
			Assert.assertEquals(file2.getTitle(), "");
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
		runSuccess3 = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if ((runSuccess1 && runSuccess2 && runSuccess3) || forceClear) {
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(ws, fileId, true);
				}
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}

}