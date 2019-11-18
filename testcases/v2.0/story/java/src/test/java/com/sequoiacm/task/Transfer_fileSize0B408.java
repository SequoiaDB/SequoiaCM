package com.sequoiacm.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
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
import com.sequoiacm.client.element.ScmTask;
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

/**
 * @FileName SCM-408: 迁移0个文件
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、在分中心A写多个文件； 2、在分中心A开始迁移任务，指定迁移条件匹配0个文件； 3、检查迁移任务执行结果；
 */
public class Transfer_fileSize0B408 extends TestScmBase {
	private boolean runSuccess = false;
	private File localPath = null;
	private String filePath = null;
	private List<ScmId> fileIdList = new ArrayList<>();
	private int fileNum = 5;
	private String authorName = "Transfer0File408";
	private final int FILE_SIZE = new Random().nextInt(1024) + 1;
	private ScmSession sessionA = null;
	private ScmWorkspace ws = null;
	private ScmId taskId = null;
	
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

			sessionA = TestScmTools.createSession(branceSite);
			ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
			
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(ws_T, cond);
			
			for (int i = 0; i < fileNum; i++) {
				ScmId fileId = createFile(ws, filePath);
				fileIdList.add(fileId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws ScmException {
		startTask();
		waitTaskStop();
		checkTaskAttribute();
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				for(ScmId fileId : fileIdList){
					ScmFactory.File.deleteInstance(ws, fileId, true);
				}
				TestTools.LocalFile.removeFile(localPath);
				TestSdbTools.Task.deleteMeta(taskId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}

		}
	}

	private ScmId createFile(ScmWorkspace ws, String filePath) throws ScmException {
		ScmId fileId = null;
		ScmFile scmfile = ScmFactory.File.createInstance(ws);
		scmfile.setContent(filePath);
		scmfile.setFileName(authorName+"_"+UUID.randomUUID());
		scmfile.setAuthor(authorName);
		fileId = scmfile.save();
		return fileId;
	}

	private void startTask() {
		try {
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName + "_NoExist").get();
			taskId = ScmSystem.Task.startTransferTask(ws, cond);
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		}
	}

	private void waitTaskStop() throws ScmException {
		Date stopTime = null;
		while (stopTime == null) {
			stopTime = ScmSystem.Task.getTask(sessionA, taskId).getStopTime();
		}
	}

	private void checkTaskAttribute() throws ScmException {
		ScmTask task = ScmSystem.Task.getTask(sessionA, taskId);
		Assert.assertEquals(task.getId(), taskId);
		Assert.assertEquals(task.getProgress(), 100);
		Assert.assertEquals(task.getRunningFlag(), CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);
		Assert.assertEquals(task.getType(), CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE);
		Assert.assertEquals(task.getWorkspaceName(), ws.getName());
	}
}
