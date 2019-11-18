package com.sequoiacm.session.concurrent;

import java.io.File;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-227: 并发登入
 * @Author linsuqiang
 * @Date 2017-06-13
 * @Version 1.00
 */

/*
 * 1、多线程并发登入客户端 2、分别对不同登入线程做文件操作 3、登出客户端 4、检查登入登出、文件是否操作成功
 */

public class Login227 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;

	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private String fileName = "session227";
	private ScmId fileId = null;
	private final int FILE_SIZE = 100;
	private File localPath = null;
	private String filePath = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + FILE_SIZE + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, FILE_SIZE);

			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
			ScmFileUtils.cleanFile(wsp, cond);

			fileId = ScmFileUtils.create(ws, fileName, filePath);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() {
		try {
			QueryThread qryThd = new QueryThread();
			qryThd.start(20);

			if (!qryThd.isSuccess()) {
				Assert.fail(qryThd.getErrorMsg());
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}

	private class QueryThread extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(site);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

				BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
				long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
				Assert.assertEquals(count, 1);
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}

}