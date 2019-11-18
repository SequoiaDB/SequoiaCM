package com.sequoiacm.task;

import java.io.File;
import java.util.Random;
import java.util.UUID;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @FileName SCM-425: 重复close
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、获取任务列表游标，任务列表至少包含1条记录； 2、重复多次close游标； 3、检查执行结果正确性；
 */

public class Transfer_repeatCloseCursor425 extends TestScmBase {
	private boolean runSuccess = false;
	private File localPath = null;
	private String filePath = null;
	private int FILE_SIZE = new Random().nextInt(1024) + 1024;
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private ScmId taskId = null;
	
	private String authorName = "CreateMultiTasks409";
	private ScmId fileId = null;
	private BSONObject cond = null;
	
	private SiteWrapper branceSite = null;
	private WsWrapper ws_T = null;
	

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + FILE_SIZE + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, FILE_SIZE);
			
			branceSite = ScmInfo.getBranchSite();
		    ws_T = ScmInfo.getWs();

			session = TestScmTools.createSession( branceSite);
			ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
			
			cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(ws_T, cond);
			
			createFile(ws, filePath);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws ScmException {
		startTask();
		try {
			ScmTaskUtils.waitTaskFinish(session, taskId);
		} catch (Exception e) {
			Assert.fail(e.getMessage() + ",taskId = " + taskId.get());
		}
		checkRepeatCloseCursor();
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				TestTools.LocalFile.removeFile(localPath);
				ScmFactory.File.deleteInstance(ws, fileId, true);
				TestSdbTools.Task.deleteMeta(taskId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}

	private void createFile(ScmWorkspace ws, String filePath) throws ScmException {
		ScmFile scmfile = ScmFactory.File.createInstance(ws);
		scmfile.setContent(filePath);
		scmfile.setFileName(authorName+"_"+UUID.randomUUID());
		scmfile.setAuthor(authorName);
		fileId = scmfile.save();
	}

	private void startTask() {
		try {
			taskId = ScmSystem.Task.startTransferTask(ws, cond);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	private void checkRepeatCloseCursor() throws ScmException {
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.Task.WORKSPACE).lessThanEquals("ws").get();
		ScmCursor<ScmTaskBasicInfo> cursor = ScmSystem.Task.listTask(session, cond);
		if (cursor != null) {
			for (int i = 0; i < 3; i++) {
				cursor.close();
			}
		}
	}
}
