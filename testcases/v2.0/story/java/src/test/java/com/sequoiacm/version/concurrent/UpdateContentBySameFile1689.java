package com.sequoiacm.version.concurrent;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

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
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content: update Content of  the same file concurrently,two ways of concurrent:
 *               a.update content by file
 *               b.update content by breakpointfile
 * testlink-case:SCM-1689
 * 
 * @author wuyan
 * @Date 2018.06.13
 * @version 1.00
 */

public class UpdateContentBySameFile1689 extends TestScmBase {
	private boolean runSuccess = false;
	private static WsWrapper wsp = null;
	private SiteWrapper branSite = null;
	private SiteWrapper rootSite = null;	
	private ScmSession sessionM = null;	
	private ScmWorkspace wsA = null;
	private ScmSession sessionA = null;	
	private ScmWorkspace wsM = null;
	private ScmId fileId = null;
	private File localPath = null;
	private String filePath = null;

	private String fileName = "versionfile1689";
	private String authorName = "author1689";
	private byte[] writeData = new byte[ 1024 * 20 ];
	private final int FILE_SIZE = 1024 * 800;

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		VersionUtils.checkDBDataSource();
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + FILE_SIZE + ".txt";
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, FILE_SIZE);

		branSite = ScmInfo.getBranchSite();
		rootSite = ScmInfo.getRootSite();
		wsp = ScmInfo.getWs();
		
		sessionM = TestScmTools.createSession(rootSite);
		wsM = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionM);
		sessionA = TestScmTools.createSession(branSite);
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		
		fileId = VersionUtils.createFileByStream( wsM, fileName, writeData, authorName );		
	}

	@Test(groups = {  "twoSite", "fourSite"})
	private void test() throws Exception {
		int updateSize = 1024 * 900 ;		
		byte[] updateData = new byte[ updateSize ];
		
		createBreakPointFile(wsA, updateData);
		UpdateContentByBreakpointFile updateByBreakpointFile = new UpdateContentByBreakpointFile();
		UpdateContentByFile updateByFile = new UpdateContentByFile();
		updateByBreakpointFile.start();
		updateByFile.start();
		
		if( updateByBreakpointFile.isSuccess()){
			if( !updateByFile.isSuccess()){
				Assert.assertTrue( !updateByFile.isSuccess(), updateByFile.getErrorMsg());
				ScmException e =(ScmException) updateByFile.getExceptions().get(0);					
				Assert.assertEquals(e.getError(),ScmError.FILE_VERSION_MISMATCHING,
						"updateContent by file fail:"+updateByFile.getErrorMsg());		
				checkUpdateByBreakpointfileResult( wsM, updateData );
			}else if(updateByFile.isSuccess()){				
				checkAllUpdateContentResult(wsM, updateData);
			}else{
				Assert.fail("the results can only by updated successfully or one update succeeds");				
			}
		}else if(!updateByBreakpointFile.isSuccess()){
			Assert.assertTrue( updateByFile.isSuccess(), updateByFile.getErrorMsg());
			ScmException e =(ScmException) updateByBreakpointFile.getExceptions().get(0);				
			Assert.assertEquals(e.getError(),ScmError.FILE_VERSION_MISMATCHING,
					"updateContent by breakpointfile fail:"+updateByBreakpointFile.getErrorMsg());		
			checkUpdateByFileResult(wsM);
			ScmFactory.BreakpointFile.deleteInstance(wsA, fileName);
		}	
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() {		
		try {	
			if( runSuccess ){
				ScmFactory.File.deleteInstance(wsM, fileId, true);
			}						
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionM != null) {
				sessionM.close();
			}			
		}
	}	
	
	public class UpdateContentByFile extends TestThreadBase {		
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(branSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);	
				VersionUtils.updateContentByFile(ws, fileName, fileId, filePath);				
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}
	
	public class UpdateContentByBreakpointFile extends TestThreadBase {		
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(branSite);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);			
				ScmFile file = ScmFactory.File.getInstance(ws, fileId);
				ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.getInstance(ws, fileName);
				file.updateContent(breakpointFile);					
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}

	private void createBreakPointFile(ScmWorkspace ws, byte[] updateData) throws ScmException{
		ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile.createInstance(ws, fileName);
		new Random().nextBytes(updateData);	
		breakpointFile.upload(new ByteArrayInputStream(updateData));
	}
	
	private void checkAllUpdateContentResult(ScmWorkspace ws,byte[] updatedata) throws Exception{
		int historyVersion1 = 1;
		//first updateContent version
		int historyVersion2 = 2;
		//second updateContent version
		int currentVersion = 3;
		
		ScmFile file = ScmFactory.File.getInstance(ws, fileId, currentVersion, 0);
		long fileSize = file.getSize();
		
		//check the updateContent 
		if ( fileSize == updatedata.length){
			VersionUtils.CheckFileContentByStream(ws, fileName, currentVersion, updatedata);
			VersionUtils.CheckFileContentByFile(ws, fileId, historyVersion2, filePath, localPath);			
		}else if (fileSize == FILE_SIZE ){
			VersionUtils.CheckFileContentByFile(ws, fileId, currentVersion, filePath, localPath);			
			VersionUtils.CheckFileContentByStream(ws, fileName, historyVersion2, updatedata);
		}else{
			Assert.fail("update file content is error!");
		}
		//check the write content 
		VersionUtils.CheckFileContentByStream(ws, fileName, historyVersion1, writeData);
	}
	
	private void checkUpdateByBreakpointfileResult(ScmWorkspace ws,byte[] updatedata) throws Exception{
		int historyVersion = 1;		
		int currentVersion = 2;
		VersionUtils.CheckFileContentByStream(ws, fileName, currentVersion, updatedata);	
		VersionUtils.CheckFileContentByStream(ws, fileName, historyVersion, writeData);	
		
		//check the breakpoint is not exist
		try {
			ScmFactory.BreakpointFile.getInstance(ws, fileName);
			Assert.fail("get breakpoint file must bu fail!");
		} catch (ScmException e) {
			if ( ScmError.FILE_NOT_FOUND != e.getError()) {
				Assert.fail("expErrorCode:-262  actError:"+e.getError()+e.getMessage());
			}
		}		
	}
	
	private void checkUpdateByFileResult(ScmWorkspace ws) throws Exception{
		int historyVersion = 1;		
		int currentVersion = 2;		
		VersionUtils.CheckFileContentByFile(ws, fileId, currentVersion, filePath, localPath);
		VersionUtils.CheckFileContentByStream(ws, fileName, historyVersion, writeData);			
	}
}