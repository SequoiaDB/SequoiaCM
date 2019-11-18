package com.sequoiacm.scmfile;

import java.io.File;
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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Testcase: SCM-173:setFileName参数校验
 * @author huangxiaoni init
 * @date 2017.4.12
 */

public class ScmFile_param_getContent182 extends TestScmBase {
	private boolean runSuccess1 = false;
	private boolean runSuccess2 = false;

	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private String fileName = "scmfile182";
	private ScmId fileId = null;
	private File localPath = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			createScmFile();
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" }) // normal test
	private void testPathNotExist() {
		try {
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			file.getContent(localPath + File.separator + "a" + File.separator + "a.txt");
			Assert.assertFalse(true, "expect result is fail but actual is success.");
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.assertEquals(e.getError(), ScmError.FILE_IO, e.getMessage());
		}
		runSuccess1 = true;
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testFileNotExist() {
		try {
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			file.getContent(localPath + File.separator + "a.txt");
			Assert.assertFalse(true, "expect result is fail but actual is success.");
		} catch (ScmException e) {
			Assert.assertEquals(e.getError(), ScmError.FILE_IO, e.getMessage());
		}
		runSuccess2 = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if ((runSuccess1 && runSuccess2) || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}

	private void createScmFile() {
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName+"_"+UUID.randomUUID());
			file.setTitle("sequoiacm");
			file.setMimeType("text/plain");
			fileId = file.save();
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

}