package com.sequoiacm.scmfile;

import java.io.File;

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
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-103:文件为空
 * @author huangxiaoni init
 * @date 2017.3.29
 */

public class CreateAndGetScmFileNotSetContent214 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private String fileName = "scmfile214";
	private ScmId fileId = null;
	private int fileSize = 0;
	private File localPath = null;
	private String filePath = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);

			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() {
		testCreateScmFileNotSetContent();
		testGetScmFile();
		runSuccess = true;
	}

	private void testCreateScmFileNotSetContent() {
		try {
			// create file
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName);
			file.setTitle("sequoiacm");
			file.setMimeType("text/plain");
			fileId = file.save();

			// check file's attribute
			checkFileAttributes(file);
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	private void testGetScmFile() {
		try {
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);

			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			file.getContent(downloadPath);

			// check content
			Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
			// check attribute
			checkFileAttributes(file);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
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

	private void checkFileAttributes(ScmFile file) {
		try {
			Assert.assertEquals(file.getWorkspaceName(), wsp.getName());
			Assert.assertEquals(file.getFileId(), fileId);

			Assert.assertEquals(file.getFileName(), fileName);
			Assert.assertEquals(file.getAuthor(), "");
			Assert.assertEquals(file.getTitle(), "sequoiacm");
			Assert.assertEquals(file.getMimeType(), "text/plain");
			Assert.assertEquals(file.getSize(), fileSize);

			Assert.assertEquals(file.getMinorVersion(), 0);
			Assert.assertEquals(file.getMajorVersion(), 1);

			Assert.assertEquals(file.getUser(), TestScmBase.scmUserName);
			Assert.assertNotNull(file.getCreateTime().getTime());
		} catch (BaseException e) {
			throw e;
		}
	}

}