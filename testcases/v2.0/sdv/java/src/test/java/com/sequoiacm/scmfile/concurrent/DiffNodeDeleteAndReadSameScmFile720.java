package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-720:本地中心不同节点并发读取、删除相同文件 1、同一个中心的不同节点并发读取、删除相同文件；
 *                                          2、检查执行结果正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class DiffNodeDeleteAndReadSameScmFile720 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper site = null;
	private WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private int fileSize = 1024 * 3;
	private File localPath = null;
	private String filePath = null;
	private ScmId fileId = null;
	private static final String author = "DiffNodeDeleteAndReadSameScmFile720";

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

			fileId = ScmFileUtils.create(ws, author+"_"+UUID.randomUUID(), filePath);
		} catch (IOException | ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		DeleteScmFile dThread = new DeleteScmFile(fileId);
		dThread.start();

		ReadScmFile rThread = new ReadScmFile(fileId);
		rThread.start(20);

		if (!(dThread.isSuccess() || rThread.isSuccess())) {
			Assert.fail(dThread.getErrorMsg() + rThread.getErrorMsg());
		}
		dThread.join();
		rThread.join();
		checkResults();
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || forceClear) {
				try{
				ScmFactory.File.deleteInstance(ws, fileId, true);
				}catch(ScmException e){
					System.out.println("MSG = " + e.getMessage());
				}
				TestTools.LocalFile.removeFile(localPath);
			}
		}finally {
			if(session != null){
				session.close();
			}
		}
	}

	private class DeleteScmFile extends TestThreadBase {
		private ScmId fileId = null;

		public DeleteScmFile(ScmId fileId) {
			this.fileId = fileId;
		}

		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(site);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
				ScmFactory.File.getInstance(ws, this.fileId).delete(true);
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}

	private class ReadScmFile extends TestThreadBase {
		private ScmId fileId = null;

		public ReadScmFile(ScmId fileId) {
			this.fileId = fileId;
		}

		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(site);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
				ScmFile file = ScmFactory.File.getInstance(ws, fileId);
				String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());
				file.getContent(downloadPath);
				// check results
				Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
			} catch (ScmException e) {
				if (e.getError() != ScmError.FILE_NOT_FOUND && e.getError() != ScmError.DATA_ERROR) {
					Assert.fail(e.getMessage());
				}
			} finally {
				if (null != session) {
					session.close();
				}
			}
		}
	}

	private void checkResults() throws Exception {
		try {
			// check meta
			BSONObject cond = new BasicBSONObject("id", fileId.get());
			long cnt = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
			Assert.assertEquals(cnt, 0);

			// check data
			ScmFileUtils.checkData(ws, fileId, localPath, filePath);
			Assert.assertFalse(true, "File is unExisted, except throw e, but success.");
		} catch (ScmException e) {
			if (e.getError() != ScmError.FILE_NOT_FOUND ) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}
}
