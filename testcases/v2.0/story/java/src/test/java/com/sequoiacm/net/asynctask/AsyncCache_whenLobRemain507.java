package com.sequoiacm.net.asynctask;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * @Testcase: SCM-507:分中心有残留LOB，且跟主中心LOB大小一致
 * @author huangxiaoni init
 * @date 2017.6.26
 */

public class AsyncCache_whenLobRemain507 extends TestScmBase {
	private boolean runSuccess = false;
	private ScmSession sessionM = null;
	private ScmWorkspace wsM = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmId fileId = null;
	private String fileName = "asyncCache507";
	private int fileSize = 100;
	private File localPath = null;
	private String filePath = null;
	private String content = null;
	private SiteWrapper sourceSite = null;
	private SiteWrapper targetSite = null;
    private WsWrapper ws_T = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws Exception {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		// ready local file
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		content = TestTools.getRandomString(fileSize);
		TestTools.LocalFile.createFile(filePath, content, fileSize);

		ws_T = ScmInfo.getWs();
		List<SiteWrapper> siteList = ScmNetUtils.getRandomSites(ws_T);
		sourceSite = siteList.get(0);
		targetSite = siteList.get(1);

		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(fileName).get();
		ScmFileUtils.cleanFile(ws_T, cond);

		// login
		sessionM = TestScmTools.createSession(sourceSite);
		wsM = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);

		sessionA = TestScmTools.createSession(targetSite);
		wsA = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);

		// ready scm file
		writeFileFromM();
		//lobRemainFromA();
		TestSdbTools.Lob.putLob(targetSite, ws_T, fileId, filePath);
	}

	@Test(groups = {"fourSite" })
	private void test() throws Exception {
		ScmFactory.File.asyncCache(wsA, fileId);
		//check result
		SiteWrapper[] expSiteList = { sourceSite, targetSite };
		ScmTaskUtils.waitAsyncTaskFinished(wsM, fileId, expSiteList.length);
		ScmFileUtils.checkMetaAndData(ws_T,fileId, expSiteList, localPath, filePath);
		readFileFromA();
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || forceClear) {
				ScmFactory.File.deleteInstance(wsM, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		}  finally {
			if (sessionM != null) {
				sessionM.close();
			}
			if (sessionA != null) {
				sessionA.close();
			}
		}
	}

	private void writeFileFromM() throws ScmException {
		ScmFile file = ScmFactory.File.createInstance(wsM);
		file.setContent(filePath);
		file.setFileName(fileName+"_"+UUID.randomUUID());
		file.setAuthor(fileName);
		fileId = file.save();
	}

	private void readFileFromA() throws Exception {
		TestSdbTools.Lob.removeLob(sourceSite, ws_T, fileId);
		// read siteA's local cache
		String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
				Thread.currentThread().getId());
		ScmFile file = ScmFactory.File.getInstance(wsA, fileId);
		file.getContent(downloadPath);
		Assert.assertEquals(TestTools.getMD5(downloadPath), TestTools.getMD5(filePath));
	}
}