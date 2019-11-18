/**
 * 
 */
package com.sequoiacm.net.version.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description ScheduleAsyncTransferAllVersionFile1672.java
 * @author luweikang
 * @date 2018年6月13日
 * @modify By wuyan
 * @modify Date 2018.07.26
 * @version 1.10
 */
public class ScheduleAsyncTransferAllVersionFile1672 extends TestScmBase {
	private boolean runSuccess = false;
	private static WsWrapper wsp = null;
	private SiteWrapper sourceSite = null;
	private SiteWrapper targetSite = null;
	private ScmSession sessionS = null;
	private ScmSession sessionT = null;
	private ScmWorkspace wsS = null;
	private ScmWorkspace wsT = null;
	private ScmId fileId1 = null;
	private ScmId fileId2 = null;
	private ScmId scheduleId = null;
	private List<String> fileIdList = new ArrayList<>();

	private String fileName1 = "fileVersion1672_1";
	private String fileName2 = "fileVersion1672_2";
	private String authorName = "fileVersion1672";
	private String scheduleName = "schedule1672";
	private byte[] filedata = new byte[1024 * 100];
	private byte[] updatedata = new byte[1024 * 200];

	@BeforeClass
	private void setUp() throws IOException, ScmException {		
		wsp = ScmInfo.getWs();
		// clean file
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		ScmFileUtils.cleanFile(wsp, cond);
		
		List<SiteWrapper> siteList = ScmNetUtils.getRandomSites(wsp);
		sourceSite = siteList.get(0);
		targetSite = siteList.get(1);
		sessionS = TestScmTools.createSession(sourceSite);
		wsS = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionS);
		sessionT = TestScmTools.createSession(targetSite);
		wsT = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionT);

		fileId1 = VersionUtils.createFileByStream(wsS, fileName1, filedata, authorName);
		fileId2 = VersionUtils.createFileByStream(wsS, fileName2, filedata, authorName);
		VersionUtils.updateContentByStream(wsS, fileId1, updatedata);
		VersionUtils.updateContentByStream(wsS, fileId2, updatedata);
		VersionUtils.updateContentByStream(wsS, fileId2, updatedata);
		fileIdList.add(fileId1.toString());
		fileIdList.add(fileId2.toString());

	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {

		createScheduleTask();

		VersionUtils.waitAsyncTaskFinished(wsT, fileId1, 1, 2);
		VersionUtils.waitAsyncTaskFinished(wsT, fileId1, 2, 2);
		VersionUtils.waitAsyncTaskFinished(wsT, fileId2, 1, 2);
		VersionUtils.waitAsyncTaskFinished(wsT, fileId2, 2, 2);
		VersionUtils.waitAsyncTaskFinished(wsT, fileId2, 3, 2);

		SiteWrapper[] expSites = { targetSite, sourceSite };
		VersionUtils.checkSite(wsT, fileId1, 1, expSites);
		VersionUtils.checkSite(wsT, fileId1, 2, expSites);
		VersionUtils.checkSite(wsT, fileId2, 1, expSites);
		VersionUtils.checkSite(wsT, fileId2, 2, expSites);
		VersionUtils.checkSite(wsT, fileId2, 3, expSites);

		runSuccess = true;
	}

	@AfterClass()
	private void tearDown() {
		try {
			ScmSystem.Schedule.delete(sessionS, scheduleId);
			if (runSuccess) {
				ScmFactory.File.deleteInstance(wsT, fileId1, true);
				ScmFactory.File.deleteInstance(wsT, fileId2, true);
				ScmScheduleUtils.cleanTask(sessionS, scheduleId);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage() + e.getStackTrace());
		} finally {
			if (sessionS != null) {
				sessionS.close();
			}
			if (sessionT != null) {
				sessionT.close();
			}
		}
	}

	private void createScheduleTask() throws ScmException {
		BSONObject queryCond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).in(fileIdList).get();
		ScmScheduleContent content = new ScmScheduleCopyFileContent(sourceSite.getSiteName(), targetSite.getSiteName(),
				"0d", queryCond, ScopeType.SCOPE_ALL);
		String cron = "* * * * * ?";
		ScmSchedule sche = ScmSystem.Schedule.create(sessionS, wsp.getName(), ScheduleType.COPY_FILE, scheduleName, "",
				content, cron);
		scheduleId = sche.getId();
	}
}
