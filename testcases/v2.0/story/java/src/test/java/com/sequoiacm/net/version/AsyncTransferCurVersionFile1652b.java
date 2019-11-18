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
 * test content:update Content of the current scm file, than ayncTransfer file does not specify version, 
 * 				ayncTransfer the current file by default
 * testlink-case:SCM-1652
 * 
 * @author wuyan
 * @Date 2018.06.05
 * @modify Date 2018.07.27
 * @version 1.10
 */

public class AsyncTransferCurVersionFile1652b extends TestScmBase {
	private static WsWrapper wsp = null;
	private SiteWrapper asyncTransferSite = null;
	private SiteWrapper targetSite = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionT = null;
	private ScmWorkspace wsT = null;
	private ScmId fileId = null;

	private String fileName = "fileVersion1652b";
	private byte[] filedata = new byte[1024 * 100];
	private byte[] updatedata = new byte[1024 * 200];

	@BeforeClass
	private void setUp() throws IOException, ScmException {
		wsp = ScmInfo.getWs();
		// clean file
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
		ScmFileUtils.cleanFile(wsp, cond);

		List<SiteWrapper> siteList = ScmNetUtils.getSortSites(wsp);
		asyncTransferSite = siteList.get(0);
		targetSite = siteList.get(1);

		sessionA = TestScmTools.createSession(asyncTransferSite);
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionT = TestScmTools.createSession(targetSite);
		wsT = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionT);

		fileId = VersionUtils.createFileByStream(wsA, fileName, filedata);
		VersionUtils.updateContentByStream(wsA, fileId, updatedata);
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		int currentVersion = 2;
		int historyVersion = 1;
		ScmFactory.File.asyncTransfer(wsA, fileId);
		// wait task finished
		int sitenums = 2;
		VersionUtils.waitAsyncTaskFinished(wsT, fileId, currentVersion, sitenums);

		// check the currentVersion file data and siteinfo
		SiteWrapper[] expCurSiteList = { targetSite, asyncTransferSite };
		VersionUtils.checkSite(wsA, fileId, currentVersion, expCurSiteList);
		VersionUtils.CheckFileContentByStream(wsT, fileName, currentVersion, updatedata);

		// check the historyVersion file only on the branSiteA
		SiteWrapper[] expHisSiteList = { asyncTransferSite };
		VersionUtils.checkSite(wsA, fileId, historyVersion, expHisSiteList);
	}

	@AfterClass
	private void tearDown() {
		try {
			ScmFactory.File.deleteInstance(wsT, fileId, true);
		} catch (Exception e) {
			Assert.fail(e.getMessage() + e.getStackTrace());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
			if (sessionT != null) {
				sessionT.close();
			}
		}
	}
}