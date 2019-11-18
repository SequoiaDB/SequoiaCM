package com.sequoiacm.scheduletask;

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
 * @FileName  SCM-1239:创建调度任务，配置cron无效
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class CreateSche_cronError1239 extends TestScmBase {
	private SiteWrapper rootSite = null;
	private SiteWrapper branSite = null;
	private WsWrapper wsp = null;
	private ScmSession ss = null;
	private final static String name = "schetask1239";
	
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
	private void test_cronError() throws Exception {
		try {
			ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
					branSite.getSiteName(), rootSite.getSiteName(), maxStayTime, queryCond);
			String cron = "600 * * * * ?";
			ScmSystem.Schedule.create(ss, wsp.getName(), 
            		ScheduleType.COPY_FILE, name, "", content, cron);
            Assert.fail("expect fail but actual succ.");
		} catch (ScmException e) {
			if (ScmError.HTTP_INTERNAL_SERVER_ERROR != e.getError()) {
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