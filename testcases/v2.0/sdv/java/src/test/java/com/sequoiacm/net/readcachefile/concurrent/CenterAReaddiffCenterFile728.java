package com.sequoiacm.net.readcachefile.concurrent;

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
import com.sequoiadb.exception.BaseException;

/**
 * @Description:SCM-728 : 在A中心并发读取其他3个分中心的不同文件 1、在分中心B、C、D写入文件；
 *                      2、在分中心A并发读取其他3个分中心的文件； 3、检查文件元数据正确性；检查所有中心LOB正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class CenterAReaddiffCenterFile728 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper site1 = null;
	private SiteWrapper site2 = null;
	private SiteWrapper site3 = null;
	private WsWrapper wsp = null;

	private int fileSize = 1024 * 1;
	private File localPath = null;
	private String filePath = null;
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private static final String author = "CenterAReaddiffCenterFile728";

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
			wsp = ScmInfo.getWs();
			List<SiteWrapper> sites = ScmNetUtils.getAllSite(wsp);
			site1 = sites.get(0);
			site2 = sites.get(1);
			site3 = sites.get(2);

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(wsp, cond);

			prepareFiles(site1);
			prepareFiles(site2);
			prepareFiles(site3);
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "fourSite" })
	private void test() throws Exception {
		ReadScmFile rThread1 = new ReadScmFile(site3, fileIdList.get(0));
		rThread1.start(3);

		ReadScmFile rThread2 = new ReadScmFile(site3, fileIdList.get(1));
		rThread2.start(3);

		ReadScmFile rThread3 = new ReadScmFile(site3, fileIdList.get(2));
		rThread3.start(3);

		if (!(rThread1.isSuccess() && rThread2.isSuccess() && rThread3.isSuccess())) {
			Assert.fail(rThread1.getErrorMsg() + rThread2.getErrorMsg() + rThread3.getErrorMsg());
		}

		checkResult(fileIdList.get(0));
		checkResult(fileIdList.get(1));
		checkResult(fileIdList.get(2));

		checkMetadataAndLobs();

		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || forceClear) {
				BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
				ScmFileUtils.cleanFile(wsp, cond);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (BaseException | ScmException e) {
			Assert.fail(e.getMessage());
		} finally {

		}
	}

	private class ReadScmFile extends TestThreadBase {
		private SiteWrapper site = null;
		private ScmId fileId = null;

		public ReadScmFile(SiteWrapper site, ScmId fileId) {
			this.site = site;
			this.fileId = fileId;
		}

		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(site);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
				ScmFile file = ScmFactory.File.getInstance(ws, fileId);
				String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());
				file.getContent(downloadPath);
				Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (null != session) {
					session.close();
				}
			}
		}
	}

	private void checkResult(ScmId fileId) throws ScmException {
		ScmSession session = null;
		try {
			session = TestScmTools.createSession(site1);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			Assert.assertEquals(file.getWorkspaceName(), wsp.getName());
			Assert.assertEquals(file.getFileId().get(), fileId.get());
			Assert.assertEquals(file.getAuthor(), author);
			Assert.assertEquals(file.getSize(), fileSize);
			Assert.assertEquals(file.getMinorVersion(), 0);
			Assert.assertEquals(file.getMajorVersion(), 1);
			Assert.assertEquals(file.getUser(), TestScmBase.scmUserName);
			Assert.assertNotNull(file.getCreateTime().getTime());
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (null != session) {
				session.close();
			}
		}
	}

	private void checkMetadataAndLobs() throws Exception {
		SiteWrapper[] expSites0 = { site1,site3 };
		ScmFileUtils.checkMetaAndData(wsp, fileIdList.get(0), expSites0, localPath, filePath);

		SiteWrapper[] expSites1 = {site2,site3 };
		ScmFileUtils.checkMetaAndData(wsp, fileIdList.get(1), expSites1, localPath, filePath);

		SiteWrapper[] expSites2 = { site3 };
		ScmFileUtils.checkMetaAndData(wsp, fileIdList.get(2), expSites2, localPath, filePath);
	}

	private void prepareFiles(SiteWrapper site) {
		ScmSession session = null;
		try {
			session = TestScmTools.createSession(site);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setContent(filePath);
			scmfile.setFileName(author + "_" + UUID.randomUUID());
			scmfile.setAuthor(author);
			fileIdList.add(scmfile.save());
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (null != session) {
				session.close();
			}
		}
	}
}
