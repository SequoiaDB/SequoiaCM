
package com.sequoiacm.auth;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description: SCM-1564 :: createRole参数校验 
 * @author fanyu
 * @Date:2018年5月22日
 * @version:1.0
 */
public class AuthServer_Param_CreateRole1564 extends TestScmBase{
	private SiteWrapper site;
	private ScmSession session;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			session = TestScmTools.createSession(site);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testRoleNameExist() throws ScmException {
		String roleName = "CreateRole1564";
		ScmRole role = null;
		try {
			role = ScmFactory.Role.createRole(session, roleName, null);
			ScmFactory.Role.createRole(session, roleName, null);
			Assert.fail("exp fail but act success");
		} catch (ScmException e) {
			if (e.getError() != ScmError.HTTP_BAD_REQUEST) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}finally{
			if(role != null){
				ScmFactory.Role.deleteRole(session, role);
			}
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testRoleIsNull() {
		String roleName = null;
		try {
			ScmFactory.Role.createRole(session, roleName, null);
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

