package com.sequoiacm.net.task;

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
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-742:从分中心A迁移文件到主中心后，清理分中心A已被迁移的文件
 * @author huangxiaoni init
 * @date 2017.8.14
 */

public class TransferAndClean742 extends TestScmBase {
	private boolean runSuccess = false;

	private ScmSession sessionM = null;
	private ScmWorkspace wsM = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;

	private String author = "TD742";
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private int fileSize = 10;
	private int fileNum = 100;
	private File localPath = null;
	private String filePath = null;
	private List<ScmId> taskIdList = new ArrayList<ScmId>();
	
	private SiteWrapper sourceSite = null;
	private SiteWrapper targetSite = null;
	//private NodeWrapper node = null;
	private WsWrapper ws_T = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
			
			ws_T = ScmInfo.getWs();
			List<SiteWrapper> siteList = ScmNetUtils.getCleanSites(ws_T);
			sourceSite = siteList.get(0);
			targetSite = siteList.get(1);

			// login
			sessionM = TestScmTools.createSession(targetSite);
			wsM = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);

			sessionA = TestScmTools.createSession(sourceSite);
			wsA =  ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() {
		try {
			this.writeFileFromA();
			this.startTransferTaskFromA();
			this.startCleanTaskFromA();
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
					ScmFactory.File.deleteInstance(wsM, fileId, true);
				}
				for (ScmId taskId : taskIdList) {
					TestSdbTools.Task.deleteMeta(taskId);
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

	private void writeFileFromA() throws ScmException {
		for (int i = 0; i < fileNum; i++) {
			ScmFile scmfile = ScmFactory.File.createInstance(wsA);
			scmfile.setContent(filePath);
			scmfile.setFileName(author+"_"+UUID.randomUUID());
			scmfile.setAuthor(author);
			fileIdList.add(scmfile.save());
		}
	}

	private void startTransferTaskFromA() throws Exception {
		BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
		ScmId taskId = ScmSystem.Task.startTransferTask(wsA, condition, ScopeType.SCOPE_CURRENT, targetSite.getSiteName());
		taskIdList.add(taskId);
		ScmTaskUtils.waitTaskFinish(sessionA, taskId);

		// check task info
		ScmTask taskInfo = ScmSystem.Task.getTask(sessionA, taskId);
		// check results
		Assert.assertEquals(taskInfo.getWorkspaceName(), ws_T.getName());
		Assert.assertEquals(taskInfo.getType(), CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE);

		SiteWrapper[] expSiteList = { sourceSite, targetSite };
		ScmFileUtils.checkMetaAndData(ws_T, fileIdList, expSiteList, localPath, filePath);
	}

	private void startCleanTaskFromA() throws Exception {
		BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
		ScmId taskId = ScmSystem.Task.startCleanTask(wsA, condition);
		taskIdList.add(taskId);
		ScmTaskUtils.waitTaskFinish(sessionA, taskId);
		// check task info
		ScmTask taskInfo = ScmSystem.Task.getTask(sessionA, taskId);
		Assert.assertEquals(taskInfo.getWorkspaceName(), ws_T.getName());
		Assert.assertEquals(taskInfo.getType(), CommonDefine.TaskType.SCM_TASK_CLEAN_FILE);

		// check results
		SiteWrapper[] expSiteList = { targetSite };
		ScmFileUtils.checkMetaAndData(ws_T,fileIdList, expSiteList, localPath, filePath);
	}
}
