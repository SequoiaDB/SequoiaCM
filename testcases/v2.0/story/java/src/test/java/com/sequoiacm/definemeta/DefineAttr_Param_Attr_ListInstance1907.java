
package com.sequoiacm.definemeta;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1907 :: Attribute.listInstance参数校验 
 * @author fanyu
 * @Date:2018年7月7日
 * @version:1.0
 */
public class DefineAttr_Param_Attr_ListInstance1907  extends TestScmBase{
	private SiteWrapper site = null;
	private WsWrapper wsp = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	
	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession(site);
            ws  = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
		
	
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testWsIsNull() {
		// create
		try {
			ScmFactory.Attribute.listInstance(null, new BasicBSONObject());
			Assert.fail("exp fail but act success");
		} catch (ScmException e) {
			if (e.getError() != ScmError.INVALID_ARGUMENT) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}
	
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testFileterIsNull() {
		// create
		try {
			ScmFactory.Attribute.listInstance(ws, null);
			Assert.fail("exp fail but act success");
		} catch (ScmException e) {
			if (e.getError() != ScmError.INVALID_ARGUMENT) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		if (session != null) {
			session.close();
		}
	}
}
