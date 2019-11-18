package com.sequoiacm.workspace.serial;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description WorkspaceRemoveSite2182.java ws删除站点
 * @author luweikang
 * @date 2018年8月28日
 */
public class WorkspaceRemoveSite2182 extends TestScmBase{

	private ScmSession session = null;
	private static SiteWrapper site = null;
	private static SiteWrapper branchSite = null;
	private String wsName = "ws2182";	
	
	@BeforeClass
	public void setUp() throws Exception{
		site = ScmInfo.getSite();
		branchSite = ScmInfo.getBranchSite();
		session = TestScmTools.createSession(site);	
		ScmWorkspaceUtil.deleteWs(wsName, session);
	}
	
	@Test(groups = { "twoSite", "fourSite" })
	public void test() throws ScmException, InterruptedException{
		int siteNum = ScmInfo.getSiteNum();
		ScmWorkspace ws = ScmWorkspaceUtil.createWS(session, wsName, siteNum);
		ScmWorkspaceUtil.wsRemoveSite(ws, branchSite.getSiteName());
	}
	
	@AfterClass
	public void tearDown() throws Exception{
		try {
			ScmWorkspaceUtil.deleteWs(wsName, session);
		} finally {
			if (session!=null) {
				session.close();
			}
		}
	}
}

