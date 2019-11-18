/**
 * 
 */
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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**在ws1和ws2并发做如下操作（每个并发均包含如下操作）：
 *  1）写文件； 2）本地读文件； 3）跨中心读文件；
 *  4）迁移任务迁移文件； 5）清理任务清理文件； 6）单个文件异步缓存
 * @Description:TODO
 * @author fanyu
 * @Date:2017年9月12日
 * @version:1.0
 */
public class AllOperWithDiffWs739 extends TestScmBase {
	private boolean runSuccess = false;
	private int fileSize = 1;
	private File localPath = null;
	private String filePath = null;
	private List<String> filePathList = new ArrayList<String>();
	private static final String author = "AllOperationWithDiffWs739";
	
	private List<WsWrapper> ws_TList = new ArrayList<WsWrapper>();

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		filePathList.add(filePath);
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
			
			ws_TList = ScmInfo.getWss(2);

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(ws_TList.get(0),cond);
			ScmFileUtils.cleanFile(ws_TList.get(1),cond);

		} catch (IOException | ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "fourSite" })
	private void test() throws ScmException {
		DoAll dThread1 = new DoAll(ws_TList.get(0));
		dThread1.start();

		DoAll dThread2 = new DoAll(ws_TList.get(1));
		dThread2.start();

		if (!(dThread1.isSuccess() && dThread2.isSuccess())) {
			Assert.fail(dThread1.getErrorMsg() + dThread2.getErrorMsg());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || forceClear) {
				BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
				ScmFileUtils.cleanFile(ws_TList.get(0),cond);
				ScmFileUtils.cleanFile(ws_TList.get(1),cond);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (BaseException | ScmException e) {
			Assert.fail(e.getMessage());
		} finally {

		}
	}

	private class DoAll extends TestThreadBase {
		private String wsName = null;
		private ScmId fileId = null;
		private String downloadPath = null;
		private ScmFile file = null;
		private List<SiteWrapper> siteList = new ArrayList<SiteWrapper>();

		public DoAll(WsWrapper wsp) throws ScmException {
			siteList = ScmNetUtils.getSortSites(wsp);
			this.wsName = wsp.getName();
		}

		@Override
		public void exec() throws Exception {
			write();
			// diff center read
			read();
			// transfer
			transfer();
			// clean
			clean();
			// cache
			cache();
			// transfer
			sinleTransfer();
		}

		public void write() {
			ScmSession session = null;
			// login
			try {
				session = TestScmTools.createSession(siteList.get(0));
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(this.wsName, session);
				// write
				ScmFile file = ScmFactory.File.createInstance(ws);
				file.setContent(filePath);
				file.setFileName(author+"_"+UUID.randomUUID());
				file.setAuthor(author);
				fileId = file.save();
				setFileId(fileId);
			} catch (ScmException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}

		public void read() {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(siteList.get(1));
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(this.wsName, session);
				file = ScmFactory.File.getInstance(ws, fileId);
				downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());
				file.getContent(downloadPath);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (null != session) {
					session.close();
				}
			}
		}

		public void transfer() {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(siteList.get(0));
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(this.wsName, session);
				BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
				ScmId taskId = ScmSystem.Task.startTransferTask(ws, condition, ScopeType.SCOPE_CURRENT, siteList.get(1).getSiteName());
				ScmTaskUtils.waitTaskFinish(session, taskId);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (null != session) {
					session.close();
				}
			}
		}

		public void clean() {
			ScmSession session = null;
			ScmId taskId = null;
			try {
				session = TestScmTools.createSession(siteList.get(0));
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, session);
				BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
				taskId = ScmSystem.Task.startCleanTask(ws, condition);
				ScmTaskUtils.waitTaskFinish(session, taskId);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (null != session) {
					session.close();
				}
			}
		}

		public void cache() {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(siteList.get(0));
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, session);
				ScmFactory.File.asyncCache(ws, fileId);
				SiteWrapper[]  expSiteList = { siteList.get(0), siteList.get(1) };
				ScmTaskUtils.waitAsyncTaskFinished(ws, fileId, expSiteList.length);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (null != session) {
					session.close();
				}
			}
		}

		public void sinleTransfer() {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(siteList.get(0));
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, session);
				ScmFactory.File.asyncTransfer(ws, fileId);
				SiteWrapper[]  expSiteList = { siteList.get(0), siteList.get(1) };
				ScmTaskUtils.waitAsyncTaskFinished(ws, fileId, expSiteList.length);
				ScmFileUtils.checkMeta(ws, fileId, expSiteList);
				ScmFileUtils.checkData(ws, fileId, localPath, downloadPath);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (null != session) {
					session.close();
				}
			}
		}

		public void setFileId(ScmId fileId) {
			this.fileId = fileId;
		}
	}
}
