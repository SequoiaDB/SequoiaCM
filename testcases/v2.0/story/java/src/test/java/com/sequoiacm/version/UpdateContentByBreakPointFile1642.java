package com.sequoiacm.version;

import java.io.File;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:specify that the breakpoint file update Content of  the current scm file 
 * testlink-case:SCM-1642 
 * @author wuyan
 * @Date 2018.06.01
 * @version 1.00
 */

public class UpdateContentByBreakPointFile1642 extends TestScmBase {
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;	
	private ScmId fileId = null;

	private String fileName = "file1642";
	private int fileSize = 1024 * 1024 * 3;
	private byte[] filedata = new byte[fileSize];
	private File localPath = null;
	private String filePath = null;	
	

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		VersionUtils.checkDBDataSource();
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";

		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);

		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite"})
	private void test() throws Exception {		
		fileId = VersionUtils.createFileByStream( ws, fileName, filedata );
		updateContentByBreakPointFile();
		
		int currentVersion = 2;
		int historyVersion = 1;
		VersionUtils.CheckFileContentByFile(ws, fileName, currentVersion, filePath, localPath);
		VersionUtils.CheckFileContentByStream(ws, fileName, historyVersion, filedata);
		VersionUtils.checkFileCurrentVersion(ws, fileId, currentVersion);
		checkBreakPointFile();
	}

	@AfterClass
	private void tearDown() {
		try {			
			ScmFactory.File.deleteInstance(ws, fileId, true);
			TestTools.LocalFile.removeFile(localPath);	
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}			
		}
	}
	

	private void updateContentByBreakPointFile() throws ScmException{
		//create breakpointfile
		createBreakPointFile();	
		//updataContent of file
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.getInstance(ws, fileName);
		file.updateContent(breakpointFile);			
	}
	
	private void createBreakPointFile() throws ScmException{
		ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.createInstance(ws, fileName);
		breakpointFile.upload(new File(filePath));
	}
	
	private void checkBreakPointFile(){
		// check breakpointfile not exist
		try {
			ScmFactory.BreakpointFile.getInstance(ws, fileName);
			Assert.fail("get breakpoint file must bu fail!");
		} catch (ScmException e) {
			if ( ScmError.FILE_NOT_FOUND != e.getError()) {
				Assert.fail("expErrorCode:-262  actError:"+e.getError()+e.getMessage());
			}
		}
	}

	
}