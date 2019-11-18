package com.sequoiacm.net.asynctask.concurrent;

import java.io.File;
import java.io.IOException;
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
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Description:SCM-752 : 分中心A的2个节点并发异步缓存相同文件 1、分中心A的2个节点并发异步缓存相同文件；
 *                      2、检查执行结果正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class AsyncCacheSameFile752 extends TestScmBase {
	private boolean runSuccess = false;
	private int fileSize = 1024 * new Random().nextInt(1025);
	private File localPath = null;
	private String filePath = null;
	private ScmId fileId = null;
	private String author = "DiffNodeCacheSameFile752";
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	
	private SiteWrapper sourceSite = null;
	private SiteWrapper targetSite = null;
	private WsWrapper ws_T = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
			
			ws_T = ScmInfo.getWs();
			List<SiteWrapper> siteList = ScmNetUtils.getSortSites(ws_T);
			sourceSite = siteList.get(0);
			targetSite = siteList.get(1);

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(ws_T,cond);

			session = TestScmTools.createSession(targetSite);
			ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
			write();
		} catch (IOException | ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() {
		try {
			CacheFile cThread1 = new CacheFile(sourceSite);
			cThread1.start(10);

			CacheFile cThread2 = new CacheFile(sourceSite);
			cThread2.start(10);

			if (!(cThread1.isSuccess() && cThread2.isSuccess())) {
				Assert.fail(cThread1.getErrorMsg() + cThread2.getErrorMsg());
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
				ScmFactory.File.deleteInstance(ws, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (BaseException | ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if(session != null){
				session.close();
			}
		}
	}

	private class CacheFile extends TestThreadBase {
		private SiteWrapper site;

		public CacheFile(SiteWrapper site) {
			this.site = site;
		}

		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(site);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
				ScmFactory.File.asyncCache(ws, fileId);
			} catch (Exception e) {
				Assert.fail(e.getMessage());
			} finally {
				if (null != session) {
					session.close();
				}
			}
		}
	}

	private void write() throws ScmException {
		ScmFile file = ScmFactory.File.createInstance(ws);
		file.setContent(filePath);
		file.setFileName(author+"_"+UUID.randomUUID());
		file.setAuthor(author);
		fileId = file.save();
	}

	private void checkResult() throws Exception {
		SiteWrapper[] expSiteList = { sourceSite, targetSite };
		ScmTaskUtils.waitAsyncTaskFinished(ws, fileId,expSiteList.length);
		ScmFileUtils.checkMetaAndData(ws_T,fileId, expSiteList, localPath, filePath);
	}
}
