package com.sequoiacm.net.scheduletask;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @FileName  SCM-1233:创建调度任务，调度内容中condition覆盖所有文件属性
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class CreateSche_coverAllFileAttr1233 extends TestScmBase {
	private boolean runSuccess = false;	
	private ScmSession ssA = null;
	private ScmWorkspace wsA = null;
	private SiteWrapper rootSite = null;
	private SiteWrapper branSite = null;
	private WsWrapper wsp = null;
	
	private final static int fileNum = 2;
	private final static int fileSize = 100;
	private final static String name = "schetask1233";
	private List<ScmId> fileIds = new ArrayList<>();
	private File localPath = null;
	private String filePath = null;
	private BSONObject queryCond = null;
	
	private ScmId scheduleId = null;
	private ScmScheduleContent content = null;
	private String cron = null;

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

			// ready scmFile
			this.readyScmFile(wsA, 0, fileNum);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			this.createScheduleTask();
			this.checkScheduleTaskInfo();

			// check results
			List<ScmId> tmpFileIds1 = new ArrayList<>();
			tmpFileIds1.add(fileIds.get(0));
			SiteWrapper[] expSites1 = {rootSite, branSite};
			ScmScheduleUtils.checkScmFile(wsA, tmpFileIds1, expSites1);

			List<ScmId> tmpFileIds2 = new ArrayList<>();
			tmpFileIds2.add(fileIds.get(1));
			SiteWrapper[] expSites2 = {branSite};
			ScmScheduleUtils.checkScmFile(wsA, tmpFileIds2, expSites2);
		} catch (Exception e) {
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
			if (ssA != null) {
				ssA.close();
			}
		}
	}
	
	private void readyScmFile(ScmWorkspace ws, int startNum, int endNum) throws ScmException, ParseException {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR)-3);
		for (int i = startNum; i < endNum; i++) {
			ScmFile file = ScmFactory.File.createInstance(ws);
			String tmpName = name + "_" + i;
			file.setContent(filePath);
			file.setFileName(tmpName);
			file.setAuthor(name);
			file.setMimeType(tmpName);
			file.setTitle(tmpName);
			file.setCreateTime(calendar.getTime());
			ScmId fileId = file.save();
			fileIds.add(fileId);
		}
	}
	
	private void createScheduleTask() {
		try {
			ScmId fileId = fileIds.get(0);
			ScmFile file = ScmFactory.File.getInstance(wsA, fileId);
//			System.out.println(file);
			ScmFileLocation fileLt = file.getLocationList().get(0);
			BSONObject lastATObj = ScmQueryBuilder.start(ScmAttributeName.File.LAST_ACCESS_TIME)
					.is(fileLt.getDate().getTime()).get();
			BSONObject createTM = ScmQueryBuilder.start(ScmAttributeName.File.CREATE_TIME)
					.is(fileLt.getCreateDate().getTime()).get();
			BSONObject siteId = ScmQueryBuilder.start(ScmAttributeName.File.SITE_ID)
					.is(fileLt.getSiteId()).get();
			
			
			SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMM");
			String fileMonth = dateFmt.format(file.getCreateTime());

			BSONObject cond = ScmQueryBuilder.start()
					.and(ScmAttributeName.File.FILE_ID).is(fileId.get())
					.and(ScmAttributeName.File.AUTHOR).is(file.getAuthor())
					.and(ScmAttributeName.File.TITLE).is(file.getTitle())
					.and(ScmAttributeName.File.MIME_TYPE).is(file.getMimeType())
					.and(ScmAttributeName.File.MAJOR_VERSION).is(file.getMajorVersion())
					.and(ScmAttributeName.File.MINOR_VERSION).is(file.getMinorVersion())
					.and(ScmAttributeName.File.SIZE).is(file.getSize())
					.and(ScmAttributeName.File.USER).is(file.getUser())
					.and(ScmAttributeName.File.CREATE_TIME).is(file.getCreateTime().getTime())
					.and(ScmAttributeName.File.UPDATE_USER).is(file.getUpdateUser())
					.and(ScmAttributeName.File.UPDATE_TIME).is(file.getUpdateTime().getTime())
					.and(ScmAttributeName.File.CREATE_MONTH).is(fileMonth)
					.and(ScmAttributeName.File.SITE_LIST).elemMatch(lastATObj)
					.and(ScmAttributeName.File.SITE_LIST).elemMatch(createTM)
					.and(ScmAttributeName.File.SITE_LIST).elemMatch(siteId)
//					.and(ScmAttributeName.File.FOLDER_ID).is(file.getDirectory())
//					.and(ScmAttributeName.File.BATCH_ID).is(file.getBatchId())
//					.and(ScmAttributeName.File.PROPERTIES).is(file.getProperties())
//					.and(ScmAttributeName.File.PROPERTY_TYPE).is(file.getPropertyType())
					.get();
			System.out.println(cond);
			
			String maxStayTime = "0d";
			content = new ScmScheduleCopyFileContent(
					branSite.getSiteName(), rootSite.getSiteName(), maxStayTime, cond);
			//System.out.println(content.toBSONObject());
            cron = "* * * * * ?";
            ScmSchedule sche = ScmSystem.Schedule.create(ssA, wsp.getName(), 
            		ScheduleType.COPY_FILE, name, "", content, cron);
            scheduleId = sche.getId();
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	private void checkScheduleTaskInfo() {
		try {
			ScmSchedule sche = ScmSystem.Schedule.get(ssA, scheduleId);
			Assert.assertEquals(sche.getId(), scheduleId);
			Assert.assertEquals(sche.getContent(), content);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}