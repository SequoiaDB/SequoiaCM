package com.sequoiacm.task.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-431: “执行迁移任务”过程中主中心读取文件内容
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、A线程在分中心A创建迁移任务； 2、“执行迁移任务”过程中（调用ScmTask.getRunningFlag()接口获取任务状态为running）
 * B线程在主中心读取正在被迁移的文件； 3、检查A、B线程执行结果正确性；
 */

public class Transfer_readWhenTaskRunning431 extends TestScmBase {
	private boolean runSuccess = false;

	private int fileSize = 50 * 1024;
	private int fileNum = 20;
	private File localPath = null;
	private String filePath = null;
	private String authorName = "ReadWhenTaskRunning431";

	private ScmSession sessionA = null;
	private ScmWorkspace ws = null;
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private ScmId taskId = null;

	private SiteWrapper rootSite = null;
	private SiteWrapper branceSite = null;
	private WsWrapper ws_T = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);

			rootSite = ScmInfo.getRootSite();
			branceSite = ScmInfo.getBranchSite();
			ws_T = ScmInfo.getWs();

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(ws_T, cond);

			sessionA = TestScmTools.createSession(branceSite);
			ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
			prepareFiles(sessionA);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			taskId = transferAllFile(sessionA);
			
			ReadThread readThd = new ReadThread();
			readThd.start(30); 
			Assert.assertTrue(readThd.isSuccess(), readThd.getErrorMsg());
			
			ScmTaskUtils.waitTaskFinish(sessionA, taskId);
			checkTransfered();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
				ScmFileUtils.cleanFile(ws_T, cond);
				TestTools.LocalFile.removeFile(localPath);
				TestSdbTools.Task.deleteMeta(taskId);
			}
		} catch (BaseException | ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
		}
	}

	private class ReadThread extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			OutputStream fos = null;
			ScmInputStream sis = null;
			try {
				session = TestScmTools.createSession(rootSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
				for (int i = fileNum - 1; i >= 0; --i) {
					ScmId fileId = fileIdList.get(i);
					synchronized (fileId) {
						ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
						String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
								Thread.currentThread().getId());
						fos = new FileOutputStream(new File(downloadPath));
						sis = ScmFactory.File.createInputStream(scmfile);
						sis.read(fos);
					}
				}
			} finally {
				if (fos != null)
					fos.close();
				if (sis != null)
					sis.close();
				if (session != null) {
					session.close();
				}
			}
			/*
			 * ScmTask taskInfo = ScmSystem.Task.getTask(ss, taskId); int status
			 * = taskInfo.getRunningFlag(); try { if (isRunning()) { if (status
			 * == CommonDefine.TaskRunningFlag.SCM_TASK_RUNNING) { // 2:running
			 * readFilesInversely(ss); } else if (status ==
			 * CommonDefine.TaskRunningFlag.SCM_TASK_ABORT) { throw new
			 * Exception("task was aborted, taskId = " + taskId.get()); } else
			 * if (status == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH) {
			 * throw new Exception("task was finished, taskId = " +
			 * taskId.get()); } else { throw new Exception(
			 * "task was canceled,taskId = " + taskId.get()); }
			 * System.out.println("---runningFlag: " + status); } else {
			 * System.out.println("---runningFlag: " + status);
			 * TestSdbTools.Task.deleteMeta(taskId); throw new Exception(
			 * "task was not running,taskInfo = " + taskInfo.toString()); } }
			 */
		}

		/*
		 * private boolean isRunning() throws Exception { ScmSession session =
		 * null; boolean isRunningFlag = false; try { session =
		 * TestScmTools.createSession(rootSite); ScmWorkspace ws =
		 * ScmFactory.Workspace.getWorkspace(ws_T.getName(), session); int
		 * tryNum = 10; for (int i = 0; i < tryNum; i++) { for (ScmId fileId :
		 * fileIdList) { ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		 * int num = file.getLocationList().size(); if (num == 2) {
		 * isRunningFlag = true; break; } } Thread.sleep(100); } } catch
		 * (ScmException e) { Assert.fail(e.getMessage()); } finally { if (null
		 * != session) { session.close(); } } return isRunningFlag; }
		 */
	}

	private void prepareFiles(ScmSession session) throws Exception {
		for (int i = 0; i < fileNum; ++i) {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setContent(filePath);
			scmfile.setFileName(authorName+"_"+UUID.randomUUID());
			scmfile.setAuthor(authorName);
			fileIdList.add(scmfile.save());
		}
	}

	private ScmId transferAllFile(ScmSession session) throws ScmException {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
		BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		return ScmSystem.Task.startTransferTask(ws, condition);
	}

	private void checkTransfered() {
		try {
			SiteWrapper[] expSiteList = { rootSite, branceSite };
			ScmFileUtils.checkMetaAndData(ws_T, fileIdList, expSiteList, localPath, filePath);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}