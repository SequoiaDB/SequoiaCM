package com.sequoiacm.asynctask.concurrent;

import java.io.File;
import java.io.IOException;
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
 * @FileName SCM-495: 并发在分中心迁移文件、主中心读取文件
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * 1、并发在分中心A异步迁移单个文件、在主中心读取文件； 2、检查执行返回结果； 3、后台异步迁移任务执行完成后检查迁移后的文件正确性；
 */
public class AsynctransferAndReadFile495 extends TestScmBase {
	private boolean runSuccess = false;
	private int fileSize = new Random().nextInt(1024) + 1024 * 1024;
	private File localPath = null;
	private String filePath = null;
	private ScmId fileId = null;
	private String fileName = "AsynctransferAndReadFile495";
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	
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
			ScmFileUtils.cleanFile(ws_T,cond);

			// login in
			sessionA = TestScmTools.createSession(branceSite);
			wsA = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
			writeFileFromSubCenterB();
		} catch (ScmException | IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			AsyncTransferFromSubCenterB asyncTransferBT = new AsyncTransferFromSubCenterB();
			ReadFileFromMainCenter readFileMT = new ReadFileFromMainCenter();
			asyncTransferBT.start(15);
			readFileMT.start(15);
			if (!(asyncTransferBT.isSuccess() && readFileMT.isSuccess())) {
				Assert.fail(asyncTransferBT.getErrorMsg() + readFileMT.getErrorMsg());
			}
			checkResult();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || forceClear) {
				ScmFactory.File.deleteInstance(wsA, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (BaseException | ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}

		}
	}

	private void writeFileFromSubCenterB() {
		try {
			ScmFile scmfile = ScmFactory.File.createInstance(wsA);
			scmfile.setContent(filePath);
			scmfile.setFileName(fileName+"_"+UUID.randomUUID());
			fileId = scmfile.save();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	private class AsyncTransferFromSubCenterB extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession sessionA = null;
			try {
				sessionA = TestScmTools.createSession(branceSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(),sessionA);
				// AsyncTransfer
				ScmFactory.File.asyncTransfer(ws, fileId);
			} catch (ScmException e) {
				throw e;
			} finally {
				if (sessionA != null) {
					sessionA.close();
				}
			}
		}
	}

	private class ReadFileFromMainCenter extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession sessionM = null;
			try {
				// login
				sessionM = TestScmTools.createSession(rootSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);
				ScmFile file = ScmFactory.File.getInstance(ws, fileId);
				String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());
				file.getContent(downloadPath);
			} catch (ScmException e) {
				throw e;
			} finally {
				if (sessionM != null) {
					sessionM.close();
				}
			}
		}
	}

	private void checkResult() {
		try {
			SiteWrapper[] expSiteList = { rootSite, branceSite };
			ScmTaskUtils.waitAsyncTaskFinished(wsA, fileId, expSiteList.length);
			ScmFileUtils.checkMetaAndData(ws_T,fileId, expSiteList, localPath, filePath);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
