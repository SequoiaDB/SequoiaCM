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
 * test content:update Content of the current scm file, than ayncCache file does not specify version,
 * 				 ayncCache the current file by default
 * testlink-case:SCM-1656
 * 
 * @author wuyan
 * @Date 2018.06.05
 * @modify By wuyan
 * @modify Date 2018.07.27
 * @version 1.10
 */

public class AsyncCacheCurVersionFile1656b extends TestScmBase {
	private static WsWrapper wsp = null;
	private SiteWrapper cacheSite = null;
	private SiteWrapper sourceSite = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionS = null;
	private ScmWorkspace wsS = null;
	private ScmId fileId = null;

	private String fileName = "fileVersion1656b";
	private byte[] filedata = new byte[1024 * 50];
	private byte[] updatedata = new byte[1024 * 2];

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		wsp = ScmInfo.getWs();
		// clean file
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
		ScmFileUtils.cleanFile(wsp, cond);

		List<SiteWrapper> siteList = ScmNetUtils.getRandomSites(wsp);
		cacheSite = siteList.get(0);
		sourceSite = siteList.get(1);

		sessionA = TestScmTools.createSession(cacheSite);
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionS = TestScmTools.createSession(sourceSite);
		wsS = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionS);

		fileId = VersionUtils.createFileByStream(wsS, fileName, filedata);
		VersionUtils.updateContentByStream(wsS, fileId, updatedata);
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		int currentVersion = 2;
		int historyVersion = 1;
		ScmFactory.File.asyncCache(wsA, fileId);
		int sitenums = 2;
		VersionUtils.waitAsyncTaskFinished(wsA, fileId, currentVersion, sitenums);

		// check the currentVersion file data and siteinfo
		SiteWrapper[] expCurSiteList = { sourceSite, cacheSite };
		VersionUtils.checkSite(wsS, fileId, currentVersion, expCurSiteList);
		VersionUtils.CheckFileContentByStream(wsA, fileName, currentVersion, updatedata);

		// check the historyVersion file only on the rootSite
		SiteWrapper[] expHisSiteList = { sourceSite };
		VersionUtils.checkSite(wsA, fileId, historyVersion, expHisSiteList);
	}

	@AfterClass
	private void tearDown() {
		try {
			ScmFactory.File.deleteInstance(wsA, fileId, true);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
			if (sessionS != null) {
				sessionS.close();
			}
		}
	}

}