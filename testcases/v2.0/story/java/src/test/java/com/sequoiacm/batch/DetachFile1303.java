package com.sequoiacm.batch;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
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
 * @FileName SCM-1303: 解除属于本批次的文件
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class DetachFile1303 extends TestScmBase {
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private final String batchName = "batch1303";
	private final String fileName = "file1303";
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

		ScmBatch batch = ScmFactory.Batch.createInstance(ws);
		batch.setName(batchName);
		batchId = batch.save();
		batch.attachFile(fileId);
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
	    ScmBatch batch = ScmFactory.Batch.getInstance(ws, batchId);
		batch.detachFile(fileId);
        List<ScmFile> files = batch.listFiles();
		Assert.assertEquals(files.size(), 0);
		ScmFactory.File.deleteInstance(ws, fileId, true);
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		ScmFactory.Batch.deleteInstance(ws, batchId);
        if (session != null)
            session.close();
	}
}