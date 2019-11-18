package com.sequoiacm.net.scheduletask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

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
	private WsWrapper wsp = null;
	
	private final static int fileNum = 10;
	private final static int fileSize = 100;
	private final static String name = "sche1252";
	private List<ScmId> fileIds = new ArrayList<>();
	private Long fileCreatetime;  //unit: s
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
			wsp = ScmInfo.getWs();
			
			// clean environment
			queryCond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
			ScmFileUtils.cleanFile(wsp, queryCond);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	// TODO: authSever may be have multiple node, 
	// same of the nodes may be deployed with scmServer in the same host, 
	// and session login time is a random host time of authServer, 
	// may lead to the expired of session.
	@Test(groups = { "twoSite", "fourSite" }, enabled = false)
	private void test() throws Exception {
		Long aMonthTime = 32 * 24 * 3600 * 1000L;
		int sleepTime = 1500; //ms
		try {
			// write scmFile
			this.readyScmFile(0, fileNum / 2);
			
			// set localTime back and jump 1 months
			Long newTime1 = fileCreatetime - aMonthTime;
			TestTools.setSystemTime(branSite.getNode().getHost(), newTime1);
			Thread.sleep(sleepTime);
			
			// write scmFile again
			this.readyScmFile(fileNum / 2, fileNum);
			
			// create schedule task, type is cope, and check
			String maxStayTime = "1d";
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
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			TestTools.restoreSystemTime(branSite.getNode().getHost());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		if (null != scheduleId) {
			this.deleteScheduleTask();	
			if (runSuccess || TestScmBase.forceClear) {
				ScmFileUtils.cleanFile(wsp, queryCond);
				TestTools.LocalFile.removeFile(localPath);
			}
		}
	}
	
	private void readyScmFile(int startNum, int endNum) throws ScmException {
		ScmSession ss = null;
		try {
			ss = TestScmTools.createSession(branSite);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), ss);
			
			for (int i = startNum; i < endNum; i++) {
				ScmFile file = ScmFactory.File.createInstance(ws);
				file.setContent(filePath);
				file.setFileName(name + "_" + i);
				file.setAuthor(name);
				ScmId fileId = file.save();
				fileIds.add(fileId);
			}
			
			ScmFile file = ScmFactory.File.getInstance(ws, fileIds.get(0));
			fileCreatetime = file.getCreateTime().getTime();
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
			//System.out.println(content.toBSONObject());
            String cron = "* * * * * ?";
            ScmSchedule sche = ScmSystem.Schedule.create(ss, wsp.getName(), 
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
	
	private void checkScmFile(int startNum, int endNum, SiteWrapper[] expSites) throws Exception {
		ScmSession ss = null;
		try {
			ss = TestScmTools.createSession(branSite);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), ss);
			ScmScheduleUtils.checkScmFile(ws, fileIds, startNum, endNum, expSites);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (null != ss) ss.close();
		}
	}
}