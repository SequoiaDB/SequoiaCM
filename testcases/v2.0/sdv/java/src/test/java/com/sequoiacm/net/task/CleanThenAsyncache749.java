package com.sequoiacm.net.task;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @FileName SCM-749 : 清理分中心A的文件，单个异步缓存该文件到分中心A并读取该文件
 * @Author linsuqiang
 * @Date 2017-08-08
 * @Version 1.00
 */

/*
 * 1、清理分中心A的文件； 2、单个异步缓存该文件到分中心A； 3、检查执行结果正确性；
 */

public class CleanThenAsyncache749 extends TestScmBase {
	private boolean runSuccess = false;

	private final int fileSize = 1 * 1024 * 1024;
	private final int fileNum = 1;
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private final String author = "clean749";

	private File localPath = null;
	private List<String> filePathList = new ArrayList<String>();
	private String filePath = null;
	private ScmSession sessionM = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsM = null;
	private ScmId taskId = null;
	
	private SiteWrapper sourceSite = null;
	private SiteWrapper targetSite = null;
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

			for (int i = 0; i < fileNum; ++i) {
				filePathList.add(filePath);
			}
			
			ws_T = ScmInfo.getWs();
			List<SiteWrapper> siteList = ScmNetUtils.getCleanSites(ws_T);
			sourceSite = siteList.get(0);
			targetSite = siteList.get(1);
			
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(ws_T,cond);

			sessionA = TestScmTools.createSession(sourceSite);
			sessionM = TestScmTools.createSession(targetSite);
			wsM = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);
			prepareFiles(sessionA);
		} catch (Exception e) {
			if (sessionA != null) {
				sessionA.close();
			}
			if (sessionM != null) {
				sessionM.close();
			}
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			readAllFile(sessionM);
			taskId = cleanAllFile(sessionA);
			asyncCacheFile(sessionA);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);
				for (int i = 0; i < fileNum; ++i) {
					ScmFactory.File.getInstance(ws, fileIdList.get(i)).delete(true);
				}
				TestSdbTools.Task.deleteMeta(taskId);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (BaseException | ScmException e) {
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

	private void prepareFiles(ScmSession session) throws Exception {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
		for (int i = 0; i < fileNum; ++i) {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setFileName(author+"_"+UUID.randomUUID());
			scmfile.setAuthor(author);
			scmfile.setContent(filePathList.get(i));
			fileIdList.add(scmfile.save());
		}
	}

	private void readAllFile(ScmSession session) throws Exception {
		OutputStream fos = null;
		ScmInputStream sis = null;
		try {
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
			for (ScmId fileId : fileIdList) {
				ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
				String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());
				fos = new FileOutputStream(new File(downloadPath));
				sis = ScmFactory.File.createInputStream(scmfile);
				sis.read(fos);
			}
		} finally {
			if (fos != null) {
				fos.close();
			}
			if (sis != null) {
				sis.close();
			}
		}
	}

	private ScmId cleanAllFile(ScmSession session) throws Exception {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
		BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
		ScmId taskId = ScmSystem.Task.startCleanTask(ws, condition);
		ScmTaskUtils.waitTaskFinish(session, taskId);
	    SiteWrapper[] expSiteArr = { targetSite };
		ScmFileUtils.checkMetaAndData(ws_T,fileIdList, expSiteArr, localPath, filePath);
		return taskId;
	}

	private void asyncCacheFile(ScmSession session) throws Exception {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
		for (ScmId fileId : fileIdList) {
			ScmFactory.File.asyncCache(ws, fileId);
			SiteWrapper[] expSiteArr1 = { sourceSite, targetSite };
			ScmTaskUtils.waitAsyncTaskFinished(wsM, fileId, expSiteArr1.length);
			ScmFileUtils.checkMetaAndData(ws_T,fileId, expSiteArr1, localPath, filePath);
		}
	}
}