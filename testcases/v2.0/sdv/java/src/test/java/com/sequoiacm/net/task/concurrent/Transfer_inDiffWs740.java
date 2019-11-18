package com.sequoiacm.net.task.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-740:并发迁移多个ws下的文件
 * @author huangxiaoni init
 * @date 2017.8.14
 */

public class Transfer_inDiffWs740 extends TestScmBase {
	private boolean runSuccess = false;

	private ScmSession sessionA = null;
	private String author = "TD740";
	private List<ScmId> fileIdList1 = new ArrayList<ScmId>();
	private List<ScmId> fileIdList2 = new ArrayList<ScmId>();
	private List<ScmId> fileIdList3 = new ArrayList<ScmId>();
	private int fileSize = 10;
	private int fileNum = 100;
	private File localPath = null;
	private String filePath = null;
	
	private SiteWrapper sourceSite = null;
	private SiteWrapper targetSite = null;
	private List<WsWrapper> ws_TList = new ArrayList<WsWrapper>();

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
			
			ws_TList = ScmInfo.getWss(3);
			List<SiteWrapper> siteList = ScmNetUtils.getCleanSites(ws_TList.get(0));
			sourceSite = siteList.get(0);
			targetSite = siteList.get(1);
			
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(ws_TList.get(0),cond);
			ScmFileUtils.cleanFile(ws_TList.get(1),cond);
			ScmFileUtils.cleanFile(ws_TList.get(2),cond);

			sessionA = TestScmTools.createSession(sourceSite);
			this.writeFile(ws_TList.get(0).getName(), fileIdList1);
			this.writeFile(ws_TList.get(1).getName(), fileIdList2);
			this.writeFile(ws_TList.get(2).getName(), fileIdList3);
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() {
		try {
			StartTransferTaskInWs tfWs1 = new StartTransferTaskInWs();
			tfWs1.start();

			StartTransferTaskInWs2 tfWs2 = new StartTransferTaskInWs2();
			tfWs2.start();

			StartTransferTaskInWs3 tfWs3 = new StartTransferTaskInWs3();
			tfWs3.start();

			if (!(tfWs1.isSuccess() && tfWs2.isSuccess() && tfWs3.isSuccess())) {
				Assert.fail(tfWs1.getErrorMsg() + tfWs2.getErrorMsg() + tfWs3.getErrorMsg());
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		ScmSession sessionM = null;
		try {
			if (runSuccess || forceClear) {
				sessionM = TestScmTools.createSession(targetSite);

				ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace(ws_TList.get(0).getName(), sessionM);
				for (ScmId fileId : fileIdList1) {
					ScmFactory.File.deleteInstance(ws1, fileId, true);
				}

				ScmWorkspace ws2 = ScmFactory.Workspace.getWorkspace(ws_TList.get(1).getName(), sessionM);
				for (ScmId fileId : fileIdList2) {
					ScmFactory.File.deleteInstance(ws2, fileId, true);
				}

				ScmWorkspace ws3 = ScmFactory.Workspace.getWorkspace(ws_TList.get(2).getName(), sessionM);
				for (ScmId fileId : fileIdList3) {
					ScmFactory.File.deleteInstance(ws3, fileId, true);
				}

				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (BaseException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionM != null) {
				sessionM.close();
			}
			if(sessionA != null){
				sessionA.close();
			}
		}
	}

	private class StartTransferTaskInWs extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession sessionA = null;
			ScmWorkspace wsA = null;
			String wsName = ws_TList.get(0).getName();
			List<ScmId> fileIdList = fileIdList1;
			try {
				sessionA = TestScmTools.createSession(sourceSite);
				wsA = ScmFactory.Workspace.getWorkspace(wsName, sessionA);

				BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
				ScmId taskId = ScmSystem.Task.startTransferTask(wsA, condition, ScopeType.SCOPE_CURRENT, targetSite.getSiteName());

				// check task info
				ScmTask taskInfo = null;
				while (true) {
					taskInfo = ScmSystem.Task.getTask(sessionA, taskId);
					if (taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH) {
						break;
					}
					Thread.sleep(200);
				}

				// check results
				Assert.assertEquals(taskInfo.getWorkspaceName(), wsName);
				Assert.assertEquals(taskInfo.getType(), CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE);

				SiteWrapper[] expSiteList = { sourceSite, targetSite };
				ScmFileUtils.checkMetaAndData(ws_TList.get(0), fileIdList, expSiteList, localPath, filePath);
			} finally {
				if (sessionA != null) {
					sessionA.close();
				}
			}
		}
	}

	private class StartTransferTaskInWs2 extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession sessionA = null;
			ScmWorkspace wsA = null;
			String wsName = ws_TList.get(1).getName();
			List<ScmId> fileIdList = fileIdList2;
			try {
				sessionA = TestScmTools.createSession(sourceSite);
				wsA = ScmFactory.Workspace.getWorkspace(wsName, sessionA);

				BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
				ScmId taskId = ScmSystem.Task.startTransferTask(wsA, condition, ScopeType.SCOPE_CURRENT, targetSite.getSiteName());

				// check task info
				ScmTask taskInfo = null;
				while (true) {
					taskInfo = ScmSystem.Task.getTask(sessionA, taskId);
					if (taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH) {
						break;
					}
					Thread.sleep(200);
				}

				// check results
				Assert.assertEquals(taskInfo.getWorkspaceName(), wsName);
				Assert.assertEquals(taskInfo.getType(), CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE);

				SiteWrapper[] expSiteList = { sourceSite, targetSite };
				ScmFileUtils.checkMetaAndData(ws_TList.get(1), fileIdList, expSiteList, localPath, filePath);
			} finally {
				if (sessionA != null) {
					sessionA.close();
				}
			}
		}
	}

	private class StartTransferTaskInWs3 extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession sessionA = null;
			ScmWorkspace wsA = null;
			String wsName = ws_TList.get(2).getName();
			List<ScmId> fileIdList = fileIdList3;
			try {
				sessionA = TestScmTools.createSession(sourceSite);
				wsA = ScmFactory.Workspace.getWorkspace(wsName, sessionA);

				BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
				ScmId taskId = ScmSystem.Task.startTransferTask(wsA, condition, ScopeType.SCOPE_CURRENT, targetSite.getSiteName());

				// check task info
				ScmTask taskInfo = null;
				while (true) {
					taskInfo = ScmSystem.Task.getTask(sessionA, taskId);
					if (taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH) {
						break;
					}
					Thread.sleep(200);
				}

				// check results
				Assert.assertEquals(taskInfo.getWorkspaceName(), wsName);
				Assert.assertEquals(taskInfo.getType(), CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE);

				SiteWrapper[] expSiteList = { sourceSite, targetSite };
				ScmFileUtils.checkMetaAndData(ws_TList.get(2), fileIdList, expSiteList, localPath, filePath);
			} finally {
				if (sessionA != null) {
					sessionA.close();
				}
			}
		}
	}

	private void writeFile(String wsName, List<ScmId> fileIdList) throws ScmException {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, sessionA);
		for (int i = 0; i < fileNum; i++) {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setContent(filePath);
			scmfile.setFileName(author+"_"+i+UUID.randomUUID());
			scmfile.setAuthor(author);
			fileIdList.add(scmfile.save());
		}
	}

}
