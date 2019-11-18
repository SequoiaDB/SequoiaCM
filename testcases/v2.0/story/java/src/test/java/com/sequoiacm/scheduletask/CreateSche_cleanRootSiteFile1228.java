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
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1228:创建调度任务，类型为清理，指定站点为主站点
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class CreateSche_cleanRootSiteFile1228 extends TestScmBase {
	private SiteWrapper rootSite = null;
	private WsWrapper wsp = null;
	private final static String name = "schetask1228";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			rootSite = ScmInfo.getRootSite();
			wsp = ScmInfo.getWs();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		ScmSession ss = null;
		try {
			ss = TestScmTools.createSession(rootSite);
			
			String maxStayTime = "0d";
			BSONObject queryCond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
			ScmScheduleCleanFileContent content = 
					new ScmScheduleCleanFileContent(rootSite.getSiteName(), maxStayTime, queryCond);
            String cron = "* * * * * ?";
            ScmSystem.Schedule.create(ss, wsp.getName(), 
            		ScheduleType.CLEAN_FILE, name, "", content, cron);
            Assert.fail("expect fail but actual succ.");
		} catch (ScmException e) {
			if (ScmError.HTTP_BAD_REQUEST != e.getError()) {
				e.printStackTrace();
				throw e;
			}
		} finally {
			if (null != ss) {
				ss.close();
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
	}

}