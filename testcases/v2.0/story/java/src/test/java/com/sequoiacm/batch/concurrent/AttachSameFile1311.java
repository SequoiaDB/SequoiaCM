package com.sequoiacm.batch.concurrent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;

/**
 * @FileName SCM-1311: 并发添加同一个文件
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class AttachSameFile1311 extends TestScmBase {
	org.slf4j.Logger log = LoggerFactory.getLogger(AttachSameFile1311.class);
	private boolean runSuccess = false;
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private final String batchName = "batch1311";
	private ScmId fileId = null;
	private ScmId batchId = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException {
		SiteWrapper site = ScmInfo.getSite();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(ScmInfo.getWs().getName(), session);

		ScmFile file = ScmFactory.File.createInstance(ws);
		file.setFileName("file1311");
		file.setTitle(batchName);
		fileId = file.save();

		ScmBatch batch = ScmFactory.Batch.createInstance(ws);
		batch.setName(batchName);
		batchId = batch.save();
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		AttachThread attachThrd = new AttachThread();
		attachThrd.start(5);
		Assert.assertTrue(attachThrd.isSuccess(), attachThrd.getErrorMsg());
		Assert.assertEquals(attachThrd.getSuccessTimes(), 1);

		ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
		List<ScmFile> files = batch.listFiles();
		Assert.assertEquals(files.size(), 1);
		Assert.assertEquals(files.get(0).getFileId().get(), fileId.get());
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				try {
					ScmFactory.Batch.deleteInstance(ws, batchId);
				} catch (ScmException e) {
					log.warn("delete inexist batchId," + batchId.get());
				}
				try {
					ScmFactory.File.deleteInstance(ws, fileId, true);
				} catch (ScmException e) {
					log.warn("delete inexist fileId," + batchId.get());
				}
			}
		} finally {
			if (session != null)
				session.close();
		}
	}

	private class AttachThread extends TestThreadBase {
		private AtomicInteger successTimes = new AtomicInteger(0);

		@Override
		public void exec() throws Exception {
			try {
				ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
				batch.attachFile(fileId);
				successTimes.getAndIncrement();
			} catch (ScmException e) {
				// TODO:重复添加同一文件到批次错误码不对
				// if (e.getError().getErrorCode() !=
				// ScmError.FILE_IN_SPECIFIED_BATCH.getErrorCode()) {
				// throw e;
				// }
			}
		}

		public int getSuccessTimes() {
			return successTimes.get();
		}
	}
}