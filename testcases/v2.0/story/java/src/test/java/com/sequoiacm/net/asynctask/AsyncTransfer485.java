package com.sequoiacm.net.asynctask;

import java.io.File;
import java.util.List;
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
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @FileName SCM-485: 异步迁移单个文件
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、在分中心A异步迁移单个文件； 2、检查执行结果正确性； 3、后台异步迁移任务执行完成后检查迁移后的文件正确性；
 */

public class AsyncTransfer485 extends TestScmBase {
	private boolean runSuccess = false;

	private final int fileSize = 200 * 1024;
	private ScmId fileId = null;
	private String fileName = "AsyncTransfer485";
	private File localPath = null;
	private String filePath = null;

	private ScmSession sessionA = null;
	private ScmWorkspace ws = null;
	
	private SiteWrapper sourceSite = null;
	private SiteWrapper targetSite = null;
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

			ws_T = ScmInfo.getWs();
			List<SiteWrapper> siteList = ScmNetUtils.getSortSites(ws_T);
			sourceSite = siteList.get(0);
			targetSite = siteList.get(1); 

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
			ScmFileUtils.cleanFile(ws_T, cond);

			sessionA = TestScmTools.createSession(sourceSite);
			ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
			prepareFiles();
		} catch (Exception e) {
			if (sessionA != null) {
				sessionA.close();
			}
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			ScmFactory.File.asyncTransfer(ws, fileId);
			checkResult();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
		}
	}

	private void prepareFiles() throws Exception {
		ScmFile scmfile = ScmFactory.File.createInstance(ws);
		scmfile.setContent(filePath);
		scmfile.setFileName(fileName+"_"+UUID.randomUUID());
		fileId = scmfile.save();
	}

	private void checkResult() {
		try {
			SiteWrapper[] expSiteList = { sourceSite, targetSite };
			ScmTaskUtils.waitAsyncTaskFinished(ws, fileId, expSiteList.length);
			ScmFileUtils.checkMetaAndData(ws_T,fileId,expSiteList, localPath, filePath);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}