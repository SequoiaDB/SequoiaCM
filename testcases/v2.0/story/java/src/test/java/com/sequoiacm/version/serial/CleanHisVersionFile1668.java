package com.sequoiacm.version.serial;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:Clean the history version file
 * testlink-case:SCM-1668
 * 
 * @author wuyan
 * @Date 2018.06.08
 * @version 1.00
 */

public class CleanHisVersionFile1668 extends TestScmBase {	
	private static WsWrapper wsp = null;
	private SiteWrapper branSite = null;
	private SiteWrapper rootSite = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionM = null;
	private ScmWorkspace wsM = null;
	private ScmId taskId = null;
	private List<String> fileIdList = new ArrayList<String>();
	private File localPath = null;
	private int fileNum = 10;
	private BSONObject condition = null;

	private String fileName = "fileVersion1668";	
	private String authorName = "author1668";
	private int fileSize1 = 1024 * 100;
	private int fileSize2 = 1024 * 5;
	private String filePath1 = null;	
	private String filePath2 = null;	
	private byte[] writedata = new byte[ 1024 * 200 ];	
	private boolean runSuccess = false;

	@BeforeClass
	private void setUp() throws IOException, ScmException  {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		// ready file
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		filePath1 = localPath + File.separator + "localFile_" + fileSize1 + ".txt";
		filePath2 = localPath + File.separator + "localFile_" + fileSize2 + ".txt";
		TestTools.LocalFile.createFile(filePath1, fileSize1);
		TestTools.LocalFile.createFile(filePath2, fileSize2);

		branSite = ScmInfo.getBranchSite();
		rootSite = ScmInfo.getRootSite();
		wsp = ScmInfo.getWs();
		
		sessionA = TestScmTools.createSession(branSite);
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionM = TestScmTools.createSession(rootSite);
		wsM = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionM);
		writeAndUpdateFile(wsA);		
	}

	@Test(groups = {  "twoSite", "fourSite"})
	private void test() throws Exception {			
		int currentVersion = 2;
		int historyVersion = 1;
		readFileFromM(wsM, currentVersion);
		readFileFromM(wsM, historyVersion);
		
		//clean history version file
		ScopeType scopeType = ScopeType.SCOPE_HISTORY;
		startCleanTaskByHistoryVerFile(wsA, sessionA, scopeType);
		
		//check siteinfo
		checkCurrentVerFileSiteInfo(wsA, currentVersion);
		checkHisVersionFileInfo(wsM, historyVersion);	
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() {
		try {	
			if ( runSuccess ){
				TestSdbTools.Task.deleteMeta(taskId);			
				for (String fileId : fileIdList) {
					ScmFactory.File.deleteInstance(wsM, new ScmId(fileId), true);
				}
				TestTools.LocalFile.removeFile(localPath);	
			}					
		} catch (Exception e) {
			Assert.fail(e.getMessage()+e.getStackTrace());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}	
			if (sessionM != null) {
				sessionM.close();
			}
		}
	}	
	
	private void writeAndUpdateFile(ScmWorkspace ws) throws ScmException{
		for (int i = 0; i < fileNum; i++) {
			String subfileName = fileName + "_" + i;
			ScmId fileId = VersionUtils.createFileByStream( ws, subfileName, writedata, authorName );
			if( i % 2 == 0 ){
				VersionUtils.updateContentByFile(ws, subfileName, fileId, filePath1);
			}else{
				VersionUtils.updateContentByFile(ws, subfileName, fileId, filePath2);
			}			
			fileIdList.add(fileId.get());			
		}
	}
	
	private void startCleanTaskByHistoryVerFile(ScmWorkspace ws, ScmSession session, ScopeType scopeType) throws Exception {
		condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).in(fileIdList).get();		
		taskId = ScmSystem.Task.startCleanTask(ws, condition, scopeType);
		
		//wait task finish
		ScmTaskUtils.waitTaskFinish(session, taskId);		
	}
	
	private void checkCurrentVerFileSiteInfo(ScmWorkspace ws, int currentVersion) throws Exception{
		//check the current version file sitelist , current version file no clean
		BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_CURRENT, condition);
		int size = 0;			
		SiteWrapper[] expCurSiteList = { rootSite, branSite };		
		while (cursor.hasNext()) {
			ScmFileBasicInfo file = cursor.getNext();			
			ScmId fileId = file.getFileId();
			VersionUtils.checkSite(ws, fileId, currentVersion, expCurSiteList);			
			size++;
		}
		cursor.close();
		int expFileNum = 10;
		Assert.assertEquals(size, expFileNum);		
	}
	
	private void checkHisVersionFileInfo(ScmWorkspace ws,int version) throws ScmException{
		//all history version file only on the rootSite		
		ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_CURRENT, condition);
		SiteWrapper[] expHisSiteList = {  rootSite };
		int size = 0;
		while (cursor.hasNext()) {
			ScmFileBasicInfo file = cursor.getNext();
			// check results
			ScmId fileId = file.getFileId();			
			VersionUtils.checkSite(ws, fileId, version, expHisSiteList);	
			size ++;
		}		
		cursor.close();	
		int expFileNums = 10;
		Assert.assertEquals(size, expFileNums);
	}
	
	private void readFileFromM(ScmWorkspace ws, int version) throws Exception {		
		for (int i = 0; i < fileNum; i++) {
			ScmId fileId = new ScmId(fileIdList.get(i));
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			ScmFile file = ScmFactory.File.getInstance(wsM, fileId, version, 0);
			file.getContent(downloadPath);
		}
	}
	
}