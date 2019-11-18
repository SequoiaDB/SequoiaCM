package com.sequoiacm.net.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
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
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.exception.ScmError;
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
 * @Testcase: SCM-406:迁移分中心部分文件
 * @author huangxiaoni init linsuqiang modified
 * @date 2017.8.8
 */

/*
 * 1、在分中心A写多个文件； 2、在分中心A开始迁移任务，指定迁移条件匹配分中心部分文件 3、检查迁移任务执行结果；
 */

public class Transfer_matchPartFile406 extends TestScmBase {
	private boolean runSuccess = false;

	private ScmSession sessionM = null; // mainCenter
	private ScmWorkspace wsM = null;
	private ScmSession sessionA = null; // subCenterA
	private ScmWorkspace  wsA = null;
	private ScmId taskId = null;
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private String authorName = "transfer406";
	private int fileSize = 100;
	private int fileNum = 100;
	private int startNum = 2;
	private File localPath = null;
	private List<String> filePathList = new ArrayList<String>();
	private BSONObject condition = null;
	
	private SiteWrapper rootSite = null;
	private List<SiteWrapper> siteList = new ArrayList<SiteWrapper>();
	private SiteWrapper branceSite = null;
	private WsWrapper ws_T = null;

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
			
			rootSite = ScmInfo.getRootSite();
			if (ScmInfo.getSiteNum() <= 2 && ScmInfo.getSiteNum() > 0) {
				siteList = ScmInfo.getBranchSites(1);
			} else {
				siteList = ScmInfo.getBranchSites(2);
			}
			branceSite = siteList.get(0);
			ws_T = ScmInfo.getWs();
			
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(ws_T, cond);

			// login
			sessionM = TestScmTools.createSession(rootSite);
			wsM = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);
			sessionA =TestScmTools.createSession(branceSite);
			wsA = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" },enabled=false)
	private void testTransfer() {
		try {
			writeFileFromSubCenterA();
			List<List<ScmFileLocation>> befLocLists = getLocationLists(fileIdList);
			startTaskFromSubCenterA();

			ScmTaskUtils.waitTaskFinish(sessionA, taskId);

			List<List<ScmFileLocation>> aftLocLists = getLocationLists(fileIdList);
			checkLocationLists(befLocLists, aftLocLists);
			checkTaskAtt(sessionA);
			checkMetaAndLobs();
			readFileFromMainCenter(sessionM);
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
					ScmFactory.File.getInstance(wsM, fileId).delete(true);
				}
				TestTools.LocalFile.removeFile(localPath);
				TestSdbTools.Task.deleteMeta(taskId);
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
		}
	}

	private void writeFileFromSubCenterA() throws ScmException {
		for (int i = 0; i < fileNum; i++) {
			ScmFile file = ScmFactory.File.createInstance(wsA);
			file.setContent(filePathList.get(i));
			file.setFileName(authorName+"_"+UUID.randomUUID());
			file.setAuthor(authorName);
			ScmId fileId = file.save();
			fileIdList.add(fileId);
		}
	}

	private void startTaskFromSubCenterA() throws ScmException, InterruptedException {
		int value = fileSize + startNum;
		condition = ScmQueryBuilder.start().put(ScmAttributeName.File.SIZE).greaterThanEquals(value)
				.put(ScmAttributeName.File.AUTHOR).is(authorName).get();
		taskId = ScmSystem.Task.startTransferTask(wsA, condition, ScopeType.SCOPE_ALL, rootSite.getSiteName());
	}

	private void checkTaskAtt(ScmSession session) throws ScmException {
		// check task info
		ScmTask taskInfo = ScmSystem.Task.getTask(session, taskId);
		Assert.assertEquals(taskInfo.getProgress(), 100);
		Assert.assertEquals(taskInfo.getRunningFlag(), CommonDefine.TaskRunningFlag.SCM_TASK_FINISH);
		Assert.assertEquals(taskInfo.getType(), CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE);
		Assert.assertEquals(taskInfo.getWorkspaceName(), wsA.getName());
		Assert.assertEquals(taskInfo.getContent(), condition);
		Assert.assertNotNull(taskInfo.getId());
		Assert.assertNotNull(taskInfo.getServerId());
		Assert.assertNotNull(taskInfo.getStartTime());
		Assert.assertNotNull(taskInfo.getStopTime());
	}

	private void checkMetaAndLobs() {
		try {
			for (int i = 0; i < fileNum; i++) {
				ScmId fileId = fileIdList.get(i);
				String filePath = filePathList.get(i);
				if (i >= startNum) {
				    SiteWrapper[] expSiteList = { rootSite, branceSite };
					ScmFileUtils.checkMetaAndData(ws_T,fileId, expSiteList, localPath, filePath);
				} else {
					SiteWrapper[] expSiteList = { branceSite };
					ScmFileUtils.checkMetaAndData(ws_T,fileId, expSiteList, localPath, filePath);
				}
			}
			// check site which does not have any data
			checkFreeSite();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage()+ " branceNode INFO " + branceSite.toString());
		}
	}

	private void readFileFromMainCenter(ScmSession ss) throws Exception {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), ss);
		for (int i = startNum; i < fileNum; i++) {
			ScmId fileId = fileIdList.get(i);
			String filePath = filePathList.get(i);

			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());

			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			file.getContent(downloadPath);

			// check content
			Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
		}
	}

	private void checkLocationLists(List<List<ScmFileLocation>> befLocLists, List<List<ScmFileLocation>> aftLocLists)
			throws Exception {
		Assert.assertEquals(befLocLists.size(), aftLocLists.size(), "file count is different!");
		for (int i = 0; i < startNum; ++i) {
			checkLastAccessTime(befLocLists.get(i), aftLocLists.get(i), false);
		}
		for (int i = startNum; i < befLocLists.size(); ++i) {
			checkLastAccessTime(befLocLists.get(i), aftLocLists.get(i), true);
		}
	}

	private void checkLastAccessTime(List<ScmFileLocation> befLocList, List<ScmFileLocation> aftLocList,
			boolean isTransfered) throws Exception {

		Assert.assertEquals(aftLocList.size(), isTransfered ? 2 : 1, "site count wrong after clean");

		Date befDate = getLastAccessTime(befLocList, branceSite.getSiteId());
		Date aftDate = getLastAccessTime(aftLocList, branceSite.getSiteId());

		Assert.assertEquals(aftDate.getTime(), befDate.getTime(), 
				"last access time is changed accidentally:"+"befDate = " + befDate + "  aftDate = "+aftDate);
	}

	private Date getLastAccessTime(List<ScmFileLocation> locList, int siteId) throws Exception {
		ScmFileLocation matchLoc = null;
		for (ScmFileLocation loc : locList) {
			if (loc.getSiteId() == siteId) {
				matchLoc = loc;
				break;
			}
		}
		if (null == matchLoc) {
			throw new Exception("no such site id on the location list");
		}
		return matchLoc.getDate();
	}

	private List<List<ScmFileLocation>> getLocationLists(List<ScmId> fileIdList) throws ScmException {
		ScmSession ss = null;
		try {
			List<List<ScmFileLocation>> locationLists = new ArrayList<>();
			ss = TestScmTools.createSession(rootSite);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), ss);
			for (ScmId fileId : fileIdList) {
				ScmFile file = ScmFactory.File.getInstance(ws, fileId);
				List<ScmFileLocation> locationList = file.getLocationList();
				locationLists.add(locationList);
			}
			return locationLists;
		} finally {
			if (null != ss) {
				ss.close();
			}
		}
	}

	private void checkFreeSite() throws Exception {
		ScmSession ss = null;
		int randNum = 0;
		try {
			if (siteList.size() == 2) {
				ss = TestScmTools.createSession(siteList.get(1));
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), ss);
				randNum = new Random().nextInt(fileNum);
				ScmFileUtils.checkData(ws, fileIdList.get(randNum), localPath, filePathList.get(randNum));
				Assert.assertFalse(true, "expect result is fail but actual is success.");
			}
		} catch (ScmException e) {
			if (ScmError.DATA_NOT_EXIST != e.getError()) {
				e.printStackTrace();
				throw e;
			}
		} finally {
			if (ss != null) {
				ss.close();
			}
		}
	}
}