package com.sequoiacm.directory.serial;


import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmDirUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * test content:create ScheduleCleanTask,match directory to clean up files in the directory
 * testlink-case:SCM-2062
 * 
 * @author wuyan
 * @Date 2018.07.13
 * @version 1.00
 */

public class Sche_CleanFileByDir2062 extends TestScmBase {	
	private static WsWrapper wsp = null;
	private SiteWrapper branSite = null;
	private SiteWrapper rootSite = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionM = null;
	private ScmWorkspace wsM = null;	
	private List<ScmId> fileIdList1 = new ArrayList<>();
	private List<ScmId> fileIdList2 = new ArrayList<>();
	private int fileNum = 10;
	private BSONObject condition = null;
	private ScmId scheduleId = null;
	private ScmScheduleCleanFileContent content = null;
	private String cron = null;

	private ScmDirectory scmDir1;
	private ScmDirectory scmDir2;
	private String fullPath1 = "/CreatefileWiteDir2062a/2062_a/2062_b/2062_c/2062_e/2062_f/";
	private String fullPath2 = "/CreatefileWiteDir2062b/2062_a/2062_b/2062_c/2062_e/2062_f/";
	private String authorName = "CreateFileWithDir2062";
	private String fileName = "filedir2060";
	private final static String taskname = "schetask2062";
	private byte[] writeData1 = new byte[ 1024 * 2 ];	
	private byte[] writeData2 = new byte[ 1024 * 5 ];
	private boolean runSuccess = false;

	@BeforeClass
	private void setUp() throws IOException, ScmException  {
		branSite = ScmInfo.getBranchSite();
		rootSite = ScmInfo.getRootSite();
		wsp = ScmInfo.getWs();
		
		sessionA = TestScmTools.createSession(branSite);
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionM = TestScmTools.createSession(rootSite);
		wsM = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionM);
		
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		ScmFileUtils.cleanFile(wsp, cond);
		ScmDirUtils.deleteDir(wsA, fullPath1);	
		ScmDirUtils.deleteDir(wsA, fullPath2);				
	}

	@Test(groups = {  "twoSite", "fourSite"})
	private void test() throws Exception {	
		scmDir1 = ScmDirUtils.createDir(wsA,fullPath1);	
		writeFileWithDir( wsA, scmDir1, fileIdList1, writeData1);
		scmDir2 = ScmDirUtils.createDir(wsA,fullPath2);	
		writeFileWithDir( wsA, scmDir2, fileIdList2, writeData2);
		
		readFileFromSouceSite(wsM, fileIdList1);
		readFileFromSouceSite(wsM, fileIdList2);
		
		//clean current version file		
		createScheduleTask(sessionA);
		
		//check siteinfo		
		int currentVersion = 1;
		SiteWrapper[] expCurSiteList1 = { rootSite };
		VersionUtils.checkScheTaskFileSites(wsA, fileIdList1,currentVersion,expCurSiteList1);
		SiteWrapper[] expCurSiteList2 = { rootSite ,branSite };
		VersionUtils.checkScheTaskFileSites(wsM, fileIdList2,currentVersion,expCurSiteList2);			
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() {
		try {	
			ScmSystem.Schedule.delete(sessionA, scheduleId);			
			if (runSuccess || TestScmBase.forceClear) {
				for (ScmId fileId : fileIdList1) {
					ScmFactory.File.deleteInstance(wsM, fileId, true);
				}
				for (ScmId fileId : fileIdList2) {
					ScmFactory.File.deleteInstance(wsM, fileId, true);
				}				
				ScmScheduleUtils.cleanTask(sessionA, scheduleId);
			}							
		} catch (Exception e) {
			Assert.fail(e.getMessage()+e.getStackTrace());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}	
			if (sessionM != null) {
				sessionM.close();
			}
		}
	}	
	
	private void writeFileWithDir(ScmWorkspace ws,ScmDirectory scmDir, List<ScmId> fileIdList, byte[] writeData) throws ScmException{
		new Random().nextBytes(writeData);		
		
		for (int i = 0; i < fileNum; i++) {
			String subfileName = fileName + "_" + i;
			ScmId fileId = createFileWithDir( ws, subfileName, writeData, authorName ,scmDir);
			
			fileIdList.add(fileId);			
		}
	}
	
	private ScmId createFileWithDir( ScmWorkspace ws, String fileName, byte[] data, String authorName, 
			ScmDirectory dir ) throws ScmException  {		
		ScmFile file = ScmFactory.File.createInstance(ws);			
			
		file.setContent(new ByteArrayInputStream(data));
		file.setFileName(fileName);
		file.setAuthor(authorName);
		file.setTitle("sequoiacm");
		if (dir != null) {
			file.setDirectory(dir);
		}		
		file.setMimeType(fileName+".txt");
		//add tags
				ScmTags tags = new ScmTags();
				tags.addTag("我是一个标签2062                                                                                                                                                                                      "
						+ "                                ");
				tags.addTag( "THIS IS TAG 2062!");
				tags.addTag( "tag *&^^^^^*90234@#$%!~asf");
				file.setTags(tags);
		ScmId fileId = file.save();		
		return fileId;
	}
	
		
	
	private void createScheduleTask(ScmSession session) throws ScmException {
		String maxStayTime = "0d";
		condition = ScmQueryBuilder.start().put(ScmAttributeName.File.DIRECTORY_ID).in(scmDir1.getId()).get();					
		// create schedule task
		content = new ScmScheduleCleanFileContent(branSite.getSiteName(), maxStayTime, condition);
		content.setScope(ScopeType.SCOPE_CURRENT);		
		cron = "* * * * * ?";
        
        ScmSchedule sche = ScmSystem.Schedule.create(session, wsp.getName(), 
            		ScheduleType.CLEAN_FILE, taskname, "", content, cron);
        scheduleId = sche.getId();	        
	}	
	
	private void readFileFromSouceSite(ScmWorkspace ws, List<ScmId> fileIdList) throws Exception {		
		for (int i = 0; i < fileNum; i++) {			
			ScmId fileId = fileIdList.get(i);		
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		    file.getContent(outputStream);
		}
	}
	
}