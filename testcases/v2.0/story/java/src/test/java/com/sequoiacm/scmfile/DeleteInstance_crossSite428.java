package com.sequoiacm.scmfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-428:删除文件，跨中心删除（ScmFactory.File.deleteInstance）
 * @author huangxiaoni init
 * @date 2017.6.15
 */

public class DeleteInstance_crossSite428 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper rootSite = null;
	private List<SiteWrapper> branSites = null;
	private final int branSitesNum = 2;
	private static WsWrapper wsp = null;

	private static ScmSession sessionM = null;
	private ScmWorkspace wsM = null;
	private static ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private static ScmSession sessionB = null;
	private ScmWorkspace wsB = null;

	private String fileName = "delete428";
	private String author = fileName;
	private ScmId fileId = null;
	private int fileSize = 100;
	private File localPath = null;
	private String filePath = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);

			rootSite = ScmInfo.getRootSite();
			branSites = ScmInfo.getBranchSites(branSitesNum);
			wsp = ScmInfo.getWs();

			sessionM = TestScmTools.createSession(rootSite);
			wsM = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionM);

			sessionA = TestScmTools.createSession(branSites.get(0));
			wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);

			sessionB = TestScmTools.createSession(branSites.get(1));
			wsB = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionB);
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(wsp, cond);
			fileId = ScmFileUtils.create(wsA, fileName, filePath);
			this.readScmFile(wsB);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "fourSite" })
	private void test() throws Exception {
		try {
			ScmFactory.File.deleteInstance(wsA, fileId, true);
			this.checkResults();
			runSuccess = true;
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || forceClear) {
				TestTools.LocalFile.removeFile(localPath);
			}
		} finally {
			if (sessionM != null)
				sessionM.close();
			if (sessionA != null)
				sessionA.close();
			if (sessionB != null)
				sessionB.close();

		}
	}

	private void readScmFile(ScmWorkspace ws) throws Exception {
		// read from siteB
		ScmFile file = ScmFactory.File.getInstance(ws, fileId);
		String downloadPath = localPath + File.separator + "download.txt";
		this.read(file, downloadPath);

		// check results
		/*SiteWrapper[] expSites = { rootSite, branSites.get(0), branSites.get(1) };
		ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);*/
	}

	private void checkResults() throws Exception {
		// check meta
		BSONObject cond = new BasicBSONObject("id", fileId.get());
		long cnt = ScmFactory.File.countInstance(wsA, ScopeType.SCOPE_CURRENT, cond);
		Assert.assertEquals(cnt, 0);

		// check data
		ScmWorkspace[] wsArr = { wsM, wsA, wsB };
		for (int i = 0; i < wsArr.length; i++) {
			try {
				ScmFileUtils.checkData(wsArr[i], fileId, localPath, filePath);
				Assert.assertFalse(true, "File is unExisted, except throw e, but success.");
			} catch (ScmException e) {
				Assert.assertEquals(e.getError(),ScmError.FILE_NOT_FOUND, e.getMessage());
			}
		}
	}

	private void read(ScmFile file, String downloadPath) throws ScmException, IOException {
		ScmInputStream sis = null;
		OutputStream fos = null;
		try {
			sis = ScmFactory.File.createInputStream(file);
			fos = new FileOutputStream(downloadPath);
			sis.read(fos);
		} finally {
			if (fos != null)
				fos.close();
			if (sis != null)
				sis.close();
		}
	}
}