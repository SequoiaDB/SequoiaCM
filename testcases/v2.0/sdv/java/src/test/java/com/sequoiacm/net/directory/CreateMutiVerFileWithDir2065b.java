package com.sequoiacm.net.directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmDirUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:specify directory to create multiple files,one of the version files specifies the directory
 * testlink-case:SCM-2065b
 * 
 * @author wuyan
 * @Date 2018.07.12
 * @version 1.00
 */

public class CreateMutiVerFileWithDir2065b extends TestScmBase {
	private boolean runSuccess = false;	
	private SiteWrapper branSite = null;
	private static WsWrapper wsp = null;
	
	private ScmSession session = null;
	private ScmWorkspace ws = null;		

	private ScmId fileId = null;
	private List<String> dirNames = new ArrayList<String>();
	private String authorName = "CreateFileWithDir2065b";
	private String fileName = "filedir2065b";
	private byte[] writeData =  new byte[ 1024 * 5 ];
	private byte[] updateData = new byte[ 1024 * 2 ];	
	
	@BeforeClass
	private void setUp() throws IOException, ScmException {
		branSite = ScmInfo.getBranchSite();			
		wsp = ScmInfo.getWs();
		
		session = TestScmTools.createSession(branSite);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);			
		
		//clean file and dirs
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		ScmFileUtils.cleanFile(wsp, cond);
		String fullPath = "";
		for (int i = 0; i < 10; i++) {	
			fullPath = "/CreateFileWithDir2065b" + i;
			ScmDirUtils.deleteDir(ws,fullPath);			
		}
	}

	@Test(groups = { "twoSite", "fourSite"})
	private void test() throws Exception {	
		fileId = VersionUtils.createFileByStream( ws, fileName, writeData, authorName );		
		updateFileWithDirAndCheckContent( ws );				
		checkFileDir(ws, fileId, dirNames);
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() {
		try {
			if( runSuccess ){
				ScmFactory.File.deleteInstance(ws, fileId, true);
				for( String fullPath : dirNames){
					ScmDirUtils.deleteDir(ws,fullPath);
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
	
	private void updateFileWithDirAndCheckContent(ScmWorkspace ws) throws Exception{
		//update ten times and add different directory to the version file
		int times = 10;
		String fullPath = "";
		for (int i = 0; i < times; i++) {	
			fullPath = "/CreateFileWithDir2065b" + i;
			ScmDirectory scmDir = ScmDirUtils.createDir( ws, fullPath );
			dirNames.add(fullPath);
			ScmDirUtils.updateContentWithDir(ws, fileId, updateData, scmDir);			
			
			//check the update file datacontent
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			int majorVersion = file.getMajorVersion();			
			String fullFileName =  fullPath + "/" + fileName;			
			ScmDirUtils.CheckFileContentByStream(ws, fullFileName, majorVersion, updateData);			
		}			
	}	
	
	private void checkFileDir(ScmWorkspace ws, ScmId fileId, List<String> dirNames) throws ScmException{
		//all files are under the last folder
		String lastDirName = dirNames.get(9);
		ScmDirectory scmDir = ScmFactory.Directory.getInstance(ws, lastDirName);
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);		
		Assert.assertEquals(file.getDirectory().toString(), scmDir.toString());
		
		//file does not exist under the original directory
		for ( int i = 0; i < 9; i++){
			String orgDir = dirNames.get(i);
			ScmDirectory scmDirTemp =  ScmFactory.Directory.getInstance(ws, orgDir);
			ScmCursor<ScmFileBasicInfo> fileCursor = scmDirTemp.listFiles(null);
			int count = 0;
			while(fileCursor.hasNext()){
				ScmFileBasicInfo fileInfo = fileCursor.getNext();				
				Assert.assertNull(fileInfo.getFileName());
				count++;
			}
			int expDirNum = 0;
			Assert.assertEquals(count, expDirNum);;
		}
		
	}
	
	
	
		
	
	
	
}