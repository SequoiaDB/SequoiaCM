package com.sequoiacm.net.task.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.*;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @FileName SCM-443:并发获取任务列表（matcher存在交集）
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、多线程并发获取任务列表，并获取任务基本信息； 2、检查返回结果正确性；
 */
public class Transfer_getTaskList443 extends TestScmBase {

	private boolean runSuccess = false;
	private File localPath = null;
	private String filePath = null;
	private int FILE_SIZE = new Random().nextInt(1024) + 1024 * 1024 * 10;
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private static ScmId taskId = null;
	private int fileNum = 3;
	private BSONObject cond = null;
	private String authorName = "GetTaskList443";
	
	private SiteWrapper rootSite = null;
	private List<SiteWrapper> siteList = new ArrayList<SiteWrapper>();
	private WsWrapper ws_T = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + FILE_SIZE + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, FILE_SIZE);
			
			ws_T = ScmInfo.getWs();
			siteList = ScmNetUtils.getAllSite(ws_T);
			rootSite = siteList.get(2);
			
			cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(ws_T,cond);
			session = TestScmTools.createSession(siteList.get(1));
			ws =  ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
			prepareFiles(session);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "fourSite" })
	private void test() {
		try {
			TaskThread TaskThreadM = new TaskThread(rootSite);
			TaskThread TaskThreadA = new TaskThread(siteList.get(0));
			TaskThread TaskThreadB = new TaskThread(siteList.get(1));

			startTask();
			ScmTaskUtils.waitTaskFinish(session, taskId);

			TaskThreadM.start(50);
			TaskThreadA.start(50);
			TaskThreadB.start(50);

			Assert.assertTrue(TaskThreadM.isSuccess(), TaskThreadM.getErrorMsg());
			Assert.assertTrue(TaskThreadA.isSuccess(), TaskThreadA.getErrorMsg());
			Assert.assertTrue(TaskThreadB.isSuccess(), TaskThreadB.getErrorMsg());

			checkTaskList(TaskThreadM.getCursor());
			checkTaskList(TaskThreadA.getCursor());
			checkTaskList(TaskThreadB.getCursor());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} 
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFileUtils.cleanFile(ws_T,cond);
				TestTools.LocalFile.removeFile(localPath);
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

	private void prepareFiles(ScmSession session) throws Exception {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
		for (int i = 0; i < fileNum; ++i) {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setFileName(authorName+"_"+UUID.randomUUID());
			scmfile.setAuthor(authorName);
			scmfile.setContent(filePath);
		}
	}

	private void startTask() {
		try {
			taskId = ScmSystem.Task.startTransferTask(ws, cond, ScopeType.SCOPE_CURRENT, rootSite.getSiteName());
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	private class TaskThread extends TestThreadBase {
		private ScmCursor<ScmTaskBasicInfo> cursor;
		private SiteWrapper site;

		TaskThread(SiteWrapper site) {
			this.site = site;
		}

		@Override
		public void exec() throws Exception {
			ScmSession ss = null;
			try {
				ss = TestScmTools.createSession(site);
				cursor = ScmSystem.Task.listTask(ss, cond);				
			} catch (ScmException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (null != ss) {
					ss.close();
				}
			}
		} 

		public ScmCursor<ScmTaskBasicInfo> getCursor() {
			return cursor;
		}
	}

	private void checkTaskList(ScmCursor<ScmTaskBasicInfo> cursor) {
		ScmTaskBasicInfo basicInfo = null;
		try {
			while (cursor.hasNext()) {
				basicInfo = cursor.getNext();
				Assert.assertEquals(basicInfo.getRunningFlag(), CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);
				Assert.assertEquals(basicInfo.getWorkspaceName(), ws);
				Assert.assertEquals(basicInfo.getType(), 1);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
