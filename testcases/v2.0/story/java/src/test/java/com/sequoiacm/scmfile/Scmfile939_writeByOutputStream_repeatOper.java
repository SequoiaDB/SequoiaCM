package com.sequoiacm.scmfile;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Testcase: SCM-939:重复write.commit/write.cancel
 * @author huangxiaoni init
 * @date 2017.9.21
 */

public class Scmfile939_writeByOutputStream_repeatOper extends TestScmBase {
	private boolean runSuccess1 = false;
	private boolean runSuccess2 = false;
	private boolean runSuccess3 = false;
	private boolean runSuccess4 = false;

	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private String fileName = "Scmfile939";
	private String title = "Scmfile939";
	private List<ScmId> fileIdList = new CopyOnWriteArrayList<>();
	private int fileSize = 200 * 1024;
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

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
			ScmFileUtils.cleanFile(wsp, cond);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/*
	 * 1）重复执行commit提交；
	 */
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testRepeatCommit() throws Exception {
		ScmOutputStream sos = null;
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName+"_"+UUID.randomUUID());
			String author = fileName + "_testRepeatCommit";
			file.setTitle(title);
			file.setAuthor(author);
			sos = ScmFactory.File.createOutputStream(file);
			byte[] buffer = TestTools.getBuffer(filePath);
			sos.write(buffer);
			sos.commit();
			ScmId fileId = file.getFileId();
			fileIdList.add(fileId);

			try {
				sos.commit();
				Assert.fail("exp fail but act success");
			} catch (ScmException e) {
				if (e.getError() != ScmError.OUTPUT_STREAM_CLOSED) { // EN_SCM_OUTPUTSTREAM_IS_CLOSED(-610)
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}
			//check file count
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
			Assert.assertEquals(count, 1);

			// check results
			ScmFile file2 = ScmFactory.File.getInstance(ws, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			file2.getContent(downloadPath);
			Assert.assertEquals(TestTools.getMD5(downloadPath), TestTools.getMD5(filePath));
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess1 = true;
	}

	/*
	 * 2）重复执行cancel取消；
	 */
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testRepeatCancel2() throws Exception {
		ScmOutputStream sos = null;
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName+"_"+UUID.randomUUID());
			file.setTitle(title);
			String author = fileName + "_" + "testRepeatCancel2";
			file.setAuthor(author);
			sos = ScmFactory.File.createOutputStream(file);
			byte[] buffer = TestTools.getBuffer(filePath);
			sos.write(buffer);

			sos.commit();
			ScmId fileId = file.getFileId();
			fileIdList.add(fileId);

			sos.cancel();

			//check file count
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			//SEQUOIACM-414
			for(int i = 0; i < 6;i++) {
				Thread.sleep(10);
				long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
				Assert.assertEquals(count, 1);
			}

			// check results
			ScmFile file2 = ScmFactory.File.getInstance(ws, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			file2.getContent(downloadPath);
			Assert.assertEquals(TestTools.getMD5(downloadPath), TestTools.getMD5(filePath));
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess2 = true;
	}

	/*
	 * 3）执行commit后再次执行cancel;
	 */
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testRepeatCancel() throws IOException, InterruptedException {
		ScmOutputStream sos = null;
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName+"_"+UUID.randomUUID());
			String author = fileName + "_testRepeatCancel";
			file.setAuthor(author);
			file.setTitle(title);
			sos = ScmFactory.File.createOutputStream(file);
			byte[] buffer = TestTools.getBuffer(filePath);
			sos.write(buffer);

			sos.cancel();
			sos.cancel();

			// check results
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			//SEQUOIACM-414
			for(int i = 0; i < 6; i++) {
				Thread.sleep(10);
				long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
				Assert.assertEquals(count, 0);
			}
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess3 = true;
	}

	/*
	 * 4）执行cancel后再次执行commit;
	 */
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testRepeatCommit2() throws Exception {
		ScmOutputStream sos = null;
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName+"_"+UUID.randomUUID());
			String author = fileName + "_testRepeatCommit2";
			file.setAuthor(author);
			file.setTitle(title);
			sos = ScmFactory.File.createOutputStream(file);
			byte[] buffer = TestTools.getBuffer(filePath);
			sos.write(buffer);
			sos.cancel();
			try {
				sos.commit();
				Assert.fail("exp fail but act success");
			} catch (ScmException e) {
				if (e.getError() != ScmError.OUTPUT_STREAM_CLOSED) { // EN_SCM_OUTPUTSTREAM_IS_CLOSED(-610)
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}

			// check results
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			//SEQUOIACM-414
			for(int i = 0; i < 6; i++) {
				Thread.sleep(10);
				long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
				Assert.assertEquals(count, 0);
			}
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess4 = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if ((runSuccess1 && runSuccess2 && runSuccess3 && runSuccess4) || TestScmBase.forceClear) {
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(ws, fileId, true);
				}
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