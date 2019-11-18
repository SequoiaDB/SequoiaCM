
package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
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
 * @Description:SCM-1199:ScmFactory.Directory中的deleteInstance参数校验
 * @author fanyu
 * @Date:2018年4月27日
 * @version:1.0
 */
public class DeleteInstance_Param1199 extends TestScmBase {
	private boolean runSuccess1;
	private boolean runSuccess2;
	private boolean runSuccess3;
	private boolean runSuccess4;
	private ScmSession session;
	private ScmWorkspace ws;
	private SiteWrapper site;
	private WsWrapper wsp;
	private String dirBasePath = "/DeleteInstance_Param1199";
	private ScmDirectory dir;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			//ScmFactory.Directory.deleteInstance(ws, dirBasePath);
			deleteDir(ws, dirBasePath);
			dir = ScmFactory.Directory.createInstance(ws, dirBasePath);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testChineseDir1() {
		String pathName = "1199_文件夹a";
		try {
			dir.createSubdirectory(pathName);
			ScmDirectory subdir1 = dir.getSubdirectory(pathName);
			Assert.assertEquals(subdir1.getPath(), dirBasePath + "/" + pathName + "/");
			subdir1.delete();
			ScmDirectory subdir2 = dir.getSubdirectory(pathName);
			Assert.assertEquals(subdir2, null);
		} catch (ScmException e) {
			if (e.getError() != ScmError.DIR_NOT_FOUND) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		runSuccess1 = true;
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testChineseDir2() {
		String pathName = "1199_文件夹b";
		try {
			dir.createSubdirectory(pathName);
			ScmDirectory subdir1 = dir.getSubdirectory(pathName);
			Assert.assertEquals(subdir1.getPath(), dirBasePath + "/" + pathName + "/");
			ScmFactory.Directory.deleteInstance(ws, dirBasePath + "/" + pathName);
			ScmDirectory subdir2 = dir.getSubdirectory(pathName);
			Assert.assertEquals(subdir2, null);
		} catch (ScmException e) {
			if (e.getError() != ScmError.DIR_NOT_FOUND) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		runSuccess2 = true;
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testWsIsNull() {
		try {
			ScmFactory.Directory.deleteInstance(null, dirBasePath);
			Assert.fail("expect fail but act success," + dir.toString());
		} catch (ScmException e) {
			if (e.getError() != ScmError.INVALID_ARGUMENT) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		runSuccess3 = true;
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testPaPathInexist() {
		String paPath = dirBasePath + "/testPaPathInexist_1199";
		try {
			ScmDirectory dir = ScmFactory.Directory.createInstance(ws, paPath);
			ScmDirectory subdir = dir.createSubdirectory("1199_a");
			subdir.delete();
			dir.delete();
			ScmFactory.Directory.deleteInstance(ws, paPath + "/1199_a");
			ScmDirectory checkDir = ScmFactory.Directory.getInstance(ws, paPath + "/1199_a");
			Assert.assertEquals(checkDir, null);
		} catch (ScmException e) {
			if (e.getError() != ScmError.DIR_NOT_FOUND) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		runSuccess4 = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess1 && runSuccess2 && runSuccess3 && runSuccess4 || TestScmBase.forceClear) {
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
	
	private void deleteDir(ScmWorkspace ws, String dirPath) {
		List<String> pathList = getSubPaths(dirPath);
		for (int i = pathList.size() - 1; i >= 0; i--) {
			try {
				ScmFactory.Directory.deleteInstance(ws, pathList.get(i));
			} catch (ScmException e) {
				if (e.getError() != ScmError.DIR_NOT_FOUND
						&& e.getError() != ScmError.DIR_NOT_EMPTY) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}
		}
	}

	private List<String> getSubPaths(String path) {
		String ele = "/";
		String[] arry = path.split("/");
		List<String> pathList = new ArrayList<String>();
		for (int i = 1; i < arry.length; i++) {
			ele = ele + arry[i];
			pathList.add(ele);
			ele = ele + "/";
		}
		return pathList;
	}
}
