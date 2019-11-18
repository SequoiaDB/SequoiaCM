package com.sequoiacm.net.task.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Testcase: SCM-438:并发创建不同ws下相同条件的迁移任务
 * @author huangxiaoni init
 * @date 2017.6.14
 */

public class Transfer_inDiffWs438 extends TestScmBase {
	private boolean runSuccess = false;

	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private ScmWorkspace newWs = null;
	private List<ScmId> taskIdList = Collections.synchronizedList(new ArrayList<ScmId>());
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private String authorName = "transfer438";
	private int fileSize = 10;
	private int fileNum = 10;
	private int startNum = 2;
	private File localPath = null;
	private List<String> filePathList = new ArrayList<String>();
	
	private SiteWrapper sourceSite = null;
	private SiteWrapper targetSite = null;
	private List<WsWrapper> ws_TList = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		try {
			// ready file
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			for (int i = 0; i < fileNum; i++) {
				String filePath = localPath + File.separator + "localFile_" + fileSize + i + ".txt";
				TestTools.LocalFile.createFile(filePath, fileSize + i);
				filePathList.add(filePath);
			}
			
			ws_TList = ScmInfo.getWss(2);
			List<SiteWrapper> siteList = ScmNetUtils.getRandomSites(ws_TList.get(0));
			sourceSite = siteList.get(0);
			targetSite = siteList.get(1);
			
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(ws_TList.get(0),cond);
			ScmFileUtils.cleanFile(ws_TList.get(1),cond);

			session = TestScmTools.createSession(sourceSite);
			ws = ScmFactory.Workspace.getWorkspace(ws_TList.get(0).getName(), session);
			newWs = ScmFactory.Workspace.getWorkspace(ws_TList.get(1).getName(), session);

			readyFile(ws);
			readyFile(newWs);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() {
		try {
			StartTaskInWs stWs = new StartTaskInWs();
			stWs.start();

			StartTaskInWs2 stWs2 = new StartTaskInWs2();
			stWs2.start();

			if (!(stWs.isSuccess() && stWs2.isSuccess())) {
				Assert.fail(stWs.getErrorMsg() + stWs2.getErrorMsg());
			}

			ScmTaskUtils.waitTaskFinish(session, taskIdList.get(0));
			ScmTaskUtils.waitTaskFinish(session, taskIdList.get(1));

			checkMetaAndLobs();

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || forceClear) {
				for (int i = 0; i < fileIdList.size(); i++) {
					ScmId fileId = fileIdList.get(i);
					if (i < fileIdList.size() / 2) {
						ScmFactory.File.deleteInstance(ws, fileId, true);
					} else {
						ScmFactory.File.deleteInstance(newWs, fileId, true);
					}
				}
				TestTools.LocalFile.removeFile(localPath);

				for (ScmId taskId : taskIdList) {
					TestSdbTools.Task.deleteMeta(taskId);
				}
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

	private class StartTaskInWs extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession ss = null;
			try {
				// login
				ss = TestScmTools.createSession(sourceSite);
				ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace(ws_TList.get(0).getName(), ss);

				// start task
				int value = fileSize + startNum;
				BSONObject condition = ScmQueryBuilder.start().put(ScmAttributeName.File.SIZE).greaterThanEquals(value)
						.put(ScmAttributeName.File.AUTHOR).is(authorName).get();
				ScmId taskId = ScmSystem.Task.startTransferTask(ws1, condition, ScopeType.SCOPE_CURRENT, targetSite.getSiteName());
				taskIdList.add(taskId);
				
				ScmTaskUtils.waitTaskFinish(ss, taskId);

				// check task info
				ScmTask taskInfo = ScmSystem.Task.getTask(ss, taskId);
				Assert.assertEquals(taskInfo.getProgress(), 100);
				Assert.assertEquals(taskInfo.getRunningFlag(), CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);
				Assert.assertEquals(taskInfo.getWorkspaceName(), ws_TList.get(0).getName());
				Assert.assertEquals(taskInfo.getContent(), condition);
				Assert.assertNotNull(taskInfo.getStopTime());
			} finally {
				if (ss != null) {
					ss.close();
				}
			}
		}
	}

	private class StartTaskInWs2 extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession ss = null;
			try {
				// login
				ss = TestScmTools.createSession(sourceSite);
				ScmWorkspace ws2 = ScmFactory.Workspace.getWorkspace(ws_TList.get(1).getName(), ss);

				// start task
				int value = fileSize + startNum;

				BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.SIZE).greaterThanEquals(value)
						.and(ScmAttributeName.File.AUTHOR).is(authorName).get();

				ScmId taskId = ScmSystem.Task.startTransferTask(ws2, condition, ScopeType.SCOPE_CURRENT, targetSite.getSiteName());
				taskIdList.add(taskId);
				
				ScmTaskUtils.waitTaskFinish(ss, taskId);

				// check task info
				ScmTask taskInfo =  ScmSystem.Task.getTask(ss, taskId);
				Assert.assertEquals(taskInfo.getProgress(), 100);
				Assert.assertEquals(taskInfo.getRunningFlag(), CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);
				Assert.assertEquals(taskInfo.getWorkspaceName(), ws_TList.get(1).getName());
				Assert.assertEquals(taskInfo.getContent(), condition);
				Assert.assertNotNull(taskInfo.getStopTime());
			} finally {
				if (ss != null) {
					ss.close();
				}
			}
		}
	}

	private void readyFile(ScmWorkspace ws) throws ScmException {
		for (int i = 0; i < fileNum; i++) {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setContent(filePathList.get(i));
			file.setFileName(authorName+"_"+UUID.randomUUID());
			file.setAuthor(authorName);
			ScmId fileId = file.save();
			fileIdList.add(fileId);
		}
	}

	private void checkMetaAndLobs() {
		try {
			for (int i = 0; i < fileNum; i++) {
				String filePath = filePathList.get(i);
				ScmId fileId = fileIdList.get(i);
				if (i >= startNum) {
					SiteWrapper[] expSiteList = { sourceSite, targetSite };
					ScmFileUtils.checkMetaAndData(ws_TList.get(0),fileId, expSiteList, localPath, filePath);
				} else {
					SiteWrapper[] expSiteIdList = { sourceSite };
					ScmFileUtils.checkMetaAndData(ws_TList.get(0),fileId, expSiteIdList, localPath, filePath);
				}
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
