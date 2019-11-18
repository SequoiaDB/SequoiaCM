/**
 * 
 */
package com.sequoiacm.net.version.serial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description UpdateAndCleanVersionFile1699.java
 * @author luweikang
 * @date 2018年6月15日
 * @modify By wuyan
 * @modify Date: 2018.07.26
 * @version 1.10
 */
public class UpdateAndCleanVersionFile1699 extends TestScmBase {
	private boolean runSuccess = false;
	private static WsWrapper wsp = null;
	private SiteWrapper cleanSite = null;
	private SiteWrapper lastSite = null;
	private ScmSession sessionA = null;
	private ScmSession sessionL = null;
	private ScmWorkspace wsA = null;
	private ScmWorkspace wsL = null;
	private ScmId fileId = null;
	private ScmBreakpointFile sbFile = null;
	private ScmId taskId = null;

	private String fileName = "fileVersion1699";
	private byte[] filedata = new byte[ 1024 * 100 ];
	private byte[] updatedata = new byte[ 1024 * 200];	
	
	@BeforeClass
	private void setUp() throws IOException, ScmException {
		BreakpointUtil.checkDBDataSource();
		wsp = ScmInfo.getWs();
		// clean file
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
		ScmFileUtils.cleanFile(wsp, cond);
		
		cleanSite = ScmNetUtils.getNonLastSite(wsp);
		lastSite  = ScmNetUtils.getLastSite(wsp);
		
		sessionA = TestScmTools.createSession(cleanSite);
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionL = TestScmTools.createSession(lastSite);
		wsL = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionL);
		
		fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );
		sbFile = VersionUtils.createBreakpointFileByStream(wsA, fileName, updatedata);
	}
	
	@Test(groups = {  "twoSite", "fourSite"})
	private void test() throws Exception {
		readFileFromOtherSite(wsL);
		
		UpdateFileThread updateFileThread = new UpdateFileThread();
		updateFileThread.start();
		
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).is(fileId.toString()).get();
		taskId = ScmSystem.Task.startCleanTask(wsA, cond, ScopeType.SCOPE_CURRENT);
		
		Assert.assertTrue(updateFileThread.isSuccess(), updateFileThread.getErrorMsg());
		ScmTaskUtils.waitTaskFinish(sessionA, taskId);
		boolean branHasHisVersion = branHasHisVersion();
		
		if(branHasHisVersion){
			SiteWrapper[] expSites = {lastSite,cleanSite};
			VersionUtils.checkSite(wsL, fileId, 1, expSites);
			VersionUtils.CheckFileContentByStream(wsA, fileName, 1, filedata);
		}else{
			SiteWrapper[] expSites = {lastSite};
			VersionUtils.checkSite(wsL, fileId, 1, expSites);
		}
		VersionUtils.CheckFileContentByStream(wsA, fileName, 2, updatedata);
		
		runSuccess = true;
	}
	
	@AfterClass
	private void tearDown() {
		try {			
			if(runSuccess){
				ScmFactory.File.deleteInstance(wsL, fileId, true);	
				TestSdbTools.Task.deleteMeta(taskId);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage()+e.getStackTrace());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}	
			if (sessionL != null) {
				sessionL.close();
			}
		}
	}
	
	class UpdateFileThread extends TestThreadBase{

		@Override
		public void exec() throws Exception {
			Thread.sleep(210);
			ScmFile scmFile = ScmFactory.File.getInstance(wsA, fileId);
			scmFile.updateContent(sbFile);
		}
		
	}
	
	private boolean branHasHisVersion() throws ScmException{
		
		ScmFile file = ScmFactory.File.getInstance(wsA, fileId, 1, 0);
		int siteNum = file.getLocationList().size();
		
		boolean branHasHisVersion = false;
		if(siteNum > 1){
			branHasHisVersion = true;
		}
		return branHasHisVersion;
	}
	
	private void readFileFromOtherSite(ScmWorkspace ws) throws Exception {	
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent(outputStream);	
	}
}
