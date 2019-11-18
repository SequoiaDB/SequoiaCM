package com.sequoiacm.net.task;

import java.io.File;
import java.util.Random;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-472: 清理主中心文件
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * 1、在主中心开始清理任务； 2、检查执行结果；
 */
public class Clean_startCleanTaskInMain472 extends TestScmBase {
	private boolean runSuccess = false;
	private ScmId fileId = null;
	private ScmId taskId = null;
	private int fileSize = new Random().nextInt(1024) + 1024;
	private File localPath = null;
	private String filePath = null;
	private String authorName = "StartCleanTaskInMain472";
	private ScmSession sessionM = null;
	private ScmWorkspace wsM = null;
	
	private SiteWrapper rootSite = null;
	private WsWrapper ws_T = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {

		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			// ready file
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
			
			rootSite = ScmInfo.getRootSite();
			ws_T = ScmInfo.getWs();
			
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(ws_T,cond);

			// login in
			sessionM = TestScmTools.createSession(rootSite);
			wsM = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);
			writeFileFromMainCenter();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			startCleanTaskFromMainCenter();
			checkResult();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || forceClear) {
				ScmFactory.File.deleteInstance(wsM, fileId, true);
				TestSdbTools.Task.deleteMeta(taskId);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (BaseException | ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionM != null) {
				sessionM.close();
			}

		}
	}

	private void writeFileFromMainCenter() {
		try {
			ScmFile scmfile = ScmFactory.File.createInstance(wsM);
			scmfile.setContent(filePath);
			scmfile.setFileName(authorName+"_"+UUID.randomUUID());
		    scmfile.setAuthor(authorName);
			fileId = scmfile.save();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	private void startCleanTaskFromMainCenter() throws ScmException {
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		taskId = ScmSystem.Task.startCleanTask(wsM, cond);
	}

	private void checkResult() {
		// check meta data
		try {
			SiteWrapper[] expSiteIdList = { rootSite };
			ScmFileUtils.checkMetaAndData(ws_T,fileId, expSiteIdList, localPath, filePath);
		} catch (Exception e) {
		}
	}
}
