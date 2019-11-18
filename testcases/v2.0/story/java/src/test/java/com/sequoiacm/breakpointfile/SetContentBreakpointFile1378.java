package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * test content:breakpoint continuation file,then setContent to file
 * testlink-case:SCM-1378
 * 
 * @author wuyan
 * @Date 2018.05.21
 * @version 1.00
 */

public class SetContentBreakpointFile1378 extends TestScmBase {
	private final int branSitesNum = 2;
	private static WsWrapper wsp = null;
	private List<SiteWrapper> branSites = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionB = null;
	private ScmWorkspace wsB = null;
	private ScmId fileId = null;

	private String fileName = "breakpointfile1378";
	private int fileSize = 1024 * 1024 * 55;
	private File localPath = null;
	private String filePath = null;
	private boolean runSuccess = false;

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		BreakpointUtil.checkDBDataSource();
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";

		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);

		branSites = ScmInfo.getBranchSites(branSitesNum);
		wsp = ScmInfo.getWs();
		sessionA = TestScmTools.createSession(branSites.get(0));
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionB = TestScmTools.createSession(branSites.get(1));
		wsB = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionB);
		runSuccess = true;
	}

	@Test(groups = {  "fourSite" })
	private void test() throws Exception {
		createBreakpointFile();
		uploadAndSetContentFile();
		downLoadFileAndCheckData();
	}

	@AfterClass
	private void tearDown() {
		try {
			if (runSuccess) {
				ScmFactory.File.deleteInstance(wsB, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
			if (sessionB != null) {
				sessionB.close();
			}
		}
	}

	private void createBreakpointFile() throws ScmException, IOException {
		// create breakpointfile
		ScmChecksumType checksumType = ScmChecksumType.CRC32;
		ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.createInstance(wsA, fileName, checksumType);
		int uploadSize = 1024 * 51;
		InputStream inputStream = new BreakpointStream(filePath, uploadSize);
		breakpointFile.incrementalUpload(inputStream, false);
		inputStream.close();
	}

	private void uploadAndSetContentFile() throws ScmException, IOException {
		ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.getInstance(wsA, fileName);
		FileInputStream fStream = new FileInputStream(filePath);
		breakpointFile.upload(fStream);

		ScmFile file = ScmFactory.File.createInstance(wsA);
		file.setContent(breakpointFile);
		file.setFileName(fileName);
		fileId = file.save();

		// check breakpointfile not exist
		try {
			ScmFactory.BreakpointFile.getInstance(wsA, fileName);
			Assert.fail("get breakpoint file must bu fail!");
		} catch (ScmException e) {
			if ( ScmError.FILE_NOT_FOUND != e.getError()) {
				Assert.fail("expErrorCode:-262  actError:"+e.getError()+e.getMessage());
			}
		}
	}

	private void downLoadFileAndCheckData() throws Exception {
        ScmFile file = ScmFactory.File.getInstanceByPath(wsB, fileName);
		// down file
		String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
				Thread.currentThread().getId());
		file.getContent(downloadPath);

		// check results
		Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));
		TestTools.LocalFile.removeFile(downloadPath);
	}
}