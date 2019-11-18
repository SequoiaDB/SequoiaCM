package com.sequoiacm.version;


import java.io.IOException;
import java.util.Collection;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:the historyVersion file in both the rootSite and the branSite, 
 *               ayncCache the current version file again.
 * testlink-case:SCM-1659
 * 
 * @author wuyan
 * @Date 2018.06.05
 * @version 1.00
 */

public class AsyncCacheCurVersionFile1659 extends TestScmBase {	
	private static WsWrapper wsp = null;
	private SiteWrapper branSite = null;
	private SiteWrapper rootSite = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionM = null;
	private ScmWorkspace wsM = null;
	private ScmId fileId = null;

	private String fileName = "file1659";
	private byte[] filedata = new byte[ 1024 * 100 ];
	private byte[] updatedata = new byte[ 1024 * 200 ];	

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		branSite = ScmInfo.getBranchSite();
		rootSite = ScmInfo.getRootSite();
		wsp = ScmInfo.getWs();
		
		sessionA = TestScmTools.createSession(branSite);
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionM = TestScmTools.createSession(rootSite);
		wsM = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionM);
		
		fileId = VersionUtils.createFileByStream( wsM, fileName, filedata );
		VersionUtils.updateContentByStream(wsM, fileId, updatedata);		
	}

	@Test(groups = {  "twoSite", "fourSite"})
	private void test() throws Exception {
		int currentVersion = 2;
		int historyVersion = 1;
		//site A exists the historyVersion file
		asyncCacheFile(historyVersion);
		Collection<ScmFileLocation> firstGetSiteInfo = getSiteInfo(historyVersion);
		
		//asyncCache the current version file
		asyncCacheFile(currentVersion);
		
		//check the currentVersion file data and siteinfo
		SiteWrapper[] expCurSiteList = { rootSite, branSite };		
		VersionUtils.checkSite(wsM, fileId, currentVersion, expCurSiteList);
		VersionUtils.CheckFileContentByStream(  wsA, fileName, currentVersion, updatedata );
		
		//check the historyVersion file sitelist  
		Collection<ScmFileLocation> secondGetSiteInfo = getSiteInfo(historyVersion);
		Assert.assertEquals(firstGetSiteInfo.toString(), secondGetSiteInfo.toString(), "fisrt get siteList:"
				+firstGetSiteInfo.toString()+" 2nd get siteList:"+secondGetSiteInfo.toString());		
	}

	@AfterClass
	private void tearDown() {
		try {			
			ScmFactory.File.deleteInstance(wsA, fileId, true);			
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}	
			if (sessionM != null) {
				sessionM.close();
			}
		}
	}	
	
	private void asyncCacheFile(int majorVersion) throws Exception {	
		// the first asyncCache history version file
		ScmFactory.File.asyncCache(wsA, fileId, majorVersion, 0);
		SiteWrapper[] expHisSiteList = { rootSite, branSite };		
		VersionUtils.waitAsyncTaskFinished(wsM, fileId, majorVersion, expHisSiteList.length);	
	}
	
	private Collection<ScmFileLocation> getSiteInfo(int majorVersion) throws ScmException{
		//get the create and last access time 
		ScmFile file = ScmFactory.File.getInstance(wsA, fileId, majorVersion, 0);
		Collection<ScmFileLocation> actSiteInfo = file.getLocationList();		
		return actSiteInfo;				
	}
}