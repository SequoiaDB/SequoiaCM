package com.sequoiacm.net.scheduletask;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
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
 * @FileName  SCM-1246:原type为清理，更新为迁移，并更新源和目标站点满足迁移条件
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class UpdateSche_cleanToCopy1246 extends TestScmBase {
	private boolean runSuccess = false;		
	private ScmSession ssR = null;
	private ScmWorkspace wsR = null;
	private ScmSession ssA = null;
	private ScmWorkspace wsA = null;
	private SiteWrapper rootSite = null;
	private SiteWrapper branSite = null;
	private WsWrapper wsp = null;
	
	private final static int fileNum = 3;
	private final static int fileSize = 100;
	private final static String name = "sche1246";
	private List<ScmId> fileIds = new ArrayList<>();
	private File localPath = null;
	private String filePath = null;
	private BSONObject queryCond = null;
	
	private ScmId scheduleId = null;
	private final static String cron = "* * * * * ?";

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
			wsp = ScmInfo.getWs();
			List<SiteWrapper> sites = ScmNetUtils.getCleanSites(wsp);
			rootSite = sites.get(1);
			branSite = sites.get(0);
		
			ssR = TestScmTools.createSession(rootSite);
			wsR = ScmFactory.Workspace.getWorkspace(wsp.getName(), ssR);
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
			this.writeScmFile(wsA, 0, fileNum);
			this.readScmFile(wsR, 0, fileNum);
			SiteWrapper[] expSites1 = {rootSite, branSite};
			ScmScheduleUtils.checkScmFile(wsA, fileIds, 0, fileNum, expSites1);
			// create schedule task, type is clean, and check
			this.createScheduleTask();
			SiteWrapper[] expSites2 = {rootSite};
			ScmScheduleUtils.checkScmFile(wsA, fileIds, 0, fileNum, expSites2);
		
			// update schedule task to copy, and check
			this.updateScheTaskToCopeTask();
			// write scmFile again at the rootSite
			this.writeScmFile(wsA, fileNum, fileNum + 3);	
			SiteWrapper[] expSites3 = {rootSite, branSite};
			ScmScheduleUtils.checkScmFile(wsA, fileIds, fileNum, fileNum + 3, expSites3);		

			SiteWrapper[] expSites4 = {rootSite};
			ScmScheduleUtils.checkScmFile(wsA, fileIds, 0, fileNum, expSites4);
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
				ScmFileUtils.cleanFile(wsp, queryCond);
				TestTools.LocalFile.removeFile(localPath);
				ScmScheduleUtils.cleanTask(ssA, scheduleId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (ssR != null) {
				ssR.close();
			}
			if (ssA != null) {
				ssA.close();
			}
		}
	}
	
	private void writeScmFile(ScmWorkspace ws, int startNum, int endNum) throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR)-3);
		for (int i = startNum; i < endNum; i++) {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setContent(filePath);
			file.setFileName(name + "_" + i);
			file.setAuthor(name);
			file.setCreateTime(calendar.getTime());
			ScmId fileId = file.save();
			fileIds.add(fileId);
		}
	}
	
	private void readScmFile(ScmWorkspace ws, int startNum, int endNum) throws Exception {
		for (int i = startNum; i < endNum; i++) {
			ScmId fileId = fileIds.get(i);
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			file.getContent(downloadPath);
		}
	}
	
	private void createScheduleTask() {
		try {
			ScheduleType taskType = ScheduleType.CLEAN_FILE;
			String maxStayTime = "0d";
			ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
					branSite.getSiteName(), maxStayTime, queryCond);
            ScmSchedule sche = ScmSystem.Schedule.create(ssA, wsp.getName(), 
            		taskType, name, "", content, cron);
            scheduleId = sche.getId();	
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	private void updateScheTaskToCopeTask() {
		String newName = name + "_cope";
		try {
			ScmSchedule sche = ScmSystem.Schedule.get(ssA, scheduleId);
			
			ScheduleType taskType = ScheduleType.COPY_FILE;
			String maxStayTime = "0d";
			ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
					branSite.getSiteName(), rootSite.getSiteName(), maxStayTime, queryCond);
			
			sche.updateName(newName);
			sche.updateSchedule(taskType, content);
            
            // check shedule info
			ScmSchedule sche2 = ScmSystem.Schedule.get(ssA, scheduleId);
			Assert.assertEquals(sche2.getId(), scheduleId);
			Assert.assertEquals(sche2.getType(), taskType);
			Assert.assertEquals(sche2.getName(), newName);
			Assert.assertEquals(sche2.getContent(), content);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}