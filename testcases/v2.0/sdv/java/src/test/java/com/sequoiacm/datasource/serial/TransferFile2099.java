package com.sequoiacm.datasource.serial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:Transfer file,target site and source site data sources are different 
 * testlink-case:SCM-2099
 * @author wuyan
 * @Date 2018.07.17
 * @version 1.00
 */

public class TransferFile2099 extends TestScmBase {
	private boolean runSuccess = false;	
	private static WsWrapper wsp = null;
	private SiteWrapper branSite = null;
	private SiteWrapper rootSite = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionM = null;
	private ScmWorkspace wsM = null;	
	private ScmId taskId = null;
	private BSONObject taskCondition = null;
	private List<ScmId> fileIdList = new ArrayList<ScmId>();

	private String fileName = "file2099";	
	private String authorName = "file2099";
	private int fileSize1 = 1024 * 3;
	private int fileSize2 = 1024 * 10;
	private String filePath1 = null;	
	private String filePath2 = null;
	private File localPath = null;

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
		
		int getSiteNums = 2;
		List<SiteWrapper> branSitelist = ScmInfo.getBranchSites(getSiteNums);
		rootSite = ScmInfo.getRootSite();
		DatasourceType rootSiteDataType = rootSite.getDataType();
		int dbDataSoureCount = 0;
		for (int i = 0; i < branSitelist.size(); i++) {
			DatasourceType dataType = branSitelist.get(i).getDataType();
			if (!dataType.equals(rootSiteDataType)) {
				branSite = branSitelist.get(i);
				break;
			}
			dbDataSoureCount ++;
		}
		if ( dbDataSoureCount == branSitelist.size() ) {				
			throw new SkipException("all bransite are connected to sequoiadb datasourse, skip!");
		}

		wsp = ScmInfo.getWs();
		sessionA = TestScmTools.createSession(branSite);
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionM = TestScmTools.createSession(rootSite);
		wsM = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionM);

		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		ScmFileUtils.cleanFile(wsp, cond);			
	}

	@Test(groups = { "fourSite" })
	private void test() throws Exception {
		writeFile( wsA );
		startTransferTaskByFile( wsA, sessionA );

		// check the file data and siteinfo
		checkTransferResult(wsA);
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() {
		try {
			if( runSuccess ){
				for( ScmId fileId : fileIdList ){
					ScmFactory.File.deleteInstance(wsM, fileId, true);
				}				
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
			if (sessionM != null) {
				sessionM.close();
			}
		}
	}

	private void startTransferTaskByFile(ScmWorkspace ws, ScmSession session) throws Exception {		
		taskCondition = ScmQueryBuilder.start().put(ScmAttributeName.File.SIZE)
				.greaterThanEquals(fileSize2)
				.put(ScmAttributeName.File.AUTHOR).is(fileName).get();			
		taskId = ScmSystem.Task.startTransferTask(ws, taskCondition);

		//wait task finish
		ScmTaskUtils.waitTaskFinish(session, taskId);	
	}
	
	private void writeFile(ScmWorkspace ws) throws ScmException{
		int fileNum = 10;
		for (int i = 0; i < fileNum; i++) {
			String subfileName = fileName + "_" + i;
			ScmId fileId = null;
			if( i % 2 == 0 ){
				fileId = VersionUtils.createFileByFile(ws, subfileName, filePath1, authorName);			
			}else{
				fileId = VersionUtils.createFileByFile(ws, subfileName, filePath2, authorName);
			}			
			fileIdList.add(fileId);			
		}
	}
	
	private void checkTransferResult(ScmWorkspace ws) throws Exception{
		//check the transfered file,check the sitelist and data	
		ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_CURRENT, taskCondition);
		int size = 0;
		ScmFileBasicInfo file;		
		List<ScmId> transferfileIdList = new ArrayList<ScmId>();
		while (cursor.hasNext()) {
			file = cursor.getNext();			
			ScmId fileId = file.getFileId();
			transferfileIdList.add(fileId);				
			size++;
		}
		cursor.close();
		int expTransferFileNums = 5;
		Assert.assertEquals(size, expTransferFileNums);
		//check transfered file siteinfo and data 
		SiteWrapper[] expCurSiteList = { rootSite, branSite };
		ScmFileUtils.checkMetaAndData(wsp, transferfileIdList, expCurSiteList, 
						localPath, filePath2);	
		
		//check the no transfered file,check the sitelist
		BSONObject findCondition = ScmQueryBuilder.start().put(ScmAttributeName.File.SIZE)
				.lessThan(fileSize2)
				.put(ScmAttributeName.File.AUTHOR).is(authorName).get();
		
		ScmCursor<ScmFileBasicInfo> cursor1 = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_CURRENT, findCondition);
		SiteWrapper[] expSiteList1 = {  branSite };
		int size1 = 0;
		while (cursor1.hasNext()) {
			ScmFileBasicInfo file1 = cursor1.getNext();			
			// check results
			ScmId fileId1 = file1.getFileId();	
			ScmFileUtils.checkMeta(ws, fileId1, expSiteList1);			
			size1 ++;
		}		
		cursor1.close();	
		int expNoTransferFileNums = 5;
		Assert.assertEquals(size1, expNoTransferFileNums);
	}

}