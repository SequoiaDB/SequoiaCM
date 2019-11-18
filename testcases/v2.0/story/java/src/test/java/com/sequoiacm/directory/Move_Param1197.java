
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
 * @Description:SCM-1197 :: ScmDirectory中的move参数校验 
 * @author fanyu
 * @Date:2018年4月27日
 * @version:1.0
 */
public class Move_Param1197 extends TestScmBase{
	private boolean runSuccess1;
	private boolean runSuccess2;
	private boolean runSuccess3;
	private ScmSession session;
	private ScmWorkspace ws;
	private SiteWrapper site;
	private WsWrapper wsp;
	private String dirBasePath = "/Move_Param1197";
	private String fullPath1 = dirBasePath + "/文件夹a_1197";
	private String fullPath2 = dirBasePath + "/1197_e/1197_f/1197_g";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			deleteDir(ws,fullPath1+"/1197_e/1197_f/1197_g");
			createDir(ws, fullPath1);
			createDir(ws, fullPath2);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testMove2ChineseDir() {
		String path = dirBasePath + "/1197_e";
		String newPaPath = fullPath1;
		try {
			ScmDirectory subDir = ScmFactory.Directory.getInstance(ws, path);
			subDir.move(ScmFactory.Directory.getInstance(ws, newPaPath));
			String expPath =  dirBasePath + "/文件夹a_1197/1197_e/1197_f/1197_g/";
			ScmDirectory subDir1 =  ScmFactory.Directory.getInstance(ws,expPath);
			Assert.assertEquals(subDir1.getPath(), expPath);
			String noexistPath = fullPath2;
			ScmDirectory subdir2 = ScmFactory.Directory.getInstance(ws, noexistPath);
			Assert.fail("expect fail but success," + subdir2.toString());
		} catch (ScmException e) {
			if (e.getError() != ScmError.DIR_NOT_FOUND) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		runSuccess1 = true;
	}
	
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testMoveToDirIsNull() throws ScmException {
		String path = fullPath1;
		try {
			ScmDirectory dir = ScmFactory.Directory.getInstance(ws, path);
			dir.move(null);
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
	private void testMoveToDirIsInexist() throws ScmException {
		String path = fullPath1;
		String inexistPath = dirBasePath + "/1197_f";
		try {
			ScmDirectory destDir = ScmFactory.Directory.createInstance(ws, inexistPath);
			destDir.delete();
			ScmDirectory dir = ScmFactory.Directory.getInstance(ws, path);
			dir.move(destDir);
			Assert.fail("expect fail but autal successsfully " + dir.toString());
		} catch (ScmException e) {
			if (e.getError() != ScmError.DIR_NOT_FOUND) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		runSuccess3 = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess1 && runSuccess2 && runSuccess3 || TestScmBase.forceClear) {
				deleteDir(ws, fullPath1+"/1197_e/1197_f/1197_g");
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



