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
 * @FileName SCM-1300: 添加已存在本批次的文件
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class AttachFile1300 extends TestScmBase {
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private final String batchName = "batch1300";
	private final String fileName = "file1300";
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

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		ScmBatch batch = ScmFactory.Batch.createInstance(ws);
		batch.setName(batchName);
		batchId = batch.save();
        batch.attachFile(fileId);

        try {
			batch.attachFile(fileId);
			Assert.fail("attach a file again should not succeed");
		} catch (ScmException e) {
		}
        List<ScmFile> files = batch.listFiles();
		Assert.assertEquals(files.size(), 1);
		Assert.assertEquals(files.get(0).getFileName(), fileName);
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		ScmFactory.Batch.deleteInstance(ws, batchId);
        if (session != null)
            session.close();
	}
}