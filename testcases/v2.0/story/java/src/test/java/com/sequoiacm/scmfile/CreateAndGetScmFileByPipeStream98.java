package com.sequoiacm.scmfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
 * @Testcase: SCM-98:管道方式写文件
 * @author huangxiaoni init
 * @date 2017.4.11
 */

public class CreateAndGetScmFileByPipeStream98 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private String fileName = "scmfile98";
	private ScmId fileId = null;
	private int fileSize = 1024;
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
	private void test() throws IOException {
		testCreateScmFileByByte();
		testGetScmFileByFile();
		runSuccess = true;
	}

	private void testCreateScmFileByByte() throws IOException {
		InputStream ism = null;
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);

			// get pipeInputStream and setContent
			ism = new FileInputStream(filePath);

			file.setContent(ism);
			file.setFileName(fileName);
			file.setAuthor("test");
			file.setTitle("sequoiacm");
			file.setMimeType("text/plain");
			fileId = file.save();

			// check results
			checkFileAttributes(file);
		} catch (ScmException | IOException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (ism != null)
				ism.close();
		}
	}

	private void testGetScmFileByFile() {
		try {
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			OutputStream fileOutputStream = new FileOutputStream(new File(downloadPath));
			file.getContent(fileOutputStream);
			fileOutputStream.close();

			// check results
			Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
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
			Assert.assertEquals(file.getAuthor(), "test");
			Assert.assertEquals(file.getTitle(), "sequoiacm");
			Assert.assertEquals(file.getMimeType(), "text/plain");
			Assert.assertEquals(file.getSize(), fileSize);

			Assert.assertEquals(file.getMinorVersion(), 0);
			Assert.assertEquals(file.getMajorVersion(), 1);

			Assert.assertEquals(file.getUser(), TestScmBase.scmUserName);
			Assert.assertNotNull(file.getCreateTime().getTime());

			Assert.assertEquals(file.getUpdateUser(), TestScmBase.scmUserName);
			Assert.assertNotNull(file.getUpdateTime().getTime());
		} catch (BaseException e) {
			throw e;
		}
	}

}