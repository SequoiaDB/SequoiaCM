package com.sequoiacm.batch;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @FileName SCM-1289: 更改批次属性后再次查询批次
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class UpdateBatchAttr1289 extends TestScmBase {
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private final String oldBatchName = "batch1289_old";
	private final String newBatchName = "batch1289_new";
	private final int fileNum = 5;
	private List<ScmId> fileIdList = new ArrayList<>(fileNum);
	private ScmId batchId = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException {
		SiteWrapper site = ScmInfo.getSite();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(ScmInfo.getWs().getName(), session);

		ScmBatch batch = ScmFactory.Batch.createInstance(ws);
		batch.setName(oldBatchName);
		batchId = batch.save();
		for (int i = 0; i < fileNum; ++i) {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName("file1289_" + i);
			ScmId fileId = file.save();
			fileIdList.add(fileId);
			batch.attachFile(fileId);
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(ws, new BasicBSONObject("id", batchId.get()));
	    ScmBatchInfo info = cursor.getNext();
	    Assert.assertEquals(info.getName(), oldBatchName);
	    Assert.assertEquals(info.getFilesCount(), fileNum);
	    Assert.assertEquals(info.getId().get(), batchId.get());
	    Assert.assertFalse(cursor.hasNext());
	    cursor.close();

		ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
		batch.setName(newBatchName);
		for (ScmId fileId : fileIdList) {
			batch.detachFile(fileId);
		}

		cursor = ScmFactory.Batch.listInstance(ws, new BasicBSONObject("id", batchId.get()));
		info = cursor.getNext();
		Assert.assertEquals(info.getName(), newBatchName);
		Assert.assertEquals(info.getFilesCount(), 0);
		Assert.assertEquals(info.getId().get(), batchId.get());
		Assert.assertFalse(cursor.hasNext());
		cursor.close();
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		for (ScmId fileId : fileIdList) {
			ScmFactory.File.deleteInstance(ws, fileId, true);
		}
		ScmFactory.Batch.deleteInstance(ws, batchId);
        if (session != null) {
            session.close();
        }
	}
}