package com.sequoiacm.asynctask.concurrent;

import java.io.File;
import java.io.IOException;
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

/**
 * @Description: SCM-755 : 并发异步缓存、异步迁移不同文件 1、并发异步缓存文件 2、异步迁移文件； 3、检查执行结果正确性；
 * @author fanyu
 * @Date:2017年8月11日
 */
public class AsyncCacheAndTransferDiffFile755 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper rootSite = null;
	private SiteWrapper branceSite = null;
	private WsWrapper ws_T = null;
	private ScmSession sessionM = null;
	private ScmWorkspace wsM = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;

	private int fileSize = 1024 * new Random().nextInt(1025);
	private File localPath = null;
	private String filePath = null;
	private List<ScmId> fileIdList1 = new ArrayList<ScmId>();
	private List<ScmId> fileIdList2 = new ArrayList<ScmId>();
	private static final String author = "CacheAndTransferDiffeFile755";
	private int fileNum = 30;	

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
			
			rootSite = ScmInfo.getRootSite();
			branceSite = ScmInfo.getBranchSite();
			ws_T = ScmInfo.getWs();
					
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(ws_T,cond);

			sessionM = TestScmTools.createSession(rootSite);
			wsM = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);
			sessionA = TestScmTools.createSession(branceSite);
			wsA = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
			write(wsM, fileIdList1);
			write(wsA, fileIdList2);
		} catch (IOException | ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" } ) 
	private void test() {
		try {
			CacheFile cThread = new CacheFile();
			cThread.start(5);

			TransferFile tThread = new TransferFile();
			tThread.start(5);

			if (!(cThread.isSuccess() && tThread.isSuccess())) {
				Assert.fail(cThread.getErrorMsg() + tThread.getErrorMsg());
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
				try {
					for(ScmId fileId : fileIdList1){
						ScmFactory.File.deleteInstance(wsM, fileId,true);
					}
					for(ScmId fileId : fileIdList2){
						ScmFactory.File.deleteInstance(wsM, fileId,true);
					}
					TestTools.LocalFile.removeFile(localPath);
				} catch (ScmException e) {
					Assert.fail(e.getMessage());
				}
			}
		} finally {
			if (sessionM != null) {
				sessionM.close();
			}
			if (sessionA != null) {
				sessionA.close();
			}

		}
	}

	private class CacheFile extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(branceSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
				for (ScmId fileId : fileIdList1) {
					ScmFactory.File.asyncCache(ws, fileId);
				}
			} catch (Exception e) {
				Assert.fail(e.getMessage());
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}

	private class TransferFile extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(branceSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
				for (ScmId fileId : fileIdList2) {
					ScmFactory.File.asyncTransfer(ws, fileId);
				}
			} catch (Exception e) {
				Assert.fail(e.getMessage());
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}

	private void write(ScmWorkspace ws, List<ScmId> fileIdList) {
		try {
			for (int i = 0; i < fileNum; i++) {
				ScmFile file;
				file = ScmFactory.File.createInstance(ws);
				file.setContent(filePath);
				file.setAuthor(author);
				file.setFileName(author+"_"+UUID.randomUUID());
				ScmId fileId = file.save();
				fileIdList.add(fileId);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	private void checkResult() {
		try {
			SiteWrapper[] expSiteList = { rootSite, branceSite };
			for (ScmId fileId : fileIdList1) {
				ScmTaskUtils.waitAsyncTaskFinished(wsM, fileId, expSiteList.length);
			}
			ScmFileUtils.checkMetaAndData(ws_T, fileIdList1, expSiteList, localPath, filePath);

			for (ScmId fileId : fileIdList2) {
				ScmTaskUtils.waitAsyncTaskFinished(wsM, fileId, expSiteList.length);
			}
			ScmFileUtils.checkMetaAndData(ws_T, fileIdList2, expSiteList, localPath, filePath);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
