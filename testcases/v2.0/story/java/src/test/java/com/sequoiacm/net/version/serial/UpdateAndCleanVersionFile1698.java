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

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
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
 * @Description UpdateAndCleanVersionFile1698.java
 * @author luweikang
 * @date 2018年6月15日
 */
public class UpdateAndCleanVersionFile1698 extends TestScmBase {
	private boolean runSuccess = false;
	private static WsWrapper wsp = null;
	private SiteWrapper cleanSite = null;
	private SiteWrapper LastSite = null;
	private ScmSession sessionS = null;
	private ScmSession sessionT = null;
	private ScmWorkspace wsS = null;
	private ScmWorkspace wsT = null;
	private ScmId fileId = null;
	private ScmId taskId = null;

	private String fileName = "fileVersion1698";
	private byte[] filedata = new byte[ 1024 * 100 ];
	private byte[] updatedata = new byte[ 1024 * 200];	
	
	@BeforeClass
	private void setUp() throws IOException, ScmException {
		wsp = ScmInfo.getWs();
		// clean file
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
		ScmFileUtils.cleanFile(wsp, cond);		
		
		cleanSite = ScmNetUtils.getNonLastSite(wsp);
		LastSite  = ScmNetUtils.getLastSite(wsp);
		
		sessionS = TestScmTools.createSession(cleanSite);
		wsS = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionS);
		sessionT = TestScmTools.createSession(LastSite);
		wsT = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionT);
		
		fileId = VersionUtils.createFileByStream( wsS, fileName, filedata );
	}
	
	@Test(groups = {  "fourSite"})
	private void test() throws Exception {
		readFileFromOtherSite(wsT);
		
		UpdateFileThread updateFileThread = new UpdateFileThread();
		updateFileThread.start();
		
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).is(fileId.toString()).get();
		taskId = ScmSystem.Task.startCleanTask(wsS, cond, ScopeType.SCOPE_CURRENT);
		
		Assert.assertTrue(updateFileThread.isSuccess(), updateFileThread.getErrorMsg());
		ScmTaskUtils.waitTaskFinish(sessionS, taskId);
		boolean branHasHisVersion = branHasHisVersion();
		
		if(branHasHisVersion){
			SiteWrapper[] expSites = {LastSite,cleanSite};
			VersionUtils.checkSite(wsT, fileId, 1, expSites);
			VersionUtils.CheckFileContentByStream(wsS, fileName, 1, filedata);
		}else{
			SiteWrapper[] expSites = {LastSite};
			VersionUtils.checkSite(wsT, fileId, 1, expSites);
		}
		VersionUtils.CheckFileContentByStream(wsS, fileName, 2, updatedata);
		
		
		runSuccess = true;
		
	}
	
	@AfterClass
	private void tearDown() {
		try {			
			if(runSuccess){
				ScmFactory.File.deleteInstance(wsT, fileId, true);	
				TestSdbTools.Task.deleteMeta(taskId);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage()+e.getStackTrace());
		} finally {
			if (sessionS != null) {
				sessionS.close();
			}	
			if (sessionT != null) {
				sessionT.close();
			}
		}
	}
	
	class UpdateFileThread extends TestThreadBase{

		@Override
		public void exec() throws Exception {
			VersionUtils.updateContentByStream(wsS, fileId, updatedata);
		}
		
	}
	
	private boolean branHasHisVersion() throws ScmException{
		
		ScmFile file = ScmFactory.File.getInstance(wsS, fileId, 1, 0);
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
