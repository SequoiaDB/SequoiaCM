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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description CreateWorkspace1818.java 创建ws名重复 
 * @author luweikang
 * @date 2018年6月22日
 */
public class CreateWorkspace1818 extends TestScmBase{
	
	private String wsName = "ws1818";
	private ScmSession session = null;
	private SiteWrapper rootSite = null;
	
	@BeforeClass
	private void setUp() throws Exception{
		
		rootSite = ScmInfo.getRootSite();
		session = TestScmTools.createSession(rootSite);
		ScmWorkspaceUtil.deleteWs(wsName, session);
	}
	
	@Test(groups = { "one", "twoSite", "fourSite" })
	private void test() throws ScmException, InterruptedException{
		int siteNum = ScmInfo.getSiteNum();
		ScmWorkspaceUtil.createWS(session, wsName, siteNum);
		try {
			ScmWorkspaceUtil.createWS(session, wsName, siteNum);
		} catch (ScmException e) {
			Assert.assertEquals(e.getError(), ScmError.WORKSPACE_EXIST, e.getMessage());
		}
	
	}
	
	@AfterClass
	private void tearDown(){
		try{
			ScmFactory.Workspace.deleteWorkspace(session, wsName, true);
		}catch( Exception e){
			Assert.fail(e.getMessage()+e.getStackTrace());
		}finally {
			if( session != null){
				session.close();
			}
		}
	}
}
