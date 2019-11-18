
package com.sequoiacm.testenv;

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
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.dsutils.HdfsUtils;

/**
 * @Description:clean HDFS
 * @author fanyu
 * @Date:2018年10月13日
 * @version:1.0
 */
public class CleanEnvForHdfs extends TestScmBase {
	Logger log = LoggerFactory.getLogger(CleanEnvForHBase.class);

	@BeforeClass(alwaysRun = true)
	private void setUp() {
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		List<SiteWrapper> siteList = ScmInfo.getAllSites();
		for (SiteWrapper site : siteList) {
			if (site.getDataType().equals(DatasourceType.HDFS)) {
				List<WsWrapper> wspList = ScmInfo.getAllWorkspaces();
				for (WsWrapper wsp : wspList) {
					String rootPath =  HdfsUtils.getRootPath(site, wsp);
					HdfsUtils.deletePath(site, rootPath);
				}
			} else {
				log.info(site.getSiteName() + "'s datasourcetype is not hbase,it is " + site.getDataType());
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
	}
}
