package com.sequoiacm.version.serial;


import java.io.IOException;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:Transfer the history version file, specify the version is not exist
 * testlink-case:SCM-1665
 * 
 * @author wuyan
 * @Date 2018.06.05
 * @version 1.00
 */

public class TransferHisVersionFile1665 extends TestScmBase {	
	private static WsWrapper wsp = null;
	private SiteWrapper branSite = null;
	private SiteWrapper rootSite = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionM = null;
	private ScmWorkspace wsM = null;
	private ScmId taskId = null;	
	private ScmId fileId = null;	

	private String fileName = "fileVersion1661";	
	private String authorName = "transfer1661";
	private byte[] writeData = new byte[ 1024 * 2 ];	
	private boolean runSuccess = false;

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		branSite = ScmInfo.getBranchSite();
		rootSite = ScmInfo.getRootSite();
		wsp = ScmInfo.getWs();
		
		sessionA = TestScmTools.createSession(branSite);
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionM = TestScmTools.createSession(rootSite);
		wsM = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionM);
		fileId = VersionUtils.createFileByStream( wsA, fileName, writeData, authorName );		
	}

	@Test(groups = {  "twoSite", "fourSite"})
	private void test() throws Exception {			
		int currentVersion = 1;			
		startTransferTaskByHistoryVerFile(wsA, sessionA);		

		// check task info, the task successCount is 0
		ScmTask taskInfo = ScmSystem.Task.getTask(sessionA, taskId);
		Assert.assertEquals(taskInfo.getSuccessCount(), 0);
		Assert.assertEquals(taskInfo.getActualCount(), 0);
		
		//check the currentVersion file data and siteinfo
		SiteWrapper[] expSiteList = {  branSite };		
		VersionUtils.checkSite(wsA, fileId, currentVersion, expSiteList);		
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() {
		try {
			if( runSuccess ){
				TestSdbTools.Task.deleteMeta(taskId);	
				ScmFactory.File.deleteInstance(wsM, fileId, true);
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
	
	
	private void startTransferTaskByHistoryVerFile(ScmWorkspace ws, ScmSession session) throws Exception {
		BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).is(fileId.get()).get();		
		taskId = ScmSystem.Task.startTransferTask(ws, condition, ScopeType.SCOPE_HISTORY);
		
		//wait task finish
		ScmTaskUtils.waitTaskFinish(session, taskId);		}
	
	
}