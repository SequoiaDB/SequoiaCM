package com.sequoiacm.readcachefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase:SCM232 本地读取文件 （A/B网络不通） 1、本地中心写文件，分别覆盖：分中心、主中心； 2、连接本中心，本地读取该文件；
 *                  3、检查写入后的元数据及lob正确性； 4、检查读取文件内容正确性。
 * @author fanyu init in 2017.05.10; huangxiaoni modify in 2017.6.6
 */

public class ReadFileFromLocalCenter232 extends TestScmBase {
	private boolean runSuccess1 = false;
	private boolean runSuccess2 = false;
	private SiteWrapper rootSite = null;
	private SiteWrapper branSite = null;
	private WsWrapper wsp = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;

	private String fileName = "readCacheFile232";
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private int fileSize = 100;
	private File localPath = null;
	private String filePath = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws IOException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			// ready file
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);

			rootSite = ScmInfo.getRootSite();
			branSite = ScmInfo.getBranchSite();
			wsp = ScmInfo.getWs();

			session = TestScmTools.createSession(branSite);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void testWRLocalFileFromM() throws Exception {
		try {
			ScmId fileId = writeFile(rootSite);
			readFile(rootSite, fileId);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess1 = true;
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void testWRLocalFileFromA() throws Exception {
		try {
			ScmId fileId = writeFile(branSite);
			readFile(branSite, fileId);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess2 = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if ((runSuccess1 && runSuccess2) || forceClear) {
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(ws, fileId, true);
				}
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}

	private ScmId writeFile(SiteWrapper site) throws Exception {
		ScmSession session = null;
		ScmId fileId = null;
		try {
			session = TestScmTools.createSession(site);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			// write file
			fileId = ScmFileUtils.create(ws, fileName+"_"+UUID.randomUUID(), filePath);
			fileIdList.add(fileId);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return fileId;
	}

	private void readFile(SiteWrapper site, ScmId fileId) throws Exception {
		ScmSession session = null;
		OutputStream fos = null;
		ScmInputStream sis = null;
		try {
			session = TestScmTools.createSession(site);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			// read content
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			fos = new FileOutputStream(new File(downloadPath));
			sis = ScmFactory.File.createInputStream(file);
			sis.read(fos);

			// check results
			Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));

			SiteWrapper[] expSites = { site };
			ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);

		} finally {
			if (fos != null)
				fos.close();
			if (sis != null)
				sis.close();
			if (session != null)
				session.close();
		}
	}

}
