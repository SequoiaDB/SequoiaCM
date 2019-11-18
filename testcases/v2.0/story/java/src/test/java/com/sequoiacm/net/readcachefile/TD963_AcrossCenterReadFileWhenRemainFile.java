package com.sequoiacm.net.readcachefile;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @Testcase: SCM-963:文件在A中心，B中心有残留相同LOB（大小和内容一致），B中心读取文件
 * @author huangxiaoni init
 * @date 2017.11.9
 */

public class TD963_AcrossCenterReadFileWhenRemainFile extends TestScmBase {
	private boolean runSuccess = false;
	private List<SiteWrapper> sites = null;
	private WsWrapper wsp = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;

	private String fileName = "file963";
	private ScmId fileId = null;
	private int fileSize = 10;
	private File localPath = null;
	private String filePath = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException, IOException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);
		wsp = ScmInfo.getWs();
		sites = ScmNetUtils.getRandomSites(wsp);
		sessionA = TestScmTools.createSession(sites.get(0));
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
	}

	@Test(groups = {"fourSite"})
	private void test() throws Exception {
		// write from centerA
		fileId = ScmFileUtils.create(wsA, fileName, filePath);
		// remain file from centerB
		TestSdbTools.Lob.putLob(sites.get(1), wsp, fileId, filePath);
		// read from centerB
		this.readFileFromB();
		// check result
		SiteWrapper[] expSites = {sites.get(0), sites.get(1)};
		ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || forceClear) {
				ScmFactory.File.deleteInstance(wsA, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
		}
	}

	private void readFileFromB() throws Exception {
		ScmSession session = null;
		try {
			session = TestScmTools.createSession(sites.get(1));
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			// read scmfile
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			file.getContent(downloadPath);
			Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
		}finally {
			if (session != null) {
				session.close();
			}
		}
	}
}