package com.sequoiacm.scheduletask;

import java.util.List;

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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName  SCM-1227:创建调度任务，类型为迁移，指定源和目标站点错误
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class CreateSche_siteError1227 extends TestScmBase {
	private SiteWrapper rootSite = null;
	private final static int branSiteNum = 2;
	private List<SiteWrapper> branSites = null;
	private WsWrapper wsp = null;
	private ScmSession ss = null;
	private final static String name = "schetask1227";
	
	private final static String cron = "* * * * * ?";
	private final static String maxStayTime = "0d";
	private BSONObject queryCond = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			rootSite = ScmInfo.getRootSite();
			branSites = ScmInfo.getBranchSites(branSiteNum);
			wsp = ScmInfo.getWs();
			ss = TestScmTools.createSession(rootSite);

			queryCond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "fourSite" })
	private void test_rootsiteToBransite() throws Exception {
		try {
			ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
					rootSite.getSiteName(), branSites.get(0).getSiteName(), maxStayTime, queryCond);
            ScmSystem.Schedule.create(ss, wsp.getName(), 
            		ScheduleType.COPY_FILE, name, "", content, cron);
            Assert.fail("expect fail but actual succ.");
		} catch (ScmException e) {
			if (ScmError.HTTP_BAD_REQUEST != e.getError()) {
				e.printStackTrace();
				throw e;
			}
		}
	}

	@Test(groups = {"fourSite" })
	private void test_sameSite() throws Exception {
		try {
			ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
					rootSite.getSiteName(), rootSite.getSiteName(), maxStayTime, queryCond);
            ScmSystem.Schedule.create(ss, wsp.getName(), 
            		ScheduleType.COPY_FILE, name, "", content, cron);
            Assert.fail("expect fail but actual succ.");
		} catch (ScmException e) {
			if (ScmError.HTTP_BAD_REQUEST != e.getError()) {
				e.printStackTrace();
				throw e;
			}
		}
	}

	@Test(groups = { "fourSite" })
	private void test_diffBranSite() throws Exception {
		try {
			ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
					branSites.get(0).getSiteName(), branSites.get(1).getSiteName(), maxStayTime, queryCond);
            ScmSystem.Schedule.create(ss, wsp.getName(), 
            		ScheduleType.COPY_FILE, name, "", content, cron);
            Assert.fail("expect fail but actual succ.");
		} catch (ScmException e) {
			if (ScmError.HTTP_BAD_REQUEST != e.getError()) {
				e.printStackTrace();
				throw e;
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		if (null != ss) {
			ss.close();
		}
	}

}