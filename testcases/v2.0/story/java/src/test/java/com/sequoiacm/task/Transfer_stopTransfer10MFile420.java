package com.sequoiacm.task;

import java.io.File;
import java.util.Date;
import java.util.UUID;

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
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-420: 迁移单个<=10M的文件过程中停止迁移任务
 * @Author linsuqiang
 * @Date 2017-08-25
 * @Version 2.00
 */

/*
 * 1、在分中心A写入1个=10M的文件（<10M在其他测试点已覆盖测试）； 2、在分中心A迁移该文件（迁移该文件部分内容）；
 * 3、“执行迁移文件”过程中停止该迁移任务； 4、检查执行结果正确性；
 */

public class Transfer_stopTransfer10MFile420 extends TestScmBase {
	private boolean runSuccess = false;
	private final int fileSize = 9 * 1024 * 1024;
	private final int fileNum = 1;
	private final String authorName = "StopTransfer10MFile420";

	private File localPath = null;
	private String filePath = null;

	private ScmSession sessionA = null;
	private ScmWorkspace ws = null;
	private ScmId fileId = null;

	private ScmId taskId = null;
	
	private SiteWrapper branceSite = null;
	private WsWrapper ws_T = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			// ready file
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
			
			branceSite = ScmInfo.getBranchSite();
		    ws_T = ScmInfo.getWs();

			sessionA = TestScmTools.createSession(branceSite);
			ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
			
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(ws_T, cond);
			
			prepareFiles(sessionA);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
			if (sessionA != null) {
				sessionA.close();
			}
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			taskId = transferAllFile(sessionA);
			waitTaskRunning();
			ScmSystem.Task.stopTask(sessionA, taskId);
			waitStopFinish(sessionA, taskId);
			Assert.assertEquals(ScmSystem.Task.getTask(sessionA, taskId).getRunningFlag(),
					CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL); // 4: cancel
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
				ScmFactory.File.deleteInstance(ws, fileId, true);;
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

	private void prepareFiles(ScmSession session) throws Exception {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
		for (int i = 0; i < fileNum; ++i) {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setFileName(authorName+"_"+UUID.randomUUID());
			scmfile.setAuthor(authorName);
			scmfile.setContent(filePath);
			fileId = scmfile.save();
		}
	}

	private ScmId transferAllFile(ScmSession session) throws ScmException {
		BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		return ScmSystem.Task.startTransferTask(ws, condition);
	}

	
	/*private void waitTaskRunning() throws InterruptedException {
		Sequoiadb db = null;
		int retryNum = 30;
		int sleepSecond = 10;
		boolean isRunning = false;
		try {
			db = TestSdbTools.getSdb(TestScmBase.mainSdbUrl);
			String CSName = TestSdbTools.getFileDataCsName(ws_T);
			String CLName = TestSdbTools.getFileDataClName(ws_T);
			DBCollection cl = db.getCollectionSpace(CSName).getCollection(CLName);
			ObjectId lobObjId = new ObjectId(fileId.get());
			for (int i = 0; i < retryNum; i++) {
				try {
					cl.openLob(lobObjId);
				} catch (Exception e) {
					if (e.getMessage().contains("LOB is not useable")) {
						System.out.println("Msg = " + e.getMessage());
						isRunning = true;
						break;
					}
				} finally {
					if (i == retryNum - 1 && !isRunning) {
						System.out.println("Msg = retry is over,fileId is " + fileId.get());
					}
				}
				Thread.sleep(sleepSecond);
			}
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}*/
	
	private void waitTaskRunning()throws ScmException {
		Date startTime = null;
		while (startTime == null) {
			startTime = ScmSystem.Task.getTask(sessionA, taskId).getStartTime();
		}
	}

	private void waitStopFinish(ScmSession session, ScmId taskId) throws ScmException {
		Date stopTime = null;
		while (stopTime == null) {
			stopTime = ScmSystem.Task.getTask(session, taskId).getStopTime();
		}
	}

	private void checkTransfered() {
		try {
			SiteWrapper rootSite = ScmInfo.getRootSite();
			SiteWrapper[] expSiteList = { rootSite, branceSite };
			ScmFileUtils.checkMetaAndData(ws_T,fileId, expSiteList, localPath, filePath);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}