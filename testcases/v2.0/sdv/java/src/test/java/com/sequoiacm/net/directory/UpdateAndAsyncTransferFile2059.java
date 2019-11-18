package com.sequoiacm.net.directory;


import java.io.IOException;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmDirUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:create a file under a dir,than update Content of the file, than ayncTransfer the current version file
 * testlink-case:SCM-2059
 * 
 * @author wuyan
 * @Date 2018.07.12
 * @modify Date 2018.07.30
 * @version 1.10
 */

public class UpdateAndAsyncTransferFile2059 extends TestScmBase {
	private boolean runSuccess = false;	
	private SiteWrapper sourceSite = null;
	private static WsWrapper wsp = null;
	private SiteWrapper targetSite = null;
	private ScmSession sessionS = null;
	private ScmWorkspace wsS = null;	
	private ScmSession sessionT = null;
	private ScmWorkspace wsT = null;
	private ScmId fileId = null;
	private ScmDirectory scmDir1;
	private ScmDirectory scmDir2;
	private String dirBasePath = "/CreatefileWiteDir2059";
	private String fullPath1 = dirBasePath + "/2059_a/2059_b/2059_c/2059_e/2059_f/2059_g/2059_h/2059_i/2059_g/";
	private String fullPath2 = "/2059_updatedir/2059_b/2059_c/2059_update/2059_1/2059_2/2059_update3/";
	private String authorName = "CreateFileWithDir2059";
	private String fileName = "filedir2059";
	private byte[] writeData = new byte[ 1024 * 10 ];
	private byte[] updateData = new byte[ 1024 * 200 ];	

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		wsp = ScmInfo.getWs();
		List<SiteWrapper> siteList = ScmNetUtils.getSortSites(wsp);	
		sourceSite = siteList.get(0);
		targetSite = siteList.get(1);   	
		
		sessionS = TestScmTools.createSession(sourceSite);
		wsS = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionS);		
		sessionT = TestScmTools.createSession(targetSite);
		wsT = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionT);
	
		//clean file
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		ScmFileUtils.cleanFile(wsp, cond);
		ScmDirUtils.deleteDir(wsS, fullPath1);
		ScmDirUtils.deleteDir(wsS, fullPath2);
		
		//create dir
		scmDir1 = ScmDirUtils.createDir(wsS,fullPath1);	
		scmDir2 = ScmDirUtils.createDir(wsS,fullPath2);			
	}

	@Test(groups = { "twoSite", "fourSite"})
	private void test() throws Exception {			
		fileId = ScmDirUtils.createFileWithDir(wsS, fileName, writeData, authorName, scmDir1);
		ScmDirUtils.updateContentWithDir(wsS, fileId, updateData, scmDir2);		
		
		int currentVersion = 2;
		int historyVersion = 1;		
		asyncTransferCurrentVersionFile( wsS, currentVersion );	
		
		//check the currentVersion file data and siteinfo
		SiteWrapper[] expCurSiteList = { targetSite, sourceSite };		
		VersionUtils.checkSite(wsT, fileId, currentVersion, expCurSiteList);
		VersionUtils.CheckFileContentByStream(  wsS, fullPath2 + fileName, currentVersion, updateData );		
		
		
		//check the historyVersion file ,only on the rootSite,dir change to fullpath2
		SiteWrapper[] expHisSiteList = { sourceSite};
		VersionUtils.checkSite(wsS, fileId, historyVersion, expHisSiteList);
		VersionUtils.CheckFileContentByStream(  wsS, fullPath2 + fileName, historyVersion, writeData );
		
		//check the file dir attribute 
		checkFileDirAttr(wsS, scmDir1,scmDir2, currentVersion,historyVersion);		
		
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() {
		try {
			if( runSuccess ){
				ScmFactory.File.deleteInstance(wsS, fileId, true);	
				ScmDirUtils.deleteDir(wsT,fullPath1);
				ScmDirUtils.deleteDir(wsT,fullPath2);
			}			
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionS != null) {
				sessionS.close();
			}	
			if (sessionT != null) {
				sessionT.close();
			}			
		}
	}		
	
	private void asyncTransferCurrentVersionFile(ScmWorkspace ws, int majorVersion) throws Exception {		
		ScmFactory.File.asyncTransfer(ws, fileId, majorVersion, 0);
			
		//wait task finished
		int sitenums = 2;
		VersionUtils.waitAsyncTaskFinished(ws, fileId, majorVersion, sitenums);			
	}
	
	
	private void checkFileDirAttr(ScmWorkspace ws, ScmDirectory oldDir, 
			ScmDirectory newDir,int currentVersion,int historyVersion) throws ScmException{
		//check the current file
		ScmFile file = ScmFactory.File.getInstance(ws, fileId, currentVersion, 0);		
		Assert.assertEquals(file.getDirectory().toString(), newDir.toString());
		
		//check the history file,move to the update file directory:fullPath2
		ScmFile hisFile = ScmFactory.File.getInstance(ws, fileId, historyVersion, 0);		
		Assert.assertEquals(hisFile.getDirectory().toString(), newDir.toString());	
		try {
            ScmFactory.File.getInstanceByPath(ws, fullPath1 + fileName);
	    	Assert.fail("get  file must bu fail!");
	    } catch (ScmException e) {	    	
			if ( ScmError.FILE_NOT_FOUND != e.getError()) {
				Assert.fail("expErrorCode:-262  actError:"+e.getError()+e.getMessage());
			}
		}		
	}
	
}