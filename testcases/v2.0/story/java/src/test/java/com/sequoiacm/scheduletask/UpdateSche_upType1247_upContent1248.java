package com.sequoiacm.scheduletask;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
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
 * @FileName   SCM-1247:原type为清理，只更新类型为迁移
 * 			   SCM-1248:原type为清理，更新站点信息错误
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class UpdateSche_upType1247_upContent1248 extends TestScmBase {
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
	private final static String name = "schetask1247";
	private List<ScmId> fileIds = new ArrayList<>();
	private File localPath = null;
	private String filePath = null;
	private BSONObject queryCond = null;
	
	private ScmId scheduleId = null;
	private final static String cron = "* * * * * ?";
	private final static String maxStayTime = "0d";
	private ScmScheduleCleanFileContent content = null;

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
			wsp = ScmInfo.getWs();
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
		SiteWrapper[] expSites = {rootSite};
		try {
			// write and read scmFile
			this.readyScmFile(0, fileNum);
			// create schedule task, type is clean
			this.createCopeSchedule();
			ScmScheduleUtils.checkScmFile(wsA, fileIds, expSites);

			// update schedule type to cope, and check
			this.updateScheType();
			this.checkScheInfo();

			// update schedule content[site not exist], and check
			this.updateScheContent();
			this.checkScheInfo();
			
			// write scmFile again, and check after schedule task
			this.readyScmFile(fileNum, fileNum + 3);
			ScmScheduleUtils.checkScmFile(wsA, fileIds, expSites);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (null != scheduleId) {
				ScmSystem.Schedule.delete(ssA, scheduleId);
			}
			
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
	
	private void readyScmFile(int startNum, int endNum) throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR)-3);
		for (int i = startNum; i < endNum; i++) {
			ScmFile file = ScmFactory.File.createInstance(wsR);
			file.setContent(filePath);
			file.setFileName(name + "_" + i);
			file.setAuthor(name);
			file.setCreateTime(calendar.getTime());
			ScmId fileId = file.save();
			fileIds.add(fileId);
			
			// read scmFile at the branSite
			ScmFile file2 = ScmFactory.File.getInstance(wsA, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			file2.getContent(downloadPath);
		}
	}
	
	private void createCopeSchedule() {
		try {
			ScheduleType taskType = ScheduleType.CLEAN_FILE;
			content = new ScmScheduleCleanFileContent(
					branSite.getSiteName(), maxStayTime, queryCond);
			//System.out.println(content.toBSONObject());
            ScmSchedule sche = ScmSystem.Schedule.create(ssA, wsp.getName(), 
            		taskType, name, "desc", content, cron);
            scheduleId = sche.getId();
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	private void updateScheType() {
		try {
			ScmSchedule sche = ScmSystem.Schedule.get(ssA, scheduleId);
			
			ScheduleType taskType = ScheduleType.COPY_FILE;
			sche.updateSchedule(taskType, content);
			Assert.fail("expect fail but actual succ.");
		} catch (ScmException e) {
			if (ScmError.HTTP_BAD_REQUEST != e.getError()) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}
	
	private void updateScheContent() {
		try {
			ScmSchedule sche = ScmSystem.Schedule.get(ssA, scheduleId);
			
			ScheduleType taskType = ScheduleType.COPY_FILE;
			ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
					"test123", maxStayTime, queryCond);
			sche.updateSchedule(taskType, content);
			Assert.fail("expect fail but actual succ.");
		} catch (ScmException e) {
			if (ScmError.HTTP_BAD_REQUEST != e.getError()) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}
	
	private void checkScheInfo() {
		try {
			ScmSchedule sche = ScmSystem.Schedule.get(ssA, scheduleId);
			Assert.assertEquals(sche.getId(), scheduleId);
			Assert.assertEquals(sche.getType(), ScheduleType.CLEAN_FILE);
			Assert.assertEquals(sche.getContent(), content);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}