package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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

/**
* test content:breakpoint continuation file in input stream 
* testlink case:seqDB-1374
* @author wuyan
    * @Date    2018.05.18
* @version 1.00
*/

public class UploadBreakpointFile1374b extends TestScmBase {
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;
	private ScmId fileId = null;

	private String fileName = "breakpointfile1374b";

	private File localPath = null;
	private int dataSize = 1024 * 1024 * 2;
	private byte[] data = new byte[dataSize];

	@BeforeClass
	private void setUp() throws IOException, ScmException {	
		BreakpointUtil.checkDBDataSource();
		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);	
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {		
		createBreakpointFile();
		continuesUploadFile();
		checkUploadFileData();
		
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
	
	private void createBreakpointFile() throws ScmException, IOException {
		// create breakpointfile
		ScmChecksumType checksumType = ScmChecksumType.NONE;
		ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.createInstance(ws, fileName,checksumType);			
		
        new Random().nextBytes(data);  
        int uploadSize = 1;
        byte[] datapart = new byte[uploadSize];
        System.arraycopy(data, 0, datapart, 0, uploadSize);			
		breakpointFile.incrementalUpload(new ByteArrayInputStream(datapart), false);			
	}	
	

	private void continuesUploadFile() throws ScmException, IOException{
		ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.getInstance(ws, fileName);				
		breakpointFile.upload(new ByteArrayInputStream(data));					
	}
	
	private void checkUploadFileData() throws Exception{
		//save to file, than down file check the file data
		ScmFile file = ScmFactory.File.createInstance(ws);
		ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.getInstance(ws, fileName);	
		file.setContent(breakpointFile);
		file.setFileName(fileName);
		fileId = file.save();	        
			   
		//down file 
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
								Thread.currentThread().getId());			
		file.getContent(downloadPath);			

		// check results	
		Assert.assertEquals(TestTools.getMD5(downloadPath), TestTools.getMD5(data));
	}
	

}