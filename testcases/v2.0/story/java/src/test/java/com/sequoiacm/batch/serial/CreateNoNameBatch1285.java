package com.sequoiacm.batch.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @FileName SCM-1285: 未设置BathName创建批次
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class CreateNoNameBatch1285 extends TestScmBase {
	private ScmSession session = null;
	private ScmWorkspace ws = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException {
		SiteWrapper site = ScmInfo.getRootSite();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(ScmInfo.getWs().getName(), session);
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
	    int beforeNum = countBatch(ws);
	    try {
			ScmBatch batch = ScmFactory.Batch.createInstance(ws);
			batch.save();
			Assert.fail("creating no name batch should not succeed");
		} catch (ScmException e) {
	    	Assert.assertEquals(e.getErrorCode(), ScmError.INVALID_ARGUMENT.getErrorCode(), e.getMessage());
		}
		int afterNum = countBatch(ws);
		Assert.assertEquals(afterNum, beforeNum);
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
        if (session != null)
            session.close();
	}

	private int countBatch(ScmWorkspace ws) throws Exception {
		ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(ws, new BasicBSONObject());
		int total = 0;
		while (cursor.hasNext()) {
			cursor.getNext();
			++total;
		}
		cursor.close();
		return total;
	}
}