package com.sequoiacm.net.version.concurrent;



import java.io.IOException;
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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content: update Content and asyncCache the same file concurrently:
 *               a.update content
 *               b.asyncCache the file
 * testlink-case:SCM-1694
 * 
 * @author wuyan
 * @Date 2018.06.15
 * @modify Date 2018.07.27
 * @version 1.10
 */

public class UpdateAndAsyncCacheFile1694 extends TestScmBase {
	private boolean runSuccess = false;
	private static WsWrapper wsp = null;
	private SiteWrapper cacheSite = null;
	private SiteWrapper sourceSite = null;	
	private ScmSession sessionS = null;		
	private ScmWorkspace wsS = null;
	private ScmId fileId = null;

	private String fileName = "versionfile1694";
	private String authorName = "author1694";
	private byte[] writeData = new byte[ 1024 * 200 ];	

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		wsp = ScmInfo.getWs();
		// clean file
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
		ScmFileUtils.cleanFile(wsp, cond);

		List<SiteWrapper> siteList = ScmNetUtils.getRandomSites(wsp);
		cacheSite = siteList.get(0);
		sourceSite = siteList.get(1);
		
		sessionS = TestScmTools.createSession(sourceSite);
		wsS = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionS);		
		
		fileId = VersionUtils.createFileByStream( wsS, fileName, writeData, authorName );		
	}

	@Test(groups = {  "twoSite", "fourSite"})
	private void test() throws Exception {
		int updateSize = 1024 * 180 ;		
		byte[] updateData = new byte[ updateSize ];	
		
		AsyncCacheFile asyncCacheFile = new AsyncCacheFile();
		UpdateFile updateFile = new UpdateFile(updateData);
		asyncCacheFile.start();
		updateFile.start();
		
		Assert.assertTrue( updateFile.isSuccess(), updateFile.getErrorMsg());
		Assert.assertTrue( asyncCacheFile.isSuccess(), asyncCacheFile.getErrorMsg());
		
		checkUpdateAndAsyncCacheFileResult( wsS, updateData );		
		runSuccess = true;
		
	}

	@AfterClass
	private void tearDown() {		
		try {	
			if( runSuccess ){
				ScmFactory.File.deleteInstance(wsS, fileId, true);
			}						
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionS != null) {
				sessionS.close();
			}			
		}
	}	
	
	public class UpdateFile extends TestThreadBase {
		private byte[] updateData;
		
		public UpdateFile(byte[] updateData) {
			this.updateData = updateData;
		}
		
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(sourceSite);				
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);				
				VersionUtils.updateContentByStream(ws, fileId, updateData);					
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}
	
	public class AsyncCacheFile extends TestThreadBase {		
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(cacheSite);				
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);				
				
				int sitenums = 2;				
				ScmFile file = ScmFactory.File.getInstance(ws, fileId);
				int currentVersion = file.getMajorVersion();
				ScmFactory.File.asyncCache(ws, fileId, currentVersion, 0);					
				VersionUtils.waitAsyncTaskFinished(ws, fileId, currentVersion,sitenums);				
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}
	
	
	private void checkUpdateAndAsyncCacheFileResult( ScmWorkspace ws, byte[] updateData ) throws Exception{		
		int currentVersion = 2;
		int historyVersion = 1;		
			
		//asyncCache only one file :current version file or history version file
		ScmFile file = ScmFactory.File.getInstance(ws, fileId, currentVersion, 0);			
		int actSiteNum = file.getLocationList().size();
		SiteWrapper[] expSiteList1 = { sourceSite};
		SiteWrapper[] expSiteList2 = { sourceSite, cacheSite};
		if ( actSiteNum == expSiteList1.length ){			
			VersionUtils.checkSite(ws, fileId, currentVersion, expSiteList1);
			VersionUtils.checkSite(ws, fileId, historyVersion, expSiteList2);
		}else if ( actSiteNum == expSiteList2.length ){
			VersionUtils.checkSite(ws, fileId, currentVersion, expSiteList2);
			VersionUtils.checkSite(ws, fileId, historyVersion, expSiteList1);
		}else {
			Assert.fail("check Sitelist error!");
		}	
		
		//check the update result
		VersionUtils.CheckFileContentByStream(  ws, fileName, currentVersion, updateData );
		VersionUtils.CheckFileContentByStream(  ws, fileName, historyVersion, writeData );
	}	
}