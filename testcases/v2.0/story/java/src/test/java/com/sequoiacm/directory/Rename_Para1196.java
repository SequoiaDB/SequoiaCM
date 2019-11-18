
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
 * @Description: SCM-1196 :: ScmDirectory中的rename参数校验 
 * @author fanyu
 * @Date:2018年4月26日
 * @version:1.0
 */
public class Rename_Para1196 extends TestScmBase{
	private boolean runSuccess1;
	private boolean runSuccess2;
	private boolean runSuccess3;
	private ScmSession session;
	private ScmWorkspace ws;
	private SiteWrapper site;
	private WsWrapper wsp;
	private String dirBasePath = "/Rename_Para1196";
	private String fullPath1 = dirBasePath + "/1196_a/1196_b/1196_c";
	private String fullPath2 = dirBasePath + "/1196_e/1196_f/1196_g";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			deleteDir(ws,fullPath1);
			deleteDir(ws,fullPath2);
			createDir(ws, fullPath1);
			createDir(ws, fullPath2);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testRenameChinese() {
		String path = dirBasePath + "/1196_a/1196_b";
		String newName = "文件夹a_1196";
		String newPath = dirBasePath + "/1196_a/" + newName;
		try {
			ScmDirectory subDir = ScmFactory.Directory.getInstance(ws, path);
			subDir.rename(newName);
			ScmDirectory subDir1 = ScmFactory.Directory.getInstance(ws,newPath);
			Assert.assertEquals(subDir1.getName(), newName);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess1 = true;
	}
	
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testRename1() throws ScmException {
		String path = fullPath2;
		String newName = "/";
		try {
			ScmDirectory dir = ScmFactory.Directory.getInstance(ws, path);
			dir.rename(newName);
			Assert.fail("expect fail but autal successsfully " + dir.toString());
		} catch (ScmException e) {
			if (e.getError() != ScmError.INVALID_ARGUMENT) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		runSuccess2 = true;
	}
	
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testRename2() throws ScmException {
		String path = fullPath2;
		String newName = ".";
		try {
			ScmDirectory dir = ScmFactory.Directory.getInstance(ws, path);
			dir.rename(newName);
			Assert.fail("expect fail but autal successsfully " + dir.toString());
		} catch (ScmException e) {
			if (e.getError() != ScmError.INVALID_ARGUMENT) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		runSuccess3 = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess1 || runSuccess2 || runSuccess3 || TestScmBase.forceClear) {
				deleteDir(ws, dirBasePath + "/1196_a/文件夹a_1196/1196_c");
				deleteDir(ws, fullPath2);
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
	
	private ScmDirectory createDir(ScmWorkspace ws, String dirPath) throws ScmException {
		List<String> pathList = getSubPaths(dirPath);
		for (String path : pathList) {
			try {
				ScmFactory.Directory.createInstance(ws, path);
			} catch (ScmException e) {
				if (e.getError() != ScmError.DIR_EXIST) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}
		}
		return ScmFactory.Directory.getInstance(ws, pathList.get(pathList.size()-1));
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


