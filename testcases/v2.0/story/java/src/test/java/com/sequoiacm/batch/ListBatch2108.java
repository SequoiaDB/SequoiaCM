package com.sequoiacm.batch;

import java.util.ArrayList;
import java.util.List;

import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @FileName SCM-2108:多次列取批次,获取多个游标对象,多次操作
 * @Author linsuqiang
 * @Date 2018-04-19
 * @Version 1.00
 */

public class ListBatch2108 extends TestScmBase {
	private ScmSession session = null;
	private ScmWorkspace ws = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException {
		SiteWrapper site = ScmInfo.getSite();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(ScmInfo.getWs().getName(), session);
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		List<ScmCursor<ScmBatchInfo>> list = new ArrayList<ScmCursor<ScmBatchInfo>>();
		for (int i = 0; i < 10; i++) {
			ScmCursor<ScmBatchInfo> cursor1 = ScmFactory.Batch.listInstance(ws, new BasicBSONObject());
			list.add(cursor1);
		}
		for (ScmCursor<ScmBatchInfo> cursor : list) {
			cursor.hasNext();
		}
		for (ScmCursor<ScmBatchInfo> cursor : list) {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
        if (session != null)
            session.close();
	}
}