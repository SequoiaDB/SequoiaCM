package com.sequoiacm.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-422: 匹配多条任务记录，获取任务列表
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、获取任务列表，查询条件覆盖： 1）匹配不同ws下的迁移任务； 2）匹配同一个ws下的迁移任务（包括已完成的任务）；
 * 3）使用ScmQueryBuilder匹配符查询； 接口覆盖：hasNext()、getNext()、close()； 2、获取任务基本信息
 * 3、检查游标及任务信息正确性；
 */

public class Transfer_listTask422 extends TestScmBase {
	private boolean runSuccess = false;
	private final int fileSize = 200 * 1024;
	private final int fileNum = 100;
	private List<ScmId> fileIdList1 = new ArrayList<ScmId>();
	private List<ScmId> fileIdList2 = new ArrayList<ScmId>();

	private File localPath = null;
	private String filePath = null;

	private String authorName = "ListTask422";

	private ScmSession session = null;

	private ScmId taskId1 = null;
	private ScmId taskId2 = null;
	private List<ScmId> taskIdList = new ArrayList<ScmId>();
	
	private SiteWrapper branceSite = null;
	private List<WsWrapper> wsList_T = new ArrayList<WsWrapper>();
	
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
			wsList_T = ScmInfo.getWss(2);

			//session = TestScmTools.createSession(TestScmBase.hostName2, TestScmBase.port2);
			session = TestScmTools.createSession(branceSite);
			
			ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace(wsList_T.get(0).getName(), session);
			prepareFiles1(ws1);
			ScmWorkspace ws2 = ScmFactory.Workspace.getWorkspace(wsList_T.get(1).getName(), session);
			prepareFiles2(ws2);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
			if (session != null) {
				session.close();
			}
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void listMultiWs() throws Exception {
		ScmCursor<ScmTaskBasicInfo> cursor = null;
		try {
			taskId1 = transferAllFile(session, wsList_T.get(0).getName());
			ScmTaskUtils.waitTaskFinish(session, taskId1);

			taskId2 = transferAllFile(session,  wsList_T.get(1).getName());
			ScmTaskUtils.waitTaskFinish(session, taskId2);

			String taskIdKey = ScmAttributeName.Task.ID;

			BSONObject subCond1 = ScmQueryBuilder.start(taskIdKey).is(taskId1.get()).get();
			BSONObject subCond2 = ScmQueryBuilder.start(taskIdKey).is(taskId2.get()).get();
			BSONObject cond = ScmQueryBuilder.start().or(subCond1, subCond2).get();
			cursor = ScmSystem.Task.listTask(session, cond);
			while (cursor.hasNext()) {
				ScmTaskBasicInfo info = cursor.getNext();
				if (info.getId().get().equals(taskId1.get())) {
					Assert.assertEquals(info.getWorkspaceName(), wsList_T.get(0).getName());
				} else if (info.getId().get().equals(taskId2.get())) {
					Assert.assertEquals(info.getWorkspaceName(), wsList_T.get(1).getName());
				} else {
					Assert.fail("unknown taskId: " + info.getId().get());
				}
			}
			cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		runSuccess = true;
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void listOneWs() throws Exception {
		try {
			taskIdList = new ArrayList<ScmId>();
			int taskNum = 10;
			for (int i = 0; i < taskNum; ++i) {
				ScmId taskId = transferAllFile(session, wsList_T.get(0).getName());
				ScmTaskUtils.waitTaskFinish(session, taskId);
				taskIdList.add(taskId);
			}

			String taskIdKey = ScmAttributeName.Task.ID;
			ScmQueryBuilder cond = ScmQueryBuilder.start();
			for (ScmId taskId : taskIdList) {
				BSONObject subCond = ScmQueryBuilder.start(taskIdKey).is(taskId.get()).get();
				cond.or(subCond);
			}

			ScmCursor<ScmTaskBasicInfo> cursor = ScmSystem.Task.listTask(session, cond.get());
			while (cursor.hasNext()) {
				ScmTaskBasicInfo info = cursor.getNext();
				
				Assert.assertEquals(info.getRunningFlag(), CommonDefine.TaskRunningFlag.SCM_TASK_FINISH); // 3:
																											// finish
				Assert.assertEquals(info.getType(), CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE); // 1:
																									// transfer
				Assert.assertEquals(info.getWorkspaceName(), wsList_T.get(0).getName());
				Assert.assertTrue(taskIdList.contains(info.getId()));
				
				Assert.assertEquals(info.getTargetSite(),ScmInfo.getRootSite().getSiteId());
				Assert.assertNull(info.getScheduleId());
				Assert.assertNotNull(info.getStartTime());
			}
			cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage()+"taskList Info "+taskIdList.toString());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		ScmSession mainSession = null;
		try {
			if (runSuccess || TestScmBase.forceClear) {
				mainSession = TestScmTools.createSession(ScmInfo.getRootSite());
				ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace(wsList_T.get(0).getName(), mainSession);
				for (int i = 0; i < fileNum; ++i) {
					ScmFactory.File.getInstance(ws1, fileIdList1.get(i)).delete(true);
				}
				ScmWorkspace ws2 = ScmFactory.Workspace.getWorkspace(wsList_T.get(1).getName(), mainSession);
				for (int i = 0; i < fileNum; ++i) {
					ScmFactory.File.getInstance(ws2, fileIdList2.get(i)).delete(true);
				}
				TestTools.LocalFile.removeFile(localPath);

				for (int i = 0; i < taskIdList.size(); ++i) {
					TestSdbTools.Task.deleteMeta(taskIdList.get(i));
				}
				TestSdbTools.Task.deleteMeta(taskId1);
				TestSdbTools.Task.deleteMeta(taskId2);
			}
		} catch (BaseException | ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (mainSession != null) {
				mainSession.close();
			}
			if (session != null) {
				session.close();
			}

		}
	}

	private void prepareFiles1(ScmWorkspace ws) throws Exception {

		for (int i = 0; i < fileNum; ++i) {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setFileName(authorName+"_"+i);
			scmfile.setAuthor(authorName);
			scmfile.setTitle("ws1");
			scmfile.setContent(filePath);
			fileIdList1.add(scmfile.save());
		}
	}

	private void prepareFiles2(ScmWorkspace ws) throws Exception {
		for (int i = 0; i < fileNum; ++i) {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setFileName(authorName+"_"+UUID.randomUUID());
			scmfile.setAuthor(authorName);
			scmfile.setTitle("ws2");
			scmfile.setContent(filePath);
			fileIdList2.add(scmfile.save());
		}
	}

	private ScmId transferAllFile(ScmSession session, String wsName) throws ScmException {

		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, session);
		BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		return ScmSystem.Task.startTransferTask(ws, condition);
	}
}