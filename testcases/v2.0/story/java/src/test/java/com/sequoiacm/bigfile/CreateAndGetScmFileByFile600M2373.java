package com.sequoiacm.bigfile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @Description:通过文件创建和获取scm文件，文件大小600M
 * @author fanyu
 * @Date:2019年02月14日
 * @version:1.0
 */
public class CreateAndGetScmFileByFile600M2373 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private String name = "file600M";
	private ScmId fileId = null;
	private long fileSize = 1024*1024*600;
	private File localPath = null;
	private String filePath = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws IOException, ScmException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);
		site = ScmInfo.getRootSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
		ScmFileUtils.cleanFile(wsp,cond);
	}

	@Test(groups = { "fourSite" })
	private void test() throws Exception {
		testCreateScmFileByFile();
		testGetScmFileByFile();
		runSuccess = true;
	}

	private void testCreateScmFileByFile() throws ScmException {
		// create file
		ScmFile file = ScmFactory.File.createInstance(ws);
		System.out.println("size = " + new File(filePath).length());
		file.setContent(filePath);
		file.setFileName(name + "_" + UUID.randomUUID());
		file.setTitle("sequoiacm");
		file.setAuthor(name);
		fileId = file.save();
		System.out.println("fileId = " + fileId.get());
		// check file's attribute
		checkFileAttributes(file);
	}

	private void testGetScmFileByFile() throws Exception {
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
				Thread.currentThread().getId());
		file.getContent(downloadPath);
		// check content
		Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
		// check attribute
		checkFileAttributes(file);
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	private void checkFileAttributes(ScmFile file) {
		Assert.assertEquals(file.getWorkspaceName(), wsp.getName());
		Assert.assertEquals(file.getFileId(), fileId);
		Assert.assertEquals(file.getAuthor(), name);
		Assert.assertEquals(file.getTitle(), "sequoiacm");
		Assert.assertEquals(file.getMimeType(), "text/plain");
		Assert.assertEquals(file.getSize(), fileSize);
		Assert.assertEquals(file.getMinorVersion(), 0);
		Assert.assertEquals(file.getMajorVersion(), 1);
		Assert.assertEquals(file.getUser(), TestScmBase.scmUserName);
		Assert.assertNotNull(file.getCreateTime().getTime());
	}
}