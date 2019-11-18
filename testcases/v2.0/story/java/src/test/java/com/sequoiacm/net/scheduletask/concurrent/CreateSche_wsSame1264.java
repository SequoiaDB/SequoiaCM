package com.sequoiacm.net.scheduletask.concurrent;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Testcase: SCM-1264:并发创建相同调度任务，指定相同ws
 * @author huangxiaoni init
 * @date 2018.4.24
 */

public class CreateSche_wsSame1264 extends TestScmBase {
	private boolean runSuccess = false;	
	private ScmSession ssA = null;
	private ScmWorkspace wsA = null;
	private SiteWrapper rootSite = null;
	private SiteWrapper branSite = null;
	private WsWrapper wsp = null;
	
	private final static int fileNum = 2;
	private final static int fileSize = 100;
	private final static String name = "sche1264";
	private List<ScmId> fileIds = new ArrayList<>();
	private File localPath = null;
	private String filePath = null;
	private BSONObject queryCond = null;
	
	private List<ScmId> scheIds =  new CopyOnWriteArrayList<ScmId>();
	private List<CreateSchedule> crtSches = new CopyOnWriteArrayList<CreateSchedule>();

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
			rootSite = sites.get(0);
			branSite = sites.get(1);
			
			ssA = TestScmTools.createSession(branSite);
			wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), ssA);
			
			// clean environment
			queryCond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
			ScmFileUtils.cleanFile(wsp, queryCond);
            
			//ScmSystem.Schedule.delete(ssA, new ScmId("5b6a77464000650000000001"));
			// ready scmFile
			this.readyScmFile(wsA);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
					.is(name + "_" + 0).get();
			String cron = "2 * * * * ?";
			CreateSchedule crtSche1 = new CreateSchedule(cond, cron);

			cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME)
					.is(name + "_" + 1).get();
			cron = "3 * * * * ?";
			CreateSchedule crtSche2 = new CreateSchedule(cond, cron);

			crtSche1.start();
			crtSches.add(crtSche1);
			
			crtSche2.start();
			crtSches.add(crtSche2);

			if (!(crtSche1.isSuccess() && crtSche2.isSuccess())) {
				Assert.fail(crtSche1.getErrorMsg() + crtSche2.getErrorMsg());
			}
			
			Thread.sleep(3500); // wait schedule cycle
			SiteWrapper[] expSites = {rootSite, branSite};
			ScmScheduleUtils.checkScmFile(wsA, fileIds, expSites);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			for (ScmId scheduleId : scheIds) {			
				ScmSystem.Schedule.delete(ssA, scheduleId);				
				if (runSuccess || forceClear) {
					ScmScheduleUtils.cleanTask(ssA, scheduleId);
				}
			}
			
			if (runSuccess || forceClear) {
				ScmFileUtils.cleanFile(wsp, queryCond);	
				TestTools.LocalFile.removeFile(localPath);
			}
		} finally {
			if (ssA != null) {
				ssA.close();
			}

		}
	}

	private class CreateSchedule extends TestThreadBase {
		private BSONObject cond = null;
		private String cron = null;
		
		public CreateSchedule(BSONObject cond, String cron) {
			this.cond = cond;
			this.cron = cron;
		}
		
		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				String maxStayTime = "0d";
				ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
						branSite.getSiteName(), rootSite.getSiteName(), maxStayTime, cond);
	            ScmSchedule sche = ScmSystem.Schedule.create(ssA, wsp.getName(), 
	            		ScheduleType.COPY_FILE, name, "", content, cron);
	            ScmId scheduleId = sche.getId();
	            scheIds.add(scheduleId);
			} catch (ScmException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}
	
	private void readyScmFile(ScmWorkspace ws) throws ScmException, ParseException {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR)-3);
		for (int i = 0; i < fileNum; i++) {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setContent(filePath);
			file.setFileName(name + "_" + i);
			file.setAuthor(name);
			file.setCreateTime(calendar.getTime());
			ScmId fileId = file.save();
			fileIds.add(fileId);
		}
	}
}
