
package com.sequoiacm.testenv;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiadb.base.Sequoiadb;

/**
 * @Description:clean sdb
 * @author fanyu
 * @Date:2018年10月13日
 * @version:1.0
 */
public class CleanEnvForSdb extends TestScmBase {
	Logger log = LoggerFactory.getLogger(CleanEnvForHBase.class);

	@BeforeClass(alwaysRun = true)
	private void setUp() {
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		List<SiteWrapper> siteList = ScmInfo.getAllSites();
		for (SiteWrapper site : siteList) {
			if (site.getDataType().equals(DatasourceType.SEQUOIADB)) {
				deleteCS(site);
			} else {
				log.info(site.getSiteName() + "'s datasourcetype is not hbase,it is " + site.getDataType());
			}
		}
	}

	private void deleteCS(SiteWrapper site) {
		List<String> csNames = getCsNames(site);
		Sequoiadb sdb = null;
		try {
			sdb = TestSdbTools.getSdb(site.getDataDsUrl());
			for (String csName : csNames) {
				sdb.dropCollectionSpace(csName);
			}
		} catch (Exception e) {
			log.info("delete cs failed, e = " + e.getMessage());
		} finally {
			if (sdb != null) {
				sdb.close();
			}
		}
	}

	private List<String> getCsNames(SiteWrapper site) {
		Sequoiadb sdb = null;
		List<String> lodCSNames = null;
		try {
			sdb = TestSdbTools.getSdb(site.getDataDsUrl());
			List<String> csNames = sdb.getCollectionSpaceNames();
			lodCSNames = new ArrayList<String>();
			for (String name : csNames) {
				if (name.contains("LOB")) {
					lodCSNames.add(name);
				}
			}
		} catch (Exception e) {
			log.info("get cs names failed, e = " + e.getMessage());
		} finally {
			if (sdb != null) {
				sdb.close();
			}
		}
		return lodCSNames;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
	}
}
