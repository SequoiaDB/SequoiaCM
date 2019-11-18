package com.sequoiacm.task;

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
 * @Testcase: SCM-1028:分A跨中心读分B的文件，清理任务清理该文件，再次读取文件
 * @author fanyu init
 * @date 2018.01.02
 */

public class Clean_readAfterClean1028 extends TestScmBase {
	private boolean runSuccess = false;

	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private String authorName = "ReadAfterClean1028";
	private int fileSize = 1024 * 1024;
	private int fileNum = 50;
	private File localPath = null;
	private String filePath = null;
	private ScmId taskId = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	
	private SiteWrapper rootSite = null;
	private List<SiteWrapper> branceSites = null;
	private WsWrapper ws_T = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			// ready file
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
			
			rootSite = ScmInfo.getRootSite();
			branceSites = ScmInfo.getBranchSites(2);
			ws_T = ScmInfo.getWs();
			
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(ws_T,cond);

			session = TestScmTools.createSession(branceSites.get(0));
			ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);

			writeFileFromSubCenterA();
			readFileFromSubCenter(branceSites.get(1));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "fourSite" })
	private void test() {
		try {
			StartTaskFromSubCenterA(branceSites.get(1));
			ScmTaskUtils.waitTaskFinish(session, taskId);
			readFileFromSubCenter(branceSites.get(1));
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
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(ws, fileId, true);
				}
				TestTools.LocalFile.removeFile(localPath);
				TestSdbTools.Task.deleteMeta(taskId);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	private void StartTaskFromSubCenterA(SiteWrapper branceSite) throws ScmException {
		ScmSession session = null;
		try {
			// login
			session = TestScmTools.createSession(branceSite);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);

			// start task
			BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName)
					.get();
			taskId = ScmSystem.Task.startCleanTask(ws, condition);
		} finally {
			if (session != null)
				session.close();
		}
	}

	private void writeFileFromSubCenterA() throws ScmException {
		for (int i = 0; i < fileNum; i++) {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setContent(filePath);
			file.setFileName(authorName+"_"+UUID.randomUUID());
			file.setAuthor(authorName);
			ScmId fileId = file.save();
			fileIdList.add(fileId);
		}
	}

	private void readFileFromSubCenter(SiteWrapper site) throws Exception {
		ScmSession session = null;
		try {
			// login
			session = TestScmTools.createSession(site);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);

			for (int i = 0; i < fileNum; i++) {
				ScmId fileId = fileIdList.get(i);
				String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());
				ScmFile file = ScmFactory.File.getInstance(ws, fileId);
				file.getContent(downloadPath);
			}
		} finally {
			if (session != null)
				session.close();
		}
	}

	private void checkMetaAndLobs() throws Exception {
		SiteWrapper[] expSiteList = { rootSite, branceSites.get(0), branceSites.get(1) };
		for (int i = 0; i < fileIdList.size(); i++) {
			ScmFileUtils.checkMetaAndData(ws_T, fileIdList.get(i), expSiteList, localPath, filePath);
		}
	}
}