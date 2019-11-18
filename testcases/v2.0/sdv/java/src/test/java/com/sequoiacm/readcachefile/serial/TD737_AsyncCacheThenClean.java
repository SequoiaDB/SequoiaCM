package com.sequoiacm.readcachefile.serial;

import java.io.File;
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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Testcase: SCM-737:异步缓存后读取文件，读取后清理（写、读、缓存、清理，流程测试）
 * @author huangxiaoni init
 * @date 2017.8.14
 */

public class TD737_AsyncCacheThenClean extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper rootSite = null;
	private List<SiteWrapper> branSites = null;
	private final int branSitesNum = 2;
	private WsWrapper wsp = null;

	private ScmSession sessionM = null;
	private ScmWorkspace wsM = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionB = null;
	private ScmWorkspace wsB = null;

	private String author = "TD737";
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private int fileSize = 200 * 1024;
	private int fileNum = 20;
	private File localPath = null;
	private String filePath = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);

			rootSite = ScmInfo.getRootSite();
			branSites = ScmInfo.getBranchSites(branSitesNum);
			wsp = ScmInfo.getWs();

			sessionM = TestScmTools.createSession(rootSite);
			wsM = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionM);

			sessionA = TestScmTools.createSession(branSites.get(0));
			wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);

			sessionB = TestScmTools.createSession(branSites.get(1));
			wsB = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionB);

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(wsp, cond);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "fourSite" })
	private void test() throws Exception {
		try {
			this.writeFileFromA();

			this.readFileFromM();

			this.asyncCacheFromB();
			this.readFileFromB();

			this.startCleanTaskFromB();

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
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(wsA, fileId, true);
				}
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionM != null) {
				sessionM.close();
			}
			if (sessionA != null) {
				sessionA.close();
			}
			if (sessionB != null) {
				sessionB.close();
			}

		}
	}

	private void writeFileFromA() throws ScmException {
		for (int i = 0; i < fileNum; i++) {
			ScmFile file = ScmFactory.File.createInstance(wsA);
			file.setContent(filePath);
			file.setFileName(author+i+UUID.randomUUID());
			file.setAuthor(author);
			fileIdList.add(file.save());
		}
	}

	private void readFileFromM() throws Exception {
		for (int i = 0; i < fileNum; i++) {
			ScmId fileId = fileIdList.get(i);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			ScmFile file = ScmFactory.File.getInstance(wsM, fileId);
			file.getContent(downloadPath);
		}

		// check results
		SiteWrapper[] expSites = { rootSite, branSites.get(0) };
		ScmFileUtils.checkMetaAndData(wsp, fileIdList, expSites, localPath, filePath);
	}

	private void asyncCacheFromB() throws Exception {
		for (int i = 0; i < fileNum; i++) {
			ScmFactory.File.asyncCache(wsB, fileIdList.get(i));
		}

		for (int i = 0; i < fileNum; i++) {
			ScmTaskUtils.waitAsyncTaskFinished(wsB, fileIdList.get(i), 3);
		}

		SiteWrapper[] expSites = { rootSite, branSites.get(0), branSites.get(1) };
		ScmFileUtils.checkMetaAndData(wsp, fileIdList, expSites, localPath, filePath);
	}

	private void readFileFromB() throws Exception {
		for (int i = 0; i < fileNum; i++) {
			ScmId fileId = fileIdList.get(i);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			ScmFile file = ScmFactory.File.getInstance(wsB, fileId);
			file.getContent(downloadPath);

			// check results
			SiteWrapper[] expSites = { rootSite, branSites.get(0), branSites.get(1) };
			ScmFileUtils.checkMetaAndData(wsp, fileIdList, expSites, localPath, filePath);
		}
	}

	private void startCleanTaskFromB() throws Exception {
		BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
		ScmId taskId = ScmSystem.Task.startCleanTask(wsB, condition);

		// check task info
		ScmTask taskInfo = null;
		while (true) {
			taskInfo = ScmSystem.Task.getTask(sessionB, taskId);
			if (taskInfo.getRunningFlag() == CommonDefine.TaskRunningFlag.SCM_TASK_FINISH) {
				break;
			}
			Thread.sleep(200);
		}
		Assert.assertEquals(taskInfo.getWorkspaceName(), wsp.getName());
		Assert.assertEquals(taskInfo.getType(), CommonDefine.TaskType.SCM_TASK_CLEAN_FILE);

		// check results
		SiteWrapper[] expSites = { rootSite, branSites.get(0) };
		ScmFileUtils.checkMetaAndData(wsp, fileIdList, expSites, localPath, filePath);
	}

}
