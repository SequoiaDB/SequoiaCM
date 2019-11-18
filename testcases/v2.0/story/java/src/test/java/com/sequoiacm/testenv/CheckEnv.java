
package com.sequoiacm.testenv;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:检查运行环境是否正常
 * @Date:2018年4月20日
 * @version:1.0
 */
public class CheckEnv extends TestScmBase {
	private List<SiteWrapper> siteList = null;
	private List<ScmSession> ssList = null;
	private List<WsWrapper> wsList = null;
	private String fileName = "CheckEnv";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		siteList = ScmInfo.getAllSites();
		wsList = ScmInfo.getAllWorkspaces();
		ssList = new ArrayList<ScmSession>();
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test_checkScmServer() throws Exception {
		for (SiteWrapper site : siteList) {
			this.checkScmServer(site);
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" }, dependsOnMethods = { "test_checkScmServer" })
	private void test_checkPrivilege() throws Exception {
		for (SiteWrapper site : siteList) {
			for (WsWrapper ws : wsList) {
				checkPrivilege(site, ws);
			}
		}
	}

	@Test(groups = { "twoSite", "fourSite" }, dependsOnMethods = { "test_checkScmServer" })
	private void test_checkScheduleServer() throws Exception {
		List<SiteWrapper> siteList = ScmInfo.getBranchSites(ScmInfo.getAllSites().size() - 1);
		for (SiteWrapper branchsite : siteList) {
			for (WsWrapper wsp : wsList) {
				this.checkScheduleServer(branchsite, wsp);
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		for (ScmSession ss : ssList) {
			if (ss != null) {
				ss.close();
			}
		}
	}

	private void checkScmServer(SiteWrapper site) throws Exception {
		int i = 0;
		int tryNum = 10 * 30; // 10min
		int interval = 2 * 1000; // 2s
		for (; i < tryNum; i++) {
			try {
				ScmSession session = TestScmTools.createSession(site);
				ssList.add(session);
				break;
			} catch (ScmException e) {
				if (ScmError.HTTP_NOT_FOUND != e.getError()) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
				if (i == tryNum - 1) {
					e.printStackTrace();
					Assert.fail("try is over," + e.getMessage());
				}
				Thread.sleep(interval);
			}
		}
	}

	private void checkScheduleServer(SiteWrapper branSite, WsWrapper wsp) throws Exception {
		ScmSession ss = null;
		ScmId scheduleId = null;
		try {
			// login
			//SiteWrapper branSite = ScmInfo.getBranchSite();
			//WsWrapper wsp = ScmInfo.getWs();
			ss = TestScmTools.createSession(branSite);

			// create schedule
			int maxRetryTimes = 10 * 6; // 10min
			int retryTimes = 0;
			int sleepTime = 10 * 1000; // 10s
			BSONObject queryCond = ScmQueryBuilder.start("checkenv").is("0").get();
			ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(branSite.getSiteName(), "0d",
					queryCond);
			while (true) {
				try {
					ScmSchedule sche = ScmSystem.Schedule.create(ss, wsp.getName(), ScheduleType.CLEAN_FILE, "checkenv",
							"", content, "* * * * * ? 2029");
					scheduleId = sche.getId();
					ScmSystem.Schedule.delete(ss, scheduleId);
					break;
				} catch (ScmException e) {
					if (retryTimes < maxRetryTimes) {
						retryTimes++;
						Thread.sleep(sleepTime);
					} else {
						e.printStackTrace();
						throw new Exception("Failed to check schedule server, timeout.");
					}
				}
			}
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (null != ss)
				ss.close();
		}
	}

	private void checkPrivilege(SiteWrapper site, WsWrapper wsp) throws InterruptedException {
		int i = 0;
		for (; i < 60; i++) {
			ScmSession session = null;
			try {
				Thread.sleep(1000);
				session = TestScmTools.createSession(site);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
				ScmFile writefile = ScmFactory.File.createInstance(ws);
				writefile.setFileName(fileName + "_" + UUID.randomUUID());
				writefile.setAuthor(fileName);
				ScmId fileId = writefile.save();
				ScmFactory.File.deleteInstance(ws, fileId, true);
				break;
			} catch (ScmException e) {
				if (e.getError() != ScmError.OPERATION_UNSUPPORTED || i == 59) {
					e.printStackTrace();
					Assert.fail(e.getMessage() + ",i = " + i);
				}
			}finally{
				if(session != null){
					session.close();
				}
			}
		}
	}
}
