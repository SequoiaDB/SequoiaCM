package com.sequoiacm.session;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @FileName SCM-403: ScmConfigOption已指定主机名和端口，addUrl重复添加
 * @Author linsuqiang
 * @Date 2017-06-13
 * @Version 1.00
 */

/*
 * 1、重复添加相同url 2、重复添加不同url
 */

public class AddUrlRepeatedly403 extends TestScmBase {
	private static SiteWrapper site = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		site = ScmInfo.getSite();
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testAddSameUrl() {
		try {
			ScmConfigOption scOpt = new ScmConfigOption();
			scOpt.addUrl(TestScmBase.gateWayList.get(0)+"/"+site);
			scOpt.addUrl(TestScmBase.gateWayList.get(0)+"/"+site);
			String expectRes = "[" + TestScmBase.gateWayList.get(0) + "/" + site+ ", " 
			+ TestScmBase.gateWayList.get(0) + "/" + site + "]";
			Assert.assertEquals(scOpt.getUrls().toString(), expectRes);
			ScmSession session = ScmFactory.Session.createSession(SessionType.NOT_AUTH_SESSION, scOpt);
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void TestAddDiffUrl() {
		try {
			NodeWrapper node1 = site.getNode();
			String host1 = node1.getHost();
			int port1 = node1.getPort();

			NodeWrapper node2 = site.getNode();
			String host2 = node2.getHost();
			int port2 = node2.getPort();

			ScmConfigOption scOpt = new ScmConfigOption();
			scOpt.addUrl(host1+":"+port1);
			scOpt.addUrl(host2+":"+port2);
			String expectRes = "[" + host1 + ":" + port1 + ", " + host2 + ":" + port2 + "]";
			Assert.assertEquals(scOpt.getUrls().toString(), expectRes);
			ScmSession session = ScmFactory.Session.createSession(SessionType.NOT_AUTH_SESSION, scOpt);
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
	}

}
