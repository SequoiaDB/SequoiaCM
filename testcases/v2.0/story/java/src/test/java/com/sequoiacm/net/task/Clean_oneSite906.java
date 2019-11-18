/**
 * 
 */
package com.sequoiacm.net.task;

import java.io.File;
import java.util.ArrayList;
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
import com.sequoiacm.exception.ScmError;
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
 * @Description:1、分中心A写文件； 2、分中心B读取文件； 3、分中心A清理文件； 4、检查所有中心文件正确性；
 * @author fanyu
 * @Date:2017年9月18日
 * @version:1.0
 */

public class Clean_oneSite906 extends TestScmBase {
	private boolean runSuccess = false;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionB = null;
	private ScmWorkspace wsB = null;

	private ScmId taskId = null;
	private String authorName = "CleanOneSite906";
	private int fileSize = 100;
	private int fileNum = 10;
	private List<ScmId> fileIdList = new ArrayList<>();

	private File localPath = null;
	private String filePath = null;
	
	private List<SiteWrapper> siteList = new ArrayList<SiteWrapper>();
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

			ws_T = ScmInfo.getWs();
			siteList= ScmNetUtils.getCleanSites(ws_T);
			
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(ws_T,cond);
			
			sessionA = TestScmTools.createSession(siteList.get(0));
			wsA = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);

			sessionB = TestScmTools.createSession(siteList.get(1));
			wsB = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionB);

			// ready scmfile
			this.writeFile(wsA, authorName, fileIdList);
			this.readScmFile(wsB, fileIdList);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "fourSite" },enabled=false)//bug:315
	private void test() {
		try {
			this.startCleanTask(sessionA, authorName);
			ScmTaskUtils.waitTaskFinish(sessionA, taskId);
			ScmTask taskInfo = ScmSystem.Task.getTask(sessionA, taskId);
			Assert.assertEquals(taskInfo.getProgress(), 100);
			this.checkResults();
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
					ScmFactory.File.getInstance(wsA, fileId).delete(true);
				}
				TestTools.LocalFile.removeFile(localPath);
				TestSdbTools.Task.deleteMeta(taskId);
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

	private void writeFile(ScmWorkspace ws, String author, List<ScmId> fileIdList) throws ScmException {
		for (int i = 0; i < fileNum; i++) {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(authorName+"_"+UUID.randomUUID());
			file.setAuthor(authorName);
			file.setContent(filePath);
			ScmId fileId = file.save();
			fileIdList.add(fileId);
		}
	}

	private void readScmFile(ScmWorkspace ws, List<ScmId> fileIdList) throws Exception {
		for (int i = 0; i < fileIdList.size(); i++) {
			ScmId fileId = fileIdList.get(i);
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			file.getContent(downloadPath);
		}
	}

	private void startCleanTask(ScmSession ss, String fileName) throws Exception {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), ss);
		BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		taskId = ScmSystem.Task.startCleanTask(ws, condition);
	}

	private void checkResults() throws Exception {
		SiteWrapper rootSite = ScmInfo.getRootSite();
		// file exists only in one site
		SiteWrapper[] siteArr = { rootSite, siteList.get(1) };
		ScmFileUtils.checkMetaAndData(ws_T,fileIdList, siteArr, localPath, filePath);

		// check site that does not have any data
		checkFreeSite();
	}

	private void checkFreeSite() throws Exception {
		int randNum = 0;
		try {
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
			randNum = new Random().nextInt(fileNum);
			ScmFileUtils.checkData(ws, fileIdList.get(randNum), localPath, filePath);
			Assert.assertFalse(true, "expect result is fail but actual is success.");
		} catch (ScmException e) {
			if (ScmError.DATA_NOT_EXIST != e.getError()) {
				e.printStackTrace();
				throw e;
			}
		}
	}
}
