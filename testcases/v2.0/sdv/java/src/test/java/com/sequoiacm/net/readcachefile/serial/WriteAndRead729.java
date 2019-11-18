package com.sequoiacm.net.readcachefile.serial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-729 :并发读写（大并发） 1、在主中心写入文件；
 *                      2、并发在分中心读取主中心文件、写入新的文件，并发数如500个并发； 3、检查读写执行结果正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class WriteAndRead729 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper rootSite = null;
	private SiteWrapper branSite = null;
	private WsWrapper wsp = null;

	private int fileSize = 1024 * 1;
	private File localPath = null;
	private String filePath = null;
	private List<String> filePathList = new ArrayList<String>();
	private List<ScmId> fileIdList = new CopyOnWriteArrayList<ScmId>();
	private static final String author = "WriteAndRead729";
	private int fileNum = 10;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		filePathList.add(filePath);
		try {
			rootSite = ScmInfo.getRootSite();
			branSite = ScmInfo.getBranchSite();
			wsp = ScmInfo.getWs();

			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);

			write(rootSite, wsp);
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}
     
	//Bug:237
	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws ScmException {
		ReadFile rThread = new ReadFile();
		rThread.start(50);
		WriteFile wThread = new WriteFile();
		wThread.start(50);
		boolean rflag = rThread.isSuccess();
		boolean wflag = rThread.isSuccess();
		Assert.assertEquals(rflag, true, rThread.getErrorMsg());
		Assert.assertEquals(wflag, true, wThread.getErrorMsg());
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
			if (runSuccess || forceClear) {
				BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
				ScmFileUtils.cleanFile(wsp, cond);
				TestTools.LocalFile.removeFile(localPath);
			}
	}

	private class ReadFile extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(branSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
						ScmId fileId = fileIdList.get((int)Math.random()%10);
						ScmFile file = ScmFactory.File.getInstance(ws, fileId);
						String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
								Thread.currentThread().getId());
						file.getContent(downloadPath);
						checkResult(file, downloadPath);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (null != session) {
					session.close();
				}
			}
		}

		private void checkResult(ScmFile file, String downloadPath) {
			try {
				Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
				Assert.assertEquals(file.getWorkspaceName(), wsp.getName());
				Assert.assertEquals(file.getAuthor(), author);
				Assert.assertEquals(file.getSize(), fileSize);
				Assert.assertEquals(file.getMinorVersion(), 0);
				Assert.assertEquals(file.getMajorVersion(), 1);
				Assert.assertEquals(file.getUser(), TestScmBase.scmUserName);
				Assert.assertNotNull(file.getCreateTime().getTime());
			} catch (IOException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	private class WriteFile extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(branSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
				// write
				for (int i = 0; i < fileNum; i++) {
					ScmFile file = ScmFactory.File.createInstance(ws);
					file.setContent(filePath);
					file.setFileName(author+UUID.randomUUID());
					file.setAuthor(author);
					ScmId fileId = file.save();
					checkResult(fileId);
				}
			} catch (ScmException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}

		private void checkResult(ScmId fileId) {
			try {
				// check meta data
				SiteWrapper[] expSites = { branSite };
				// check lobs
				ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	private void write(SiteWrapper site, WsWrapper wsp) {
		ScmSession session = null;
		try {
			session = TestScmTools.createSession(site);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			// write
			for (int i = 0; i < fileNum; i++) {
				ScmFile file = ScmFactory.File.createInstance(ws);
				file.setContent(filePath);
				file.setFileName(author+"_"+UUID.randomUUID()+i);
				file.setAuthor(author);
				ScmId fileId = file.save();
				fileIdList.add(fileId);
			}
		} catch (ScmException e) {
		    e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (null != session) {
				session.close();
			}
		}
	}
}