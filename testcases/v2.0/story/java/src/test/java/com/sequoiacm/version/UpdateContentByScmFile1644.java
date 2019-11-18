package com.sequoiacm.version;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmOutputStream;
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
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:specify that scmfile outputStream update Content of  the scmfile 
 * testlink-case:SCM-1644
 * 
 * @author wuyan
 * @Date 2018.06.02
 * @version 1.00
 */

public class UpdateContentByScmFile1644 extends TestScmBase {
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;	
	private ScmId fileId = null;

	private String fileName = "file1644";
	private int fileSize = 1024 * 800;
	private byte[] contentdata = new byte[ 1024 * 1024 * 2 ];	
	private byte[] partdata = new byte[ 1024 * 1024 ];	
	private File localPath = null;
	private String filePath = null;	
	

	@BeforeClass
	private void setUp() throws IOException, ScmException {        
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";

		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);

		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		
		fileId = VersionUtils.createFileByFile( ws, fileName, filePath );
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite"})
	private void test() throws Exception {	
		//test a:scmfile outputStream update Content
		updateContentByAllOutputStream();
		//test b:scmfile outputStream specified length update Content
		updateContentByPartOutputStream();
		//check result
		int currentVersion = 3;
		int historyVersion1 = 1;
		int historyVersion2 = 2;
		
		VersionUtils.CheckFileContentByFile(  ws, fileName, historyVersion1, filePath, localPath );
		VersionUtils.CheckFileContentByStream(  ws, fileName, historyVersion2,  contentdata );
		//http://jira:8080/browse/SEQUOIACM-274
		//VersionUtil.CheckFileContentByStream(  ws, fileName, currentVersion, partdata );
		VersionUtils.checkFileCurrentVersion(ws, fileId, currentVersion);	
		 
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

	//test a 
	private void updateContentByAllOutputStream() throws ScmException{		
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		ScmOutputStream fileOutStream = ScmFactory.File.createUpdateOutputStream(file);	
		new Random().nextBytes(contentdata);	
		fileOutStream.write(contentdata);
		fileOutStream.commit();	
	}
	
	//test b
	private void updateContentByPartOutputStream() throws ScmException{		
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		ScmOutputStream fileOutStream = ScmFactory.File.createUpdateOutputStream(file);		
		int off = 1024 * 500 ;
		int len = 1024 * 1024;
		System.arraycopy(contentdata, off, partdata, 0, len);		
		fileOutStream.write(contentdata, off, len);
		fileOutStream.commit();			
	}
	
	

	
}