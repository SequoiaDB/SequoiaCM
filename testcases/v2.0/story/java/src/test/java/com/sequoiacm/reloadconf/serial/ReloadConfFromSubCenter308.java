package com.sequoiacm.reloadconf.serial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Testcase: SCM-308:配置未变更，刷新配置（从分中心）
 * @author huangxiaoni init
 * @date 2017.5.26
 */

public class ReloadConfFromSubCenter308 extends TestScmBase {
	private static String fileName = "ReloadConfFromSubCenter308";
	private static WsWrapper wsp = null;
	private static SiteWrapper rootSite = null;
	private static SiteWrapper branSite = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		rootSite = ScmInfo.getRootSite();
		wsp = ScmInfo.getWs();
		branSite = ScmInfo.getBranchSite();
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void testReloadBizConf() throws Exception {
		ScmSession session = null;
		try {
			ScmConfigOption scOpt = new ScmConfigOption(TestScmBase.gateWayList.get(0)+"/"+rootSite.getSiteServiceName());
			session = ScmFactory.Session.createSession(SessionType.NOT_AUTH_SESSION, scOpt);
			List<BSONObject> list = ScmSystem.Configuration.reloadBizConf(ServerScope.ALL_SITE, rootSite.getSiteId(),
					session);

			// check results
			List<NodeWrapper> expNodeList = ScmInfo.getNodeList();
			String errStr = "reloadBizConf failed, actual infoList after reloadBizConf: \n" + list
					+ "expect nodeInfo: \n" + expNodeList;

			Assert.assertEquals(list.size(), expNodeList.size(), errStr);

			// compare node id
			List<Integer> serverIdList = new ArrayList<>();
			for (int i = 0; i < list.size(); i++) {
				Object errormsg = list.get(i).get("errormsg");
				Assert.assertEquals(errormsg, "", errStr);
				
				int nodeId = (int) list.get(i).get("server_id");
				serverIdList.add(nodeId);
			}
			Collections.sort(serverIdList);
			for (int i = 0; i < list.size(); i++) {
				int actNodeId = serverIdList.get(i);
				int expNodeId = expNodeList.get(i).getId();
				Assert.assertEquals(actNodeId, expNodeId, errStr);
			}

			this.bizOperator();
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (session != null)
				session.close();
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
	}

	private void bizOperator() {
		ScmSession session = null;
		ScmWorkspace ws = null;
		try {
			session = TestScmTools.createSession(branSite);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName+"_"+UUID.randomUUID());
			ScmId fileId = file.save();

			ScmFactory.File.deleteInstance(ws, fileId, true);
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null)
				session.close();
		}
	}

}