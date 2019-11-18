
package com.sequoiacm.auth;

import java.io.IOException;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-1727 :: 无目录资源的权限，对表格中各个接口进行覆盖测试 
 * @author fanyu
 * @Date:2018年6月12日
 * @version:1.0
 */
public class AuthDir_None1727 extends TestScmBase{
	private SiteWrapper site;
	private WsWrapper wsp;
	private ScmSession sessionA;
	private ScmSession session;
	private ScmWorkspace ws;
	private String username = "AuthDir_None1727";
	private String passwd = "1727";
	private ScmUser user;
	
	@BeforeClass(alwaysRun = true)
	private void setUp() throws InterruptedException, IOException {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			sessionA = TestScmTools.createSession(site);
			try {
				ScmFactory.User.deleteUser(sessionA, username);
			} catch (ScmException e) {
				if (e.getError() != ScmError.HTTP_NOT_FOUND) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}
		} catch (ScmException e) {
			e.printStackTrace();
		}
		try {
			user = ScmFactory.User.createUser(sessionA, username, ScmUserPasswordType.LOCAL, passwd);
			session = TestScmTools.createSession(site, username, passwd);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testListDir(){
		ScmCursor<ScmDirectory> cursor = null;
	      try {
	    	  cursor = ScmFactory.Directory.listInstance(ws, new BasicBSONObject());
	    	  Assert.assertNotNull(cursor);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally{
			if(cursor != null){
				cursor.close();
			}
		}
	}
	
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testListSche(){
		ScmCursor<ScmScheduleBasicInfo> cursor = null;
	      try {
	    	  cursor = ScmSystem.Schedule.list(session, new BasicBSONObject());
	    	  Assert.assertNotNull(cursor);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally{
			if(cursor != null){
				cursor.close();
			}
		}
	}
	
	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			ScmFactory.User.deleteUser(sessionA, user);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
		}
	}
}

