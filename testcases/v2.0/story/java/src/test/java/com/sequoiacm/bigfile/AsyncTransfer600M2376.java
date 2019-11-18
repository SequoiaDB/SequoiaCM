package com.sequoiacm.bigfile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.UUID;

/**
 * @Description: 异步迁移600M文件
 * @author fanyu
 * @Date:2019年02月14日
 * @version:1.0
 */

public class AsyncTransfer600M2376 extends TestScmBase {
	private boolean runSuccess = false;
	private long fileSize = 1024 * 1024 * 600;
	private ScmId fileId = null;
	private String fileName = "AsyncTransfer600M";
	private File localPath = null;
	private String filePath = null;
	private ScmSession sessionA = null;
	private ScmWorkspace ws = null;
	private SiteWrapper rootSite = null;
	private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws Exception {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		// ready file
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);
		rootSite = ScmInfo.getRootSite();
		branceSite = ScmInfo.getBranchSite();
		ws_T = ScmInfo.getWs();
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
		ScmFileUtils.cleanFile(ws_T, cond);
		sessionA = TestScmTools.createSession(branceSite);
		ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
		prepareFiles();
	}

	@Test(groups = {"twoSite", "fourSite"})
	private void test() throws Exception {
		ScmFactory.File.asyncTransfer(ws, fileId);
		//check result
		SiteWrapper[] expSiteList = { rootSite, branceSite };
		ScmTaskUtils.waitAsyncTaskFinished(ws, fileId, expSiteList.length);
		ScmFileUtils.checkMetaAndData(ws_T,fileId,expSiteList, localPath, filePath);
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
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
}