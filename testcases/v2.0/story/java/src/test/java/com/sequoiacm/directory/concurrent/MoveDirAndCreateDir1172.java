
package com.sequoiacm.directory.concurrent;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;

import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1172 :: 移动文件夹和创建同名文件夹 
 * @author fanyu
 * @Date:2018年4月27日
 * @version:1.0
 */
public class MoveDirAndCreateDir1172 extends TestScmBase{
	private boolean runSuccess;
	private ScmSession session;
	private ScmWorkspace ws;
	private SiteWrapper site;
	private WsWrapper wsp;
	private String dirBasePath = "/MoveDirAndCreateDir1172";
	private String fullPath1 = dirBasePath + "/Dir_1172_a";
	private String fullPath2 = dirBasePath + "/Dir_1172_b/Dir_1172_c";
	private String newPath = dirBasePath + "/Dir_1172_a/Dir_1172_b/Dir_1172_c";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			deleteDir(ws, newPath);
			createDir(ws, fullPath1);
			createDir(ws, fullPath2);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() {
		MoveDir rThread = new MoveDir();
		CreateDir cThread = new CreateDir();
		rThread.start();
		cThread.start();
		boolean rflag = rThread.isSuccess();
		boolean cflag = cThread.isSuccess();
		Assert.assertEquals(rflag, true, rThread.getErrorMsg());
		Assert.assertEquals(cflag, true, cThread.getErrorMsg());
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				deleteDir(ws, newPath);
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

	private class MoveDir extends TestThreadBase {
		@Override
		public void exec() {
			try {
				String srcPath = dirBasePath + "/Dir_1172_b";
				ScmDirectory destDir = ScmFactory.Directory.getInstance(ws, fullPath1);
				ScmDirectory srcDir = ScmFactory.Directory.getInstance(ws, srcPath);
				srcDir.move(destDir);
				// check
				BSONObject expBSON1 = new BasicBSONObject();
				expBSON1.put("name", "Dir_1172_c");
				expBSON1.put("path", newPath + "/");
				expBSON1.put("wsName", wsp.getName());
				expBSON1.put("paName", "Dir_1172_b");
				checkDir(newPath, expBSON1);
			} catch (ScmException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	private class CreateDir extends TestThreadBase {
		public void exec() {
			try {
				String path = dirBasePath + "/Dir_1172_b";
				ScmFactory.Directory.createInstance(ws, path);
				// check
				BSONObject expBSON = new BasicBSONObject();
				expBSON.put("name", "Dir_1172_b");
				expBSON.put("path", path + "/");
				expBSON.put("wsName", wsp.getName());
				expBSON.put("paName", "MoveDirAndCreateDir1172");
				checkDir(path, expBSON);
			} catch (ScmException e) {
				if (e.getError() != ScmError.DIR_EXIST) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}
		}
	}

	private void checkDir(String path, BSONObject expBSON) {
		try {
			ScmDirectory dir = ScmFactory.Directory.getInstance(ws, path);
			Assert.assertEquals(dir.getName(), expBSON.get("name"));
			Assert.assertEquals(dir.getPath(), expBSON.get("path"));
			Assert.assertEquals(dir.getUpdateUser(), TestScmBase.scmUserName);
			Assert.assertEquals(dir.getUser(), TestScmBase.scmUserName);
			Assert.assertEquals(dir.getWorkspaceName(), expBSON.get("wsName"));
			Assert.assertNotNull(dir.getCreateTime());
			Assert.assertNotNull(dir.getUpdateTime());
			Assert.assertEquals(dir.getParentDirectory().getName(), expBSON.get("paName"));
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
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
