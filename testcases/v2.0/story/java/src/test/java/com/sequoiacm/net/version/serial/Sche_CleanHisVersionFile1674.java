package com.sequoiacm.net.version.serial;

import java.io.File;
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
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:Clean the history version file 
 * testlink-case:SCM-1674
 * 
 * @author wuyan
 * @Date 2018.06.13
 * @modify By wuyan
 * @modify Date 2018.07.26
 * @version 1.10
 */

public class Sche_CleanHisVersionFile1674 extends TestScmBase {
	private static WsWrapper wsp = null;
	private SiteWrapper cleanSite = null;
	private SiteWrapper lastSite = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionL = null;
	private ScmWorkspace wsL = null;
	private List<ScmId> fileIdList = new ArrayList<>();
	private List<String> fileIdListStr = new ArrayList<>();

	private File localPath = null;
	private int fileNum = 10;
	private BSONObject condition = null;
	private ScmId scheduleId = null;
	private ScmScheduleContent content = null;
	private String cron = null;

	private String fileName = "fileVersion1674";
	private String authorName = "author1674";
	private final static String taskname = "versionfile_schetask1674";
	private int fileSize1 = 1024 * 10;
	private int fileSize2 = 1024 * 5;
	private String filePath1 = null;
	private String filePath2 = null;
	private byte[] writedata = new byte[1024 * 20];
	private boolean runSuccess = false;

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		// ready file
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		filePath1 = localPath + File.separator + "localFile_" + fileSize1 + ".txt";
		filePath2 = localPath + File.separator + "localFile_" + fileSize2 + ".txt";
		TestTools.LocalFile.createFile(filePath1, fileSize1);
		TestTools.LocalFile.createFile(filePath2, fileSize2);
		
		wsp = ScmInfo.getWs();
		// clean file
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		ScmFileUtils.cleanFile(wsp, cond);

		cleanSite = ScmNetUtils.getNonLastSite(wsp);
		lastSite = ScmNetUtils.getLastSite(wsp);
		sessionA = TestScmTools.createSession(cleanSite);
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionL = TestScmTools.createSession(lastSite);
		wsL = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionL);
		writeAndUpdateFile(wsA);
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		int currentVersion = 2;
		int historyVersion = 1;
		readFileFromM(wsL, currentVersion);
		readFileFromM(wsL, historyVersion);

		// clean history version file
		createScheduleTask(sessionA);

		// check siteinfo
		SiteWrapper[] expCurSiteList = { lastSite, cleanSite };
		VersionUtils.checkScheTaskFileSites(wsA, fileIdList, currentVersion, expCurSiteList);
		SiteWrapper[] exphHisSiteList = { lastSite };
		;
		VersionUtils.checkScheTaskFileSites(wsL, fileIdList, historyVersion, exphHisSiteList);
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() {
		try {
			ScmSystem.Schedule.delete(sessionA, scheduleId);
			if (runSuccess || TestScmBase.forceClear) {
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(wsL, fileId, true);
				}
				TestTools.LocalFile.removeFile(localPath);
				ScmScheduleUtils.cleanTask(sessionA, scheduleId);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage() + e.getStackTrace());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
			if (sessionL != null) {
				sessionL.close();
			}
		}
	}

	private void writeAndUpdateFile(ScmWorkspace ws) throws ScmException {
		for (int i = 0; i < fileNum; i++) {
			String subfileName = fileName + "_" + i;
			ScmId fileId = VersionUtils.createFileByStream(ws, subfileName, writedata, authorName);
			if (i % 2 == 0) {
				VersionUtils.updateContentByFile(ws, subfileName, fileId, filePath1);
			} else {
				VersionUtils.updateContentByFile(ws, subfileName, fileId, filePath2);
			}
			fileIdList.add(fileId);
			fileIdListStr.add(fileId.get());
		}
	}

	private void createScheduleTask(ScmSession session) throws ScmException {
		String maxStayTime = "0d";
		condition = ScmQueryBuilder.start().put(ScmAttributeName.File.FILE_ID).in(fileIdListStr).get();
		// create schedule task
		content = new ScmScheduleCleanFileContent(cleanSite.getSiteName(), maxStayTime, condition,
				ScopeType.SCOPE_HISTORY);

		cron = "* * * * * ?";

		ScmSchedule sche = ScmSystem.Schedule.create(session, wsp.getName(), ScheduleType.CLEAN_FILE, taskname, "",
				content, cron);
		scheduleId = sche.getId();

	}

	private void readFileFromM(ScmWorkspace ws, int version) throws Exception {
		for (int i = 0; i < fileNum; i++) {
			ScmId fileId = fileIdList.get(i);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			ScmFile file = ScmFactory.File.getInstance(wsL, fileId, version, 0);
			file.getContent(downloadPath);
		}
	}

}