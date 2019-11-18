package com.sequoiacm.net.task.concurrent;

import java.io.File;
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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Testcase: SCM-744:并发迁移任务、单个异步迁移相同文件
 * @author huangxiaoni init
 * @date 2017.8.14
 */

public class TransferTaskAndAsyncCacheSameFile2277 extends TestScmBase {
	private static final int defaultTimeOut = 5 * 60; // 5min
	private boolean runSuccess = false;

	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;

	private String author = "TD2277";
	private ScmId fileId = null;
	private int fileSize = 1024*1024*50;
	private File localPath = null;
	private String filePath = null;
	private ScmId taskId = null;
	
	private SiteWrapper rootSite = null;
	private SiteWrapper branceSite = null;
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
			List<SiteWrapper> siteList = ScmNetUtils.getSortSites(ws_T);
			rootSite = siteList.get(0);
			branceSite = siteList.get(1); 

			BSONObject cond = ScmQueryBuilder.start().put(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(ws_T,cond);

			// login
			sessionA = TestScmTools.createSession(rootSite);
			wsA = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);

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
			asyncCache();
			startTransferTask();
			// check results
			SiteWrapper[] expSiteList = { rootSite, branceSite };
			ScmFileUtils.checkMetaAndData(ws_T, fileId, expSiteList, localPath, filePath);
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
				ScmFactory.File.getInstance(wsA, fileId).delete(true);
				TestSdbTools.Task.deleteMeta(taskId);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}

		}
	}

	private void  startTransferTask() throws Exception{
		ScmSession sessionA = null;
		ScmWorkspace wsA = null;
		String wsName = ws_T.getName();
		try {
			sessionA = TestScmTools.createSession(rootSite);
			wsA = ScmFactory.Workspace.getWorkspace(wsName, sessionA);

			BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			taskId = ScmSystem.Task.startTransferTask(wsA, condition, ScopeType.SCOPE_CURRENT, branceSite.getSiteName());
			waitTaskFinish(sessionA, taskId);

			// check task info
			ScmTask taskInfo = ScmSystem.Task.getTask(sessionA, taskId);
			// check results
			Assert.assertEquals(taskInfo.getWorkspaceName(), wsName);
			Assert.assertEquals(taskInfo.getType(), CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE);
			
			SiteWrapper[] expSiteList = { rootSite, branceSite };
			ScmFileUtils.checkMetaAndData(ws_T, fileId, expSiteList, localPath, filePath);
		}catch(ScmException e){
			e.printStackTrace();
			Assert.fail(e.getMessage());
			
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
		}
	}

	private void asyncCache() throws Exception {
		ScmSession sessionA = null;
		ScmWorkspace wsA = null;
		String wsName = ws_T.getName();
		try {
			sessionA = TestScmTools.createSession(branceSite);
			wsA = ScmFactory.Workspace.getWorkspace(wsName, sessionA);
			ScmFactory.File.asyncCache(wsA, fileId);
			SiteWrapper[] expSiteList = { rootSite, branceSite };
			ScmTaskUtils.waitAsyncTaskFinished(wsA, fileId, expSiteList.length);
			ScmFileUtils.checkMetaAndData(ws_T, fileId, expSiteList, localPath, filePath);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());

		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
		}
	}

	private void writeFile() throws ScmException {
		ScmFile scmfile = ScmFactory.File.createInstance(wsA);
		scmfile.setContent(filePath);
		scmfile.setFileName(author + "_" + UUID.randomUUID());
		scmfile.setAuthor(author);
		fileId = scmfile.save();
	}
	
	private void waitTaskFinish(ScmSession session, ScmId taskId) throws Exception {
		int sleepTime = 200; // millisecond
		int maxRetryTimes = (defaultTimeOut * 1000) / sleepTime;
		int retryTimes = 0;
		while (true) {
			ScmTask task = ScmSystem.Task.getTask(session, taskId);
			if (CommonDefine.TaskRunningFlag.SCM_TASK_FINISH == task.getRunningFlag()) {
				break;
			}
			else if (CommonDefine.TaskRunningFlag.SCM_TASK_ABORT == task.getRunningFlag()) {
				throw new Exception("failed, the task running flag is abort, task info : \n" + task.toString());
			} 
			else if (CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL == task.getRunningFlag()) {
				throw new Exception("failed, the task running flag is cancel, task info : \n" + task.toString());
			} 
			else if (retryTimes >= maxRetryTimes) {
				throw new Exception("failed to wait task finished, maxRetryTimes=" + maxRetryTimes
						+ ", task info : \n" + task.toString());
			}
			Thread.sleep(sleepTime);
			retryTimes++;
		}
	}
}
