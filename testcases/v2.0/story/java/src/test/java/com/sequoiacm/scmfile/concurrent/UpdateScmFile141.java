package com.sequoiacm.scmfile.concurrent;

import java.io.File;

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

/**
 * @FileName SCM-141: 并发修改同一文件不同属性
 * @Author fanyu
 * @Date 2017-06-14
 * @Version 1.00
 */
/*
 * 1、多线程并发，A、B并发修改同一个文件不同属性 2、检查操作后的文件属性正确性
 */
public class UpdateScmFile141 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper rootSite = null;
	private static SiteWrapper branSite = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private String fileName = "updateAttri141";
	private File localPath = null;
	private String filePath = null;
	private ScmId fileId = null;
	private final int FILE_SIZE = 100;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + FILE_SIZE + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, FILE_SIZE);

			rootSite = ScmInfo.getRootSite();
			branSite = ScmInfo.getBranchSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(rootSite);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
			ScmFileUtils.cleanFile(wsp, cond);
			fileId = ScmFileUtils.create(ws, fileName, filePath);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() {
		try {
			UpdateThreadA updateThdA = new UpdateThreadA();
			UpdateThreadM updateThdM = new UpdateThreadM();
			updateThdA.start();
			updateThdM.start();
			if (!(updateThdA.isSuccess() && updateThdM.isSuccess())) {
				Assert.fail(updateThdA.getErrorMsg() + updateThdM.getErrorMsg());
			}
			checkAttrUpdated();
			runSuccess = true;
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}

	private class UpdateThreadA extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(branSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

				ScmFile file = ScmFactory.File.getInstance(ws, fileId);
				int updateTimes = 1000;
				for (int i = 0; i < updateTimes; ++i) {
					String str = fileName + "_" + i;
					file.setAuthor(str);
				}
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}

	private class UpdateThreadM extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(rootSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

				ScmFile file = ScmFactory.File.getInstance(ws, fileId);
				int updateTimes = 1000;
				for (int i = 0; i < updateTimes; ++i) {
					String str = fileName + "_" + i;
					file.setTitle(str);
				}
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}

	private void checkAttrUpdated() throws ScmException {
		try {
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			String str = fileName + "_999";
			Assert.assertEquals(file.getAuthor(), str);
			Assert.assertEquals(file.getTitle(), str);
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}
}
