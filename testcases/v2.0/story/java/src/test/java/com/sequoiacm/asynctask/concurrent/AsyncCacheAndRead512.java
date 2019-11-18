package com.sequoiacm.asynctask.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-512: 并发在分中心缓存文件、文件流方式读取文件
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * 1、并发在分中心A异步缓存单个文件、文件流方式读取文件； 2、检查执行返回结果； 3、后台异步缓存任务执行完成后检查缓存后的文件正确性；；
 */
public class AsyncCacheAndRead512 extends TestScmBase {
	private boolean runSuccess = false;

	private int fileSize = 1024 * new Random().nextInt(1024 + 1);
	private File localPath = null;
	private String filePath = null;
	private ScmId fileId = null;
	private String fileName = "AsyncCacheAndRead512";

	private ScmSession sessionM = null;
	private ScmWorkspace ws = null;
	
	private SiteWrapper rootSite = null;
	private SiteWrapper branceSite = null;
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
			branceSite= ScmInfo.getBranchSite();
			ws_T = ScmInfo.getWs();

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
			ScmFileUtils.cleanFile(ws_T, cond);
			
			// login in
			sessionM = TestScmTools.createSession(rootSite);
			ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);
			writeFileFromMainCenter();
		} catch (ScmException | IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() {
		CacheFromSubCenterA cacheT = new CacheFromSubCenterA();
		ReadFile readA = new ReadFile();
		cacheT.start(20);
		readA.start(20);
		if (!(cacheT.isSuccess() && readA.isSuccess())) {
			Assert.fail(cacheT.getErrorMsg() + readA.getErrorMsg());
		}
		
		checkResult();
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (BaseException | ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionM != null) {
				sessionM.close();
			}

		}
	}

	private void writeFileFromMainCenter() {
		try {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setContent(filePath);
			scmfile.setFileName(fileName+"_"+UUID.randomUUID());
			fileId = scmfile.save();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	private class CacheFromSubCenterA extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession sessionA = null;
			try {
				// login
				sessionA = TestScmTools.createSession(branceSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
				// cache
				ScmFactory.File.asyncCache(ws, fileId);
			} catch (ScmException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (sessionA != null) {
					sessionA.close();
				}
			}
		}
	}

	private class ReadFile extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession sessionA = null;
			OutputStream fos = null;
			ScmInputStream sis = null;
			try {
				// login
				sessionA = TestScmTools.createSession(branceSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
				ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
				String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());
				fos = new FileOutputStream(new File(downloadPath));
				sis = ScmFactory.File.createInputStream(scmfile);
				sis.read(fos);
			} catch (ScmException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (sessionA != null) {
					sessionA.close();
				}
				if (fos != null) {
					fos.close();
				}
				if (sis != null) {
					sis.close();
				}
			}
		}
	}

	private void checkResult() {
		try {
			SiteWrapper[] expSiteList = { rootSite, branceSite };
			ScmTaskUtils.waitAsyncTaskFinished(ws, fileId, expSiteList.length);
			ScmFileUtils.checkMetaAndData(ws_T,fileId, expSiteList, localPath, filePath);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} 
	}
}
