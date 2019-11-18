
package com.sequoiacm.definemeta;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:TODO
 * @author fanyu
 * @Date:2018年7月7日
 * @version:1.0
 */
public class DefineAttr_Param_Attr_GetInstance1906  extends TestScmBase{
	private String name = "GetInstance1906";
	private String desc = "Param1906   It is a test";
	private SiteWrapper site = null;
	private WsWrapper wsp = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private ScmAttribute attr  = null;
	
	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession(site);
            ws  = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
        	ScmAttributeConf conf = new ScmAttributeConf();
    		conf.setName(name);
    		conf.setDisplayName(desc);
    		conf.setDescription(desc);
    		conf.setType(AttributeType.BOOLEAN);
    		//create
    		attr = ScmFactory.Attribute.createInstance(ws, conf);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
		
	
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testWsIsNull() {
		// create
		try {
			ScmFactory.Attribute.getInstance(null, attr.getId());
			Assert.fail("exp fail but act success");
		} catch (ScmException e) {
			if (e.getError() != ScmError.INVALID_ARGUMENT) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}
	
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testIdIsNull() {
		// create
		try {
			ScmFactory.Attribute.getInstance(ws, null);
			Assert.fail("exp fail but act success");
		} catch (ScmException e) {
			if (e.getError() != ScmError.INVALID_ARGUMENT) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		ScmFactory.Attribute.deleteInstance(ws, attr.getId());
		if (session != null) {
			session.close();
		}
	}
}
