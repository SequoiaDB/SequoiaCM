package com.sequoiacm.batch;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Description: SCM-2594:分页listInstances()参数校验
 * @author fanyu
 * @Date:2019年8月28日
 * @version:1.0
 */
public class ListBatch2594 extends TestScmBase {
	private SiteWrapper site;
	private WsWrapper wsp;
	private ScmSession session;
	private ScmWorkspace ws;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws Exception {
		site = ScmInfo.getSite();
		wsp = ScmInfo.getWs();
		session = TestScmTools.createSession(site);
		ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
	}

	@Test
	private void test() throws Exception {
		try {
			ScmFactory.Batch.listInstance(ws, new BasicBSONObject(), null, -1, 1);
			Assert.fail("exp fail but act success");
		}catch (ScmException e){
			if(e.getError() != ScmError.INVALID_ARGUMENT) {
				throw e;
			}
		}

		try {
			ScmFactory.Batch.listInstance(ws, new BasicBSONObject(), null, 1, -2);
			Assert.fail("exp fail but act success");
		}catch (ScmException e){
			if(e.getError() != ScmError.INVALID_ARGUMENT) {
				throw e;
			}
		}

		try {
			ScmFactory.Batch.listInstance(null, new BasicBSONObject(), null, 1, -1);
			Assert.fail("exp fail but act success");
		}catch (ScmException e){
			if(e.getError() != ScmError.INVALID_ARGUMENT) {
				throw e;
			}
		}

		try {
			ScmFactory.Batch.listInstance(ws, null, null, 1, -1);
			Assert.fail("exp fail but act success");
		}catch (ScmException e){
			if(e.getError() != ScmError.INVALID_ARGUMENT) {
				throw e;
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		if (session != null) {
			session.close();
		}
	}
}


