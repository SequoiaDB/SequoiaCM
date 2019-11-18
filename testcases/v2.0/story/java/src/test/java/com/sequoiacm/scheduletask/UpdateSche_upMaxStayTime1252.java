package com.sequoiacm.scheduletask;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @FileName SCM-1252:更新存活时间
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class UpdateSche_upMaxStayTime1252 extends TestScmBase {
	private boolean runSuccess = false;	
	private SiteWrapper rootSite = null;
	private SiteWrapper branSite = null;
	private ScmSession session = null;
	private String wsName = "ws1252";
	private final static int fileNum = 10;
	private final static int fileSize = 100;
	private final static String name = "sche1252";
	private List<ScmId> fileIds = new ArrayList<>();
	private File localPath = null;
	private String filePath = null;
	private BSONObject queryCond = null;
	
	private ScmId scheduleId = null;

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
			session = TestScmTools.createSession(rootSite);
			ScmWorkspaceUtil.deleteWs(wsName,session);
			ScmWorkspaceUtil.createWS(session,wsName,ScmInfo.getSiteNum());
			ScmWorkspaceUtil.wsSetPriority(session,wsName);
			queryCond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		Calendar cal = Calendar.getInstance();
		try {
			// write scmFile
			cal.set(Calendar.MONTH, cal.get(Calendar.MONTH)-1);
			this.readyScmFile(0, fileNum / 2,cal.getTime());
			// write scmFile again
			cal.set(Calendar.YEAR, cal.get(Calendar.YEAR)-1);
			this.readyScmFile(fileNum / 2, fileNum,cal.getTime());
			
			// create schedule task, type is copy, and check
			String maxStayTime = "366d";
			this.createScheduleTask(maxStayTime);
			
			SiteWrapper[] expSites1 = {rootSite, branSite};
			this.checkScmFile(fileNum / 2, fileNum, expSites1);
			
			SiteWrapper[] expSites2 = {branSite};
			this.checkScmFile(0, fileNum / 2, expSites2);
			
			// update schedule content[extra_condition]
			maxStayTime = "0d";
			this.updateScheMaxStayTime(maxStayTime);
			
			SiteWrapper[] expSites3 = {rootSite, branSite};
			this.checkScmFile(0, fileNum, expSites3);
			
			//update schedule type,copy to clean
			maxStayTime = "366d";
			updateScheTaskToCleanTask(maxStayTime);
			
			SiteWrapper[] expSites4 = {rootSite};
			this.checkScmFile(fileNum / 2, fileNum, expSites4);
			
			SiteWrapper[] expSites5 = {rootSite, branSite};
			this.checkScmFile(0, fileNum/2, expSites5);
			
			maxStayTime = "0d";
			updateScheTaskToCleanTask(maxStayTime);
			SiteWrapper[] expSites6 = {rootSite};
			this.checkScmFile(0, fileNum, expSites6);	
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} 
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		if (null != scheduleId) {
			this.deleteScheduleTask();
		}
		ScmWorkspaceUtil.deleteWs(wsName,session);
		if(session != null){
			session.close();
		}
		if (runSuccess || TestScmBase.forceClear) {
			TestTools.LocalFile.removeFile(localPath);
		}
	}
	
	private void readyScmFile(int startNum, int endNum,Date date) throws ScmException, ParseException {
		ScmSession ss = null;
		try {
			ss = TestScmTools.createSession(branSite);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);
			
			for (int i = startNum; i < endNum; i++) {
				ScmFile file = ScmFactory.File.createInstance(ws);
				file.setContent(filePath);
				file.setFileName(name + "_" + i);
				file.setAuthor(name);
				file.setCreateTime(date);
				ScmId fileId = file.save();
				fileIds.add(fileId);
			}
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (null != ss) ss.close();
		}
	}
	
	private void createScheduleTask(String maxStayTime) {
		ScmSession ss = null;
		try {
			ss = TestScmTools.createSession(branSite);
			
			ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
					branSite.getSiteName(), rootSite.getSiteName(), maxStayTime, queryCond);
            String cron = "* * * * * ?";
            ScmSchedule sche = ScmSystem.Schedule.create(ss, wsName,
            		ScheduleType.COPY_FILE, name, "", content, cron);
            scheduleId = sche.getId(); 

			ScmSchedule sche2 = ScmSystem.Schedule.get(ss, scheduleId);
			Assert.assertEquals(sche2.getContent(), content);
			Assert.assertEquals(sche2.getCron(), cron);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (null != ss) ss.close();
		}
	}
	
	private void deleteScheduleTask() {
		ScmSession ss = null;
		try {
			ss = TestScmTools.createSession(branSite);	
			ScmSystem.Schedule.delete(ss, scheduleId);
			if (runSuccess || TestScmBase.forceClear) {
				ScmScheduleUtils.cleanTask(ss, scheduleId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (null != ss) ss.close();
		}		
	}
	
	private void updateScheMaxStayTime(String maxStayTime) {
		ScmSession ss = null;
		try {
			ss = TestScmTools.createSession(branSite);
			
			ScmSchedule sche = ScmSystem.Schedule.get(ss, scheduleId);	
			ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
					branSite.getSiteName(), rootSite.getSiteName(), maxStayTime, queryCond);
			sche.updateContent(content);
			
            // check shedule info
			ScmSchedule sche2 = ScmSystem.Schedule.get(ss, scheduleId);
			Assert.assertEquals(sche2.getId(), scheduleId);
			Assert.assertEquals(sche2.getContent(), content);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (null != ss) ss.close();
		}
	}
	
	private void updateScheTaskToCleanTask(String maxStayTime) {
		ScmSession ss = null;
		String newName = name + "_clean";
		String newDesc = "desc_123";
		try {
			ss = TestScmTools.createSession(branSite);
			ScmSchedule sche = ScmSystem.Schedule.get(ss, scheduleId);
			
			ScheduleType taskType = ScheduleType.CLEAN_FILE;
			ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
					branSite.getSiteName(), maxStayTime, queryCond);
			
			sche.updateName(newName);
			sche.updateDesc(newDesc);
			sche.updateSchedule(taskType, content);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (null != ss) ss.close();
		}
	}
	
	private void checkScmFile(int startNum, int endNum, SiteWrapper[] expSites) throws Exception {
		ScmSession ss = null;
		try {
			ss = TestScmTools.createSession(branSite);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);
			ScmScheduleUtils.checkScmFile(ws, fileIds, startNum, endNum, expSites);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (null != ss) ss.close();
		}
	}
}