package com.sequoiacm.net.scheduletask;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName   SCM-1236:创建调度任务，指定的ws不存在
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class CreateSche_wsOrSiteNotExist1236 extends TestScmBase {
	private static final Logger logger = Logger.getLogger(CreateSche_wsOrSiteNotExist1236.class);
	
	private SiteWrapper rootSite = null;
	private SiteWrapper branSite = null;
	private WsWrapper wsp = null;
	private ScmSession ss = null;
	private final static String name = "schetask1236";
	
	private final static String cron = "* * * * * ?";
	private final static String maxStayTime = "0d";
	private BSONObject queryCond = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			rootSite = ScmInfo.getRootSite();
			branSite = ScmInfo.getBranchSite();
			wsp = ScmInfo.getWs();
			ss = TestScmTools.createSession(rootSite);

			queryCond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test_wsNotEixst() throws Exception {
		try {
			
			ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
					branSite.getSiteName(), rootSite.getSiteName(), maxStayTime, queryCond);
            ScmSystem.Schedule.create(ss, "wsNotExist", 
            		ScheduleType.COPY_FILE, name, "", content, cron);
            Assert.fail("expect fail but actual succ.");
		} catch (ScmException e) {
			logger.info("ws not exist, errorMsg = [" + e.getError() + "]");
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test_sourceSiteNotExist() throws Exception {
		try {
			ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
					"siteNotEixst", rootSite.getSiteName(), maxStayTime, queryCond);
            ScmSystem.Schedule.create(ss, wsp.getName(), 
            		ScheduleType.COPY_FILE, name, "", content, cron);
            Assert.fail("expect fail but actual succ.");
		} catch (ScmException e) {
			logger.info("source site not exist, errorMsg = [" + e.getError() + "]");
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test_targetSiteNotExist() throws Exception {
		try {
			ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
					branSite.getSiteName(), "siteNotEixst", maxStayTime, queryCond);
            ScmSystem.Schedule.create(ss, wsp.getName(), 
            		ScheduleType.COPY_FILE, name, "", content, cron);
            Assert.fail("expect fail but actual succ.");
		} catch (ScmException e) {
			logger.info("target site not exist, errorMsg = [" + e.getError() + "]");
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		if (null != ss) {
			ss.close();
		}
	}

}