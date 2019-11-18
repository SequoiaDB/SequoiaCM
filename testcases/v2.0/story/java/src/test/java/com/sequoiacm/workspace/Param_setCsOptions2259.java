
package com.sequoiacm.workspace;

import java.util.ArrayList;
import java.util.List;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.bizconf.ScmCephS3DataLocation;
import com.sequoiacm.client.element.bizconf.ScmCephSwiftDataLocation;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHbaseDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHdfsDataLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description  SCM-2259:setClOptions参数校验
 * @author fanyu
 * @date 2018年09月26日
 */
public class Param_setCsOptions2259 extends TestScmBase {

	private String wsName1 = "ws2259_1";
	private String wsName2 = "ws2259_2";
	private ScmSession session = null;
	private SiteWrapper site = null;

	@BeforeClass
	private void setUp() throws Exception {
		site = ScmInfo.getRootSite();
		session = TestScmTools.createSession(site);
		ScmWorkspaceUtil.deleteWs(wsName1, session);
		ScmWorkspaceUtil.deleteWs(wsName2, session);
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void testValueIsWrong() throws ScmInvalidArgumentException, InterruptedException {
		ScmSdbMetaLocation scmMetaLocation = new ScmSdbMetaLocation(site.getSiteName(), ScmShardingType.YEAR,
				TestSdbTools.getDomainNames(site.getMetaDsUrl()).get(0));
		scmMetaLocation.setCsOptions(new BasicBSONObject().append("LobPageSize", 4097));
		// create workspace
		try {
			createWS(session, wsName1, ScmInfo.getSiteNum(), scmMetaLocation);
			Assert.fail("exp fail but act success");
		} catch (ScmException e) {
			if (e.getError() != ScmError.CONFIG_SERVER_ERROR) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void testKeyIsWrong() throws ScmInvalidArgumentException, InterruptedException {
		ScmSdbMetaLocation scmMetaLocation = new ScmSdbMetaLocation(site.getSiteName(), ScmShardingType.YEAR,
				TestSdbTools.getDomainNames(site.getMetaDsUrl()).get(0));
		scmMetaLocation.setCsOptions(new BasicBSONObject().append("Partition1", 16));

		// create workspace
		try {
			createWS(session, wsName2, ScmInfo.getSiteNum(), scmMetaLocation);
			Assert.fail("exp fail but act success");
		} catch (ScmException e) {
			if (e.getError() != ScmError.CONFIG_SERVER_ERROR) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@AfterClass
	private void tearDown() {
		try {
			ScmWorkspaceUtil.deleteWs(wsName1, session);
			ScmWorkspaceUtil.deleteWs(wsName2, session);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage() + e.getStackTrace());
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	private void createWS(ScmSession session, String wsName, int siteNum, ScmMetaLocation scmMetaLocation)
			throws ScmException, InterruptedException {

		SiteWrapper rootSite = ScmInfo.getRootSite();
		List<SiteWrapper> siteList = new ArrayList<SiteWrapper>();
		List<ScmDataLocation> scmDataLocationList = new ArrayList<ScmDataLocation>();
		if (siteNum > 1) {
			siteList = ScmInfo.getBranchSites(siteNum - 1);
		} else if (siteNum < 1) {
			throw new IllegalArgumentException("error, create ws siteNum can't equal " + siteNum);
		}
		siteList.add(rootSite);
		for (int i = 0; i < siteList.size(); i++) {
			String siteName = siteList.get(i).getSiteName();
			String dataType = siteList.get(i).getDataType().toString();
			switch (dataType) {
			case "sequoiadb":
				String domainName = TestSdbTools.getDomainNames(siteList.get(i).getDataDsUrl()).get(0);
				scmDataLocationList.add(new ScmSdbDataLocation(siteName, domainName));
				break;
			case "hbase":
				scmDataLocationList.add(new ScmHbaseDataLocation(siteName));
				break;
			case "hdfs":
				scmDataLocationList.add(new ScmHdfsDataLocation(siteName));
				break;
			case "ceph_s3":
				scmDataLocationList.add(new ScmCephS3DataLocation(siteName));
				break;
			case "ceph_swift":
				scmDataLocationList.add(new ScmCephSwiftDataLocation(siteName));
				break;
			default:
				Assert.fail("dataSourceType not match: " + dataType);
			}
		}

		ScmWorkspaceConf conf = new ScmWorkspaceConf();
		conf.setDataLocations(scmDataLocationList);
		conf.setMetaLocation(scmMetaLocation);
		conf.setName(wsName);
		ScmFactory.Workspace.createWorkspace(session, conf);
	}
}
