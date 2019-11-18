package com.sequoiacm.net.task.concurrent;

import java.io.File;
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
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Testcase: SCM-743:并发迁移任务、单个异步迁移不同文件
 * @author huangxiaoni init
 * @date 2017.8.14
 */

public class TransferTaskAndAsyncTransferDiffFile743 extends TestScmBase {
	private boolean runSuccess = false;

	private ScmSession sessionA = null;
	private ScmSession sessionB = null;
	private ScmWorkspace wsA = null;
	private ScmWorkspace wsB = null;

	private String author1 = "TD743_1";
	private String author2 = "TD743_2";
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private int fileSize = 10;
	private int fileNum = 200;
	private File localPath = null;
	private String filePath = null;
	private ScmId taskId = null;
	
	private SiteWrapper sourceSite1 = null;
	private SiteWrapper targetSite1 = null;
	private SiteWrapper sourceSite2 = null;
	private SiteWrapper targetSite2 = null;
	//private NodeWrapper node = null;
	private WsWrapper ws_T = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			// ready local file
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
			
			ws_T = ScmInfo.getWs();
			List<SiteWrapper> siteList1 = ScmNetUtils.getRandomSites(ws_T);
			sourceSite1 = siteList1.get(0);
			targetSite1 = siteList1.get(1);
			List<SiteWrapper> siteList2 = ScmNetUtils.getSortSites(ws_T);
			sourceSite2 = siteList2.get(0);
			targetSite2 = siteList2.get(1);

			BSONObject ors1 = ScmQueryBuilder.start().put(ScmAttributeName.File.AUTHOR).is(author1).get();
			BSONObject ors2 = ScmQueryBuilder.start().put(ScmAttributeName.File.AUTHOR).is(author2).get();
			BSONObject cond = ScmQueryBuilder.start().or(ors1, ors2).get();
			ScmFileUtils.cleanFile(ws_T,cond);

			// login
			sessionA = TestScmTools.createSession(sourceSite1);
			sessionB = TestScmTools.createSession(sourceSite2);
			wsA = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
			wsB = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionB);

			// ready file
			this.writeFile();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			StartTransferTask transferTask = new StartTransferTask();
			transferTask.start();

			AsyncTransfer asyncTransfer = new AsyncTransfer();
			asyncTransfer.start();

			if (!(transferTask.isSuccess() && asyncTransfer.isSuccess())) {
				Assert.fail(transferTask.getErrorMsg() + asyncTransfer.getErrorMsg());
			}

		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}

		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || forceClear) {
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(wsA, fileId, true);
				}
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
			if (sessionB != null) {
				sessionB.close();
			}

		}
	}

	private class StartTransferTask extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession sessionA = null;
			ScmWorkspace wsA = null;
			String wsName = ws_T.getName();
			try {
				sessionA = TestScmTools.createSession(sourceSite1);
				wsA = ScmFactory.Workspace.getWorkspace(wsName, sessionA);

				BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author1).get();
				taskId = ScmSystem.Task.startTransferTask(wsA, condition, ScopeType.SCOPE_CURRENT, targetSite1.getSiteName());

				ScmTaskUtils.waitTaskFinish(sessionA, taskId);
				// check task info
				ScmTask taskInfo = ScmSystem.Task.getTask(sessionA, taskId);

				// check results
				Assert.assertEquals(taskInfo.getWorkspaceName(), wsName);
				Assert.assertEquals(taskInfo.getType(), CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE);

				SiteWrapper[] expSiteList = { sourceSite1, targetSite1 };
				ScmFileUtils.checkMetaAndData(ws_T,fileIdList.subList(0, fileNum / 2), expSiteList, localPath,
						filePath);
			} finally {
				if (sessionA != null) {
					sessionA.close();
				}
			}
		}
	}

	private class AsyncTransfer extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession sessionA = null;
			ScmWorkspace wsA = null;
			String wsName = ws_T.getName();
			try {
				sessionA = TestScmTools.createSession(sourceSite2);
				wsA = ScmFactory.Workspace.getWorkspace(wsName, sessionA);

				for (int i = fileNum / 2; i < fileNum; i++) {
					ScmFactory.File.asyncTransfer(wsA, fileIdList.get(i));
					// check results
					SiteWrapper[] expSiteList = { sourceSite2, targetSite2 };
					ScmTaskUtils.waitAsyncTaskFinished(wsA, fileIdList.get(i), expSiteList.length);
					ScmFileUtils.checkMetaAndData(ws_T,fileIdList.get(i), expSiteList, localPath, filePath);
				}
			} finally {
				if (sessionA != null) {
					sessionA.close();
				}
			}
		}
	}

	private void writeFile() throws ScmException {
		for (int i = 0; i < fileNum / 2; i++) {
			ScmFile scmfile = ScmFactory.File.createInstance(wsA);
			scmfile.setContent(filePath);
			scmfile.setFileName(author1+"_"+UUID.randomUUID());
			scmfile.setAuthor(author1);
			fileIdList.add(scmfile.save());
		}

		for (int i = fileNum / 2; i < fileNum; i++) {
			ScmFile scmfile = ScmFactory.File.createInstance(wsB);
			scmfile.setContent(filePath);
			scmfile.setFileName(author2+"_"+UUID.randomUUID());
			scmfile.setAuthor(author2);
			fileIdList.add(scmfile.save());
		}
	}

}
