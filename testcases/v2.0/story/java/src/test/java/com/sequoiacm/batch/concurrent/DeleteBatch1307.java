package com.sequoiacm.batch.concurrent;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @FileName SCM-1307: 并发删除同一批次
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class DeleteBatch1307 extends TestScmBase {
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private final String batchName = "batch1307";
	private final int fileNum = 30;
	private List<ScmId> fileIdList = new ArrayList<>(fileNum);
	private ScmId batchId = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException {
		SiteWrapper site = ScmInfo.getSite();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(ScmInfo.getWs().getName(), session);

		for (int i = 0; i < fileNum; ++i) {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName("file1307_" + i);
			file.setTitle(batchName);
			ScmId fileId = file.save();
			fileIdList.add(fileId);
		}

		ScmBatch batch = ScmFactory.Batch.createInstance(ws);
		batch.setName(batchName);
		batchId = batch.save();
		for (ScmId fileId : fileIdList) {
			batch.attachFile(fileId);
		}
	}

	// have ever fail for SEQUOIACM-249
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		DeleteThread delThrd = new DeleteThread();
		delThrd.start(10);
		Assert.assertTrue(delThrd.isSuccess(), delThrd.getErrorMsg());

		ScmCursor<ScmBatchInfo> batchCursor = ScmFactory.Batch.listInstance(ws,
				new BasicBSONObject("id", batchId.get()));
		Assert.assertFalse(batchCursor.hasNext());
		batchCursor.close();

		ScmCursor<ScmFileBasicInfo> fileCursor = ScmFactory.File.listInstance(ws, ScmType.ScopeType.SCOPE_CURRENT,
				new BasicBSONObject("title", batchName));
		Assert.assertFalse(fileCursor.hasNext());
		fileCursor.close();
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		if (session != null)
			session.close();
	}

	private class DeleteThread extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			try {
				ScmFactory.Batch.deleteInstance(ws, batchId);
			} catch (ScmException e) {
				if (e.getError() != ScmError.BATCH_NOT_FOUND) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}
		}
	}
}