package com.sequoiacm.workspace.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description WorkspaceRemoveSite2186.java 网状ws删除中间站点 
 * @author luweikang
 * @date 2018年8月28日
 */
public class WorkspaceRemoveSite2186 extends TestScmBase{

	private ScmSession session = null;
	private static SiteWrapper site = null;
	private String wsName = "ws2183";	
	private List<ScmDataLocation> dataList = new ArrayList<>();
	
	@BeforeClass
	public void setUp() throws Exception{
		site = ScmInfo.getRootSite();
		session = TestScmTools.createSession(site);	
		ScmWorkspaceUtil.deleteWs(wsName, session);
	}
	
	@Test(groups = { "fourSite" })
	public void test() throws ScmException, InterruptedException, IOException{
		int siteNum = ScmInfo.getSiteNum();
		ScmWorkspaceUtil.createWS(session, wsName, siteNum);
		wsRemoveMiddleSite();
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
	
	private void wsRemoveMiddleSite() throws ScmException, InterruptedException {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, session);
		dataList = ws.getDataLocations();
		ScmDataLocation dataLocation = null;
		if(!(boolean)dataList.get(1).getSiteName().equals(site.getSiteName())){
			dataLocation = dataList.get(1);
			dataList.remove(1);
		}else{
			dataLocation = dataList.get(2);
			dataList.remove(2);
		}
		String removeSite = dataLocation.getSiteName();
		ScmWorkspaceUtil.wsRemoveSite(ws, removeSite);
		ScmWorkspaceUtil.wsAddSite(ws, dataLocation);
		dataList.add(dataLocation);
		checkWsSiteOrder();
	}

	private void checkWsSiteOrder() throws ScmException {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, session);
		List<ScmDataLocation> actList = ws.getDataLocations();
		Assert.assertEquals(actList, dataList);
	}
}

