/**
 * 
 */
package com.sequoiacm.workspace.serial;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiacm.testcommon.scmutils.VersionUtils;
import com.sequoiadb.exception.BaseException;

/**
 * test content:delete workspace 
 * testlink-case:SCM-1820
 * 
 * @author wuyan
 * @Date 2018.06.21
 * @version 1.00
 */
public class DeleteWorkSpace1820 extends TestScmBase {
	private static SiteWrapper site = null;
	private ScmSession session = null;
	private String wsName = "ws1820";
	private String fileName = "file1820";

	@BeforeClass
	private void setUp() throws ScmException, InterruptedException {			
		site = ScmInfo.getRootSite();
		session = TestScmTools.createSession(site);		
		try {
			ScmFactory.Workspace.deleteWorkspace(session, wsName, true);
			for (int i = 0; i < 10; i++) {
				Thread.sleep(1000);
				try {
					ScmFactory.Workspace.getWorkspace(wsName, session);
				} catch (ScmException e) {
					break;
				}
			}
		} catch (ScmException e) {
			if (e.getError() != ScmError.WORKSPACE_NOT_EXIST) {
				throw e;
			}
		}
	}

	@Test(groups = { "twoSite" , "fourSite"})
	private void test() throws ScmException, InterruptedException {
		int siteNum = 2;
		ScmWorkspaceUtil.createWS( session, wsName, siteNum);	
		createfile();
		deleteWorkspace() ;
	}

	@AfterClass
	private void tearDown() throws ScmException {
		try {
				ScmFactory.Workspace.deleteWorkspace(session, wsName,true);
		} catch (BaseException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
		}	
	}	
	
	private void createfile() throws InterruptedException, ScmException{	
		for (int i = 0; i < 10; i++) {
			try {
				Thread.sleep(1000);
				ScmWorkspaceUtil.wsSetPriority(session, wsName);
				break;
			} catch (ScmException e) {
				if(e.getError() != ScmError.WORKSPACE_NOT_EXIST){
					throw e;
				}
			}
		}
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, session);
		byte[] writeData = new byte[ 1024 * 200 ];	
		VersionUtils.createFileByStream( ws, fileName, writeData);	
	}
	
	private void deleteWorkspace() throws ScmException, InterruptedException{	
		//delete ws , enforced is false
		try{
			ScmFactory.Workspace.deleteWorkspace(session, wsName, false);
			Assert.fail("exist file,delete ws fail!");
		}catch (ScmException e) {
			Assert.assertEquals(e.getError(), ScmError.WORKSPACE_NOT_EMPTY,e.getMessage());
		}	
		
		//delete ws , enforced is true
		ScmFactory.Workspace.deleteWorkspace(session, wsName, true);			
		try{
			for(int j = 0; j < 10; j++){
				Thread.sleep(1000);
				ScmFactory.Workspace.getWorkspace(wsName, session);
			}
			Assert.fail("ws delete success, and ws not exist!");
		}catch (ScmException e) {
			Assert.assertEquals(e.getError(), ScmError.WORKSPACE_NOT_EXIST,e.getMessage());
		}	
	}
}
