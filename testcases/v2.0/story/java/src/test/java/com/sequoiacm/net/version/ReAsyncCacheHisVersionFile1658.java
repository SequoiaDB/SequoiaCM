package com.sequoiacm.net.version;


import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:the historyVersion file in both the rootSite and the branSite, 
 *               ayncCache the history version file again.
 * testlink-case:SCM-1658
 * 
 * @author wuyan
 * @Date 2018.06.05
 * @version 1.00
 */

public class ReAsyncCacheHisVersionFile1658 extends TestScmBase {	
	private static WsWrapper wsp = null;
	private SiteWrapper cacheSite = null;
	private SiteWrapper sourceSite = null;
	private ScmSession sessionC = null;
	private ScmWorkspace wsC = null;
	private ScmSession sessionS = null;
	private ScmWorkspace wsS = null;
	private ScmId fileId = null;

	private String fileName = "file1658";
	private byte[] filedata = new byte[ 1024 * 100 ];
	private byte[] updatedata = new byte[ 1024 * 200 ];	

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		wsp = ScmInfo.getWs();
		// clean file
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
		ScmFileUtils.cleanFile(wsp, cond);

		List<SiteWrapper> siteList = ScmNetUtils.getRandomSites(wsp);
		cacheSite = siteList.get(0);
		sourceSite = siteList.get(1);
		
		sessionC = TestScmTools.createSession(cacheSite);
		wsC = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionC);
		sessionS = TestScmTools.createSession(sourceSite);
		wsS = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionS);
		
		fileId = VersionUtils.createFileByStream( wsS, fileName, filedata );
		VersionUtils.updateContentByStream(wsS, fileId, updatedata);
	}

	@Test(groups = {  "twoSite", "fourSite"})
	private void test() throws Exception {
		int historyVersion = 1;
		//asyncCache history file
		AsyncCacheHisVersionFile(historyVersion);
		Collection<ScmFileLocation> firstSiteInfo = getSiteInfo(historyVersion);
		// asyncCache history file once again
		AsyncCacheHisVersionFile(historyVersion);
		Collection<ScmFileLocation> secondSiteInfo = getSiteInfo(historyVersion);		 
		
		//check the siteinfo is the same
		Assert.assertEquals(firstSiteInfo.toString(), secondSiteInfo.toString(), "fisrt get siteList:"
							+firstSiteInfo.toString()+" 2nd get siteList:"+secondSiteInfo.toString());
		//check the history file data		
		VersionUtils.CheckFileContentByStream(  wsC, fileName, historyVersion, filedata );		
	}

	@AfterClass
	private void tearDown() {
		try {			
			ScmFactory.File.deleteInstance(wsC, fileId, true);			
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionC != null) {
				sessionC.close();
			}	
			if (sessionS != null) {
				sessionS.close();
			}
		}
	}	
	
	private void AsyncCacheHisVersionFile(int majorVersion) throws Exception {	
		// the first asyncCache history version file
		ScmFactory.File.asyncCache(wsC, fileId, majorVersion, 0);
		SiteWrapper[] expHisSiteList = { sourceSite, cacheSite };		
		VersionUtils.waitAsyncTaskFinished(wsS, fileId, majorVersion, expHisSiteList.length);	
	}
	
	private Collection<ScmFileLocation> getSiteInfo(int majorVersion) throws ScmException{
		//get the create and last access time 
		ScmFile file = ScmFactory.File.getInstance(wsC, fileId, majorVersion, 0);
		Collection<ScmFileLocation> actSiteInfo = file.getLocationList();		
		return actSiteInfo;				
	}
}