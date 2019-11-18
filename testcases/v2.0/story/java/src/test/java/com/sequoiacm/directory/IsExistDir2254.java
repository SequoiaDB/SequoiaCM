/**
 * 
 */
package com.sequoiacm.directory;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-2254:创建和删除文件夹，查看文件夹是否存在
 * @author fanyu
 * @Date:2018年09月25日
 * @version:1.0
 */
public class IsExistDir2254 extends TestScmBase {
	private boolean runSuccess;
	private ScmSession session;
	private ScmWorkspace ws;
	private SiteWrapper site;
	private WsWrapper wsp;
	private String dirBasePath = "/IsExistDir2254";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);	
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws ScmException {
		//directory exist
		ScmFactory.Directory.createInstance(ws, dirBasePath);
		boolean flag = ScmFactory.Directory.isInstanceExist(ws,dirBasePath);
		Assert.assertTrue(flag);
		
		//directory no exist
		ScmFactory.Directory.deleteInstance(ws, dirBasePath);
		boolean flag1 = ScmFactory.Directory.isInstanceExist(ws, dirBasePath);
		Assert.assertFalse(flag1);
		
		//create same directory again
		ScmFactory.Directory.createInstance(ws, dirBasePath);
		boolean flag3 = ScmFactory.Directory.isInstanceExist(ws, dirBasePath);
		Assert.assertTrue(flag3);
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.Directory.deleteInstance(ws, dirBasePath);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}
}
