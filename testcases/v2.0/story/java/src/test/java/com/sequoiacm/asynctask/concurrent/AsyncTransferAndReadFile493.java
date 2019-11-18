package com.sequoiacm.asynctask.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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
 * @FileName SCM-493: 并发在分中心迁移文件、文件流方式读取文件
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、并发在分中心A异步迁移单个文件、文件流方式读取文件； 2、检查执行返回结果； 3、后台异步迁移任务执行完成后检查迁移后的文件正确性；
 */

public class AsyncTransferAndReadFile493 extends TestScmBase {
	private boolean runSuccess = false;

	private final int fileSize = 10 * 1024 * 1024;
	private ScmId fileId = null;

	private File localPath = null;
	private String filePath = null;
	private String fileName = "AsyncTransferAndReadFile493";
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	
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

			branceSite = ScmInfo.getBranchSite();
			ws_T = ScmInfo.getWs();
			
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
			ScmFileUtils.cleanFile(ws_T,cond);
			
			// login in
			sessionA = TestScmTools.createSession(branceSite);
			wsA = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
			
			prepareFiles();
		} catch (Exception e) {
			if (sessionA != null) {
				sessionA.close();
			}
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			TransferThread transferThd = new TransferThread();
			transferThd.start(15);

			ReadThread readThd = new ReadThread();
			readThd.start(15);

			if (!(transferThd.isSuccess() && readThd.isSuccess())) {
				Assert.fail(transferThd.getErrorMsg() + readThd.getErrorMsg());
			}
			checkResult();
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

	private class TransferThread extends TestThreadBase {

		@Override
		public void exec() throws Exception {
			ScmSession sessionA = null;
			try {
				sessionA = TestScmTools.createSession(branceSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
				ScmFactory.File.asyncTransfer(ws, fileId);
			} finally {
				if (sessionA != null) {
					sessionA.close();
				}
			}
		}

	}

	private class ReadThread extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession sessionA = null;
			OutputStream fos = null;
			ScmInputStream sis = null;
			try {
				sessionA = TestScmTools.createSession(branceSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
				ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
				String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());
				fos = new FileOutputStream(new File(downloadPath));
				sis = ScmFactory.File.createInputStream(scmfile);
				sis.read(fos);
			} finally {
				if (fos != null) {
					fos.close();
				}
				if (sis != null) {
					sis.close();
				}
				if (sessionA != null) {
					sessionA.close();
				}
			}
		}
	}

	private void prepareFiles() throws Exception {
		ScmFile scmfile = ScmFactory.File.createInstance(wsA);
		scmfile.setContent(filePath);
		scmfile.setFileName(fileName+"_"+UUID.randomUUID());
		fileId = scmfile.save();
	}

	private void checkResult() {
		SiteWrapper rootSite;
		try {
			rootSite = ScmInfo.getRootSite();
			SiteWrapper[] expSiteList = { rootSite,branceSite };
			ScmTaskUtils.waitAsyncTaskFinished(wsA, fileId, expSiteList.length);
			ScmFileUtils.checkMetaAndData(ws_T,fileId, expSiteList, localPath, filePath);
		} catch (Exception e) {
			Assert.fail(e.getMessage() + "  fileId =" + fileId.get() + "  ws=" + wsA.toString());
		}
	}
}