/**
 * 
 */
package com.sequoiacm.net.version;

import java.io.IOException;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description SelectNotExistFilterHisVersionFile.java
 * @author luweikang
 * @date 2018年6月13日
 */
public class SelectErrorFilterHisVersionFile1687 extends TestScmBase {
	private boolean runSuccess = false;
	private static WsWrapper wsp = null;
	private SiteWrapper site = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private ScmId fileId = null;

	private String fileName = "fileVersion1687";
	private byte[] filedata = new byte[ 1024 * 100 ];
	private byte[] updatedata = new byte[ 1024 * 200 ];	
	
	@BeforeClass
	private void setUp() throws IOException, ScmException {
		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		
		fileId = VersionUtils.createFileByStream( ws, fileName, filedata );
		VersionUtils.updateContentByStream(ws, fileId, updatedata);
	}
	
	@Test(groups = {  "twoSite", "fourSite"})
	private void test() throws Exception {
		BSONObject errorFilter = new BasicBSONObject();
		errorFilter.put("author", fileName);
		ScmCursor<ScmFileBasicInfo> fileCursor = null;
		try{
			fileCursor = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_ALL, errorFilter);
			Assert.fail("using not exist field queries should fail");
		}catch( ScmException e){
			Assert.assertEquals(e.getErrorCode(), ScmError.INVALID_ARGUMENT.getErrorCode(), e.getMessage());
		}finally {
			if(fileCursor != null){
				fileCursor.close();
			}
		}
		runSuccess = true;
		
	}
	
	@AfterClass
	private void tearDown() {
		try {	
			if(runSuccess){
				ScmFactory.File.deleteInstance(ws, fileId, true);			
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage()+e.getStackTrace());
		} finally {
			if (session != null) {
				session.close();
			}	
		}
	}
}
