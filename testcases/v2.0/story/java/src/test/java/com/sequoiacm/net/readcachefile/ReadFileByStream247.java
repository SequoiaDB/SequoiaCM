package com.sequoiacm.net.readcachefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
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

/**
 * @Testcase:SCM-246 read空文件，输出流方式 1、分中心A写文件； 2、分中心B调用read输出流方式读取空文件；
 *                   3、检查读取的文件内容及元数据正确性；
 * @author fanyu init in 2017.05.10; huangxiaoni modify in 2017.6.6
 * @modify By wuyan
 * @modify Date: 2018.07.31
 * @version 1.10
 */
public class ReadFileByStream247 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper scmSite1 = null;
	private SiteWrapper scmSite2 = null;	
	private WsWrapper wsp = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionB = null;
	private ScmWorkspace wsB = null;

	private String fileName = "readCacheFile247";
	private ScmId fileId = null;
	private int fileSize = 0;
	private File localPath = null;
	private String filePath = null;

	@BeforeClass()
	private void setUp() throws IOException, ScmException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";

		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);

		wsp = ScmInfo.getWs();
		// clean file
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
		ScmFileUtils.cleanFile(wsp, cond);
		List<SiteWrapper> siteList = ScmNetUtils.getRandomSites(wsp);
		scmSite1 = siteList.get(0);
		scmSite2 = siteList.get(1);

		sessionA = TestScmTools.createSession(scmSite1);
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		sessionB = TestScmTools.createSession(scmSite2);
		wsB = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionB);		
	}

	@Test(groups = { "fourSite" })
	private void test() throws Exception {
		// writeFileFromA
		fileId = ScmFileUtils.create(wsA, fileName, filePath);
		this.readFileFromB( wsB );		
		runSuccess = true;
	}

	@AfterClass()
	private void tearDown() {
		try {
			if (runSuccess || forceClear) {
				ScmFactory.File.deleteInstance(wsA, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
			if (sessionB != null) {
				sessionB.close();
			}

		}
	}

	private void readFileFromB(ScmWorkspace ws) throws Exception {
		OutputStream fos = null;
		ScmInputStream sis = null;	

		try{
			// read content
			ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			sis = ScmFactory.File.createInputStream(scmfile);
			fos = new FileOutputStream(new File(downloadPath));
			sis.read(fos);

			// check content
			Assert.assertEquals(TestTools.getMD5(filePath), TestTools.getMD5(downloadPath));

			// check meta and data
			SiteWrapper[] expSites = {  scmSite1, scmSite2 };
			ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);
		} finally {
			if (fos != null)
				fos.close();
			if (sis != null)
				sis.close();			
		}
	}

}
