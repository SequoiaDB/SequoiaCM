package com.sequoiacm.scmfile;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @Testcase: SCM-938:ScmOutputStream.cancel取消写入文件
 * @author huangxiaoni init
 * @date 2017.9.21
 */

public class Scmfile938_writeByOutputStream_cancel extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private String fileName = "Scmfile938";
	private int fileSize = 200;
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

			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws IOException, InterruptedException {
		ScmOutputStream sos = null;
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName+"_"+UUID.randomUUID());
			String author = fileName;
			file.setAuthor(author);
			sos = ScmFactory.File.createOutputStream(file);
			byte[] buffer = TestTools.getBuffer(filePath);
			sos.write(buffer);
			sos.cancel();
			// check results
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			//SEQUOIACM-414
			for(int i = 0; i < 6;i++) {
				Thread.sleep(10);
				long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
				Assert.assertEquals(count, 0);
			}
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}
}