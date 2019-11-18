
package com.sequoiacm.definemeta.concurrent;

import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-1889 :: 删除属性和更新属性并发 
 * @author fanyu
 * @Date:2018年7月6日
 * @version:1.0
 */
public class DefineAttr_Attr_DeleteAndUpdate1889 extends TestScmBase{
	//private boolean runSuccess = false;
	private String attrname = "DeleteAndUpdate1889";
	private String desc = "DeleteAndUpdate1889";
	private ScmAttribute attr = null;
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
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			craeteAttr();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		Delete dThread = new Delete();
		Update uThread = new Update();
		dThread.start(5);
		uThread.start(5);
		boolean dflag = dThread.isSuccess();
		boolean uflag = uThread.isSuccess();	
		Assert.assertEquals(dflag, true, dThread.getErrorMsg());
		Assert.assertEquals(uflag, true, uThread.getErrorMsg());
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		if (session != null) {
			session.close();
		}
	}
	
	private class Delete extends TestThreadBase {
		@Override
		public void exec() {
			try {
				ScmFactory.Attribute.deleteInstance(ws, attr.getId());
			} catch (ScmException e) {
				if (e.getError() != ScmError.METADATA_ATTR_NOT_EXIST) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}
		}
	}

	private class Update extends TestThreadBase {
		@Override
		public void exec() {
			ScmAttribute attr1 = null;
			try {
				attr1 = ScmFactory.Attribute.getInstance(ws, attr.getId());
				attr1.setDescription(desc + "_" + UUID.randomUUID());
				attr1.setDisplayName(desc + "_" + UUID.randomUUID());
				attr1.setRequired(true);
			} catch (ScmException e) {
				if (e.getError() != ScmError.METADATA_ATTR_NOT_EXIST) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}
		}
	}
	
	private void craeteAttr() {
		ScmAttributeConf conf = new ScmAttributeConf();
		try {
			conf.setName(attrname);
			conf.setDescription(desc);
			conf.setDisplayName(attrname + "_display");
			conf.setRequired(true);
			conf.setType(AttributeType.INTEGER);

			ScmIntegerRule rule = new ScmIntegerRule();
			rule.setMinimum(0);
			rule.setMaximum(10);
			conf.setCheckRule(rule);

			attr = ScmFactory.Attribute.createInstance(ws, conf);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}
