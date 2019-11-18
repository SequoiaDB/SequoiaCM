package com.sequoiacm.batch;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @FileName SCM-1299: 获取已存在的批次实例
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class AttachFile1299 extends TestScmBase {
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private final String batchName = "batch1299";
	private final String fileName = "file1299";
	private ScmId fileId = null;
	private ScmId batchId = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException {
		SiteWrapper site = ScmInfo.getSite();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(ScmInfo.getWs().getName(), session);

		ScmFile file = ScmFactory.File.createInstance(ws);
        file.setFileName(fileName);
        fileId = file.save();
	}

	// TODO: fail for SEQUOIACM-242
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		ScmBatch batch = ScmFactory.Batch.createInstance(ws);
		batch.setName(batchName);
		batchId = batch.save();
        batch.attachFile(fileId);

        List<ScmFile> files = batch.listFiles();
		Assert.assertEquals(files.size(), 1);
		Assert.assertEquals(files.get(0).getFileName(), fileName);

		try {
			ScmFactory.File.deleteInstance(ws, fileId, true);
			Assert.fail("file should not be deleted when it belongs to batch");
		} catch (ScmException e) {
			Assert.assertEquals(e.getError(), ScmError.FILE_IN_ANOTHER_BATCH, e.getMessage());
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		ScmFactory.Batch.deleteInstance(ws, batchId);
        if (session != null)
            session.close();
	}
}