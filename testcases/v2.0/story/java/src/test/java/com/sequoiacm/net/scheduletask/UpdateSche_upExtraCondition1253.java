package com.sequoiacm.net.scheduletask;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @FileName   SCM-1253:更新content下的extra_condition
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class UpdateSche_upExtraCondition1253 extends TestScmBase {
	private boolean runSuccess = false;
	private ScmSession ssA = null;
	private ScmWorkspace wsA = null;
	private SiteWrapper rootSite = null;
	private SiteWrapper branSite = null;
	private WsWrapper wsp = null;
	
	private final static int fileNum = 10;
	private final static int fileSize = 100;
	private final static String name = "schetask1253";
	private List<ScmId> fileIds = new ArrayList<>();
	private File localPath = null;
	private String filePath = null;
	private BSONObject queryCond = null;
	
	private ScmId scheduleId = null;
	private final static String cron = "* * * * * ?";
	private final static String maxStayTime = "0d";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			// ready local file
			localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
			filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);

			// get site and workspace, create session
			rootSite = ScmInfo.getRootSite();
			branSite = ScmInfo.getBranchSite();
			wsp = ScmInfo.getWs();
			ssA = TestScmTools.createSession(branSite);
			wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), ssA);
			
			// clean environment
			queryCond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
			ScmFileUtils.cleanFile(wsp, queryCond);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			// ready scmFile
			this.writeScmFile();
			// create schedule task, type is clean, and check
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
			this.createScheduleTask(cond);
			SiteWrapper[] expSites1 = {rootSite, branSite};
			ScmScheduleUtils.checkScmFile(wsA, fileIds, 0, fileNum / 2, expSites1);
			
			SiteWrapper[] expSites2 = {branSite};
			ScmScheduleUtils.checkScmFile(wsA, fileIds, fileNum / 2, fileNum, expSites2);
			
		
			// update schedule content[extra_condition]
			cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name + "_new").get();
			this.updateExtraCondition(cond);
			SiteWrapper[] expSites3 = {rootSite, branSite};
			ScmScheduleUtils.checkScmFile(wsA, fileIds, fileNum / 2, fileNum, expSites3);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {			
			ScmSystem.Schedule.delete(ssA, scheduleId);			
			if (runSuccess || TestScmBase.forceClear) {
				queryCond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).in(name, name + "_new").get();
				ScmFileUtils.cleanFile(wsp, queryCond);
				
				TestTools.LocalFile.removeFile(localPath);
				ScmScheduleUtils.cleanTask(ssA, scheduleId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (ssA != null) {
				ssA.close();
			}
		}
	}
	
	private void writeScmFile() throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR)-3);
		for (int i = 0; i < fileNum / 2; i++) {
			ScmFile file = ScmFactory.File.createInstance(wsA);
			file.setContent(filePath);
			file.setFileName(name + "_" + i);
			file.setCreateTime(calendar.getTime());
			file.setAuthor(name);
			ScmId fileId = file.save();
			fileIds.add(fileId);
		}
		
		for (int i = fileNum / 2; i < fileNum; i++) {
			ScmFile file = ScmFactory.File.createInstance(wsA);
			file.setContent(filePath);
			file.setFileName(name + "_" + i);
			file.setAuthor(name + "_new");
			file.setCreateTime(calendar.getTime());
			ScmId fileId = file.save();
			fileIds.add(fileId);
		}
	}
	
	private void createScheduleTask(BSONObject cond) {
		try {
			ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
					branSite.getSiteName(), rootSite.getSiteName(), maxStayTime, cond);
            ScmSchedule sche = ScmSystem.Schedule.create(ssA, wsp.getName(), 
            		ScheduleType.COPY_FILE, name, "", content, cron);
            scheduleId = sche.getId();
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	private void updateExtraCondition(BSONObject cond) {
		try {
			ScmSchedule sche = ScmSystem.Schedule.get(ssA, scheduleId);	
			ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
					branSite.getSiteName(), rootSite.getSiteName(), maxStayTime, cond);
			sche.updateContent(content);
            
            // check shedule info
			ScmSchedule sche2 = ScmSystem.Schedule.get(ssA, scheduleId);
			Assert.assertEquals(sche2.getId(), scheduleId);
			Assert.assertEquals(sche2.getContent(), content);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

}