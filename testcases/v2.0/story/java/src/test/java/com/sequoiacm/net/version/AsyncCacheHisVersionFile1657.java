package com.sequoiacm.net.version;

import java.io.IOException;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:update Content of the current scm file, than ayncCache the history version file 
 * testlink-case:SCM-1657
 * 
 * @author wuyan
 * @Date 2018.06.05
 * @modify Date 2018.07.27
 * @version 1.10
 * 
 */

public class AsyncCacheHisVersionFile1657 extends TestScmBase {
	private static WsWrapper wsp = null;
	private SiteWrapper cacheSite = null;
	private SiteWrapper sourceSite = null;
	private ScmSession sessionC = null;
	private ScmWorkspace wsC = null;
	private ScmSession sessionS = null;
	private ScmWorkspace wsS = null;
	private ScmId fileId = null;

	private String fileName = "file1657";
	private byte[] filedata = new byte[1024 * 100];
	private byte[] updatedata = new byte[1024 * 200];

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		wsp = ScmInfo.getWs();
		// clean file
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
		ScmFileUtils.cleanFile(wsp, cond);

		List<SiteWrapper> siteList = ScmNetUtils.getRandomSites(wsp);
		cacheSite = siteList.get(0);
		sourceSite = siteList.get(1);

		sessionC = TestScmTools.createSession(cacheSite);
		wsC = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionC);
		sessionS = TestScmTools.createSession(sourceSite);
		wsS = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionS);

		fileId = VersionUtils.createFileByStream(wsS, fileName, filedata);
		VersionUtils.updateContentByStream(wsS, fileId, updatedata);
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		int currentVersion = 2;
		int historyVersion = 1;
		asyncCacheCurrentVersionFile(historyVersion);

		// check the history file data and siteinfo
		SiteWrapper[] expHisSiteList = { sourceSite, cacheSite };
		VersionUtils.waitAsyncTaskFinished(wsS, fileId, historyVersion, expHisSiteList.length);
		VersionUtils.checkSite(wsS, fileId, historyVersion, expHisSiteList);
		VersionUtils.CheckFileContentByStream(wsC, fileName, historyVersion, filedata);

		// check the currentVersion file only on the rootSite
		SiteWrapper[] expCurSiteList = { sourceSite };
		VersionUtils.checkSite(wsC, fileId, currentVersion, expCurSiteList);
	}

	@AfterClass
	private void tearDown() {
		try {
			ScmFactory.File.deleteInstance(wsC, fileId, true);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionC != null) {
				sessionC.close();
			}
			if (sessionS != null) {
				sessionS.close();
			}
		}
	}

	private void asyncCacheCurrentVersionFile(int majorVersion) {
		try {
			// cache
			ScmFactory.File.asyncCache(wsC, fileId, majorVersion, 0);
		} catch (ScmException e) {
			Assert.fail("asynccAche file fail!" + e.getErrorCode() + e.getStackTrace());
		}
	}
}