
package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-1338 :: 在多个ws下创建同名/不同名文件夹/文件
 * @author fanyu
 * @Date:2018年4月24日
 * @version:1.0
 */
public class CreateDirFileInMultiWs1338 extends TestScmBase {
	private boolean runSuccess;
	private ScmSession session;
	private int wsNum = 2;
	private ScmWorkspace ws1;
	private ScmWorkspace ws2;
	private SiteWrapper site;
	private List<WsWrapper> wspList;
	private String dirBasePath = "/CreateDirInMultiWs1338";
	private ScmDirectory scmDir1;
	private ScmDirectory scmDir2;
	private String eleName = "1338_same";
	private String eleName1 = "1338_diff";
	private List<ScmId> fileIdList = new ArrayList<ScmId>();

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException {
		if (ScmInfo.getWsNum() < wsNum) {
			throw new SkipException("ws num is less than need,skip");
		}
		site = ScmInfo.getSite();
		wspList = ScmInfo.getWss(wsNum);
		session = TestScmTools.createSession(site);
		ws1 = ScmFactory.Workspace.getWorkspace(wspList.get(0).getName(), session);
		ws2 = ScmFactory.Workspace.getWorkspace(wspList.get(1).getName(), session);

		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(eleName).get();
		ScmFileUtils.cleanFile(wspList.get(0), cond);
		ScmFileUtils.cleanFile(wspList.get(1), cond);
		deleteDir(ws1, dirBasePath);
		deleteDir(ws2, dirBasePath);

		scmDir1 = ScmFactory.Directory.createInstance(ws1, dirBasePath);
		scmDir2 = ScmFactory.Directory.createInstance(ws2, dirBasePath);
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws Exception {
		// create same file and dir in multi ws
		createDirAndFile(ws1, scmDir1);
		createDirAndFile(ws2, scmDir2);
		check(fileIdList.get(0), scmDir1, ws1);
		check(fileIdList.get(1), scmDir2, ws2);

		// create diff file and dir in multi ws
		createDiffDirAndFile(ws1, scmDir1);
		createDiffDirAndFile(ws2, scmDir2);
		check(fileIdList.get(2), scmDir1, ws1);
		check(fileIdList.get(3), scmDir2, ws2);
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				for (ScmId fileId : fileIdList) {
					try {
						ScmFactory.File.deleteInstance(ws1, fileId, true);
					} catch (ScmException e) {
						System.out.println("MSG = " + e.getMessage());
					}
					try {
						ScmFactory.File.deleteInstance(ws2, fileId, true);
					} catch (ScmException e) {
						System.out.println("MSG = " + e.getMessage());
					}
				}
				deleteDir(ws1, dirBasePath + "/" + eleName);
				deleteDir(ws2, dirBasePath + "/" + eleName);
				deleteDir(ws1, dirBasePath + "/" + eleName1 + "_" + ws1.getName());
				deleteDir(ws2, dirBasePath + "/" + eleName1 + "_" + ws2.getName());
			}
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	private void createDirAndFile(ScmWorkspace ws, ScmDirectory dir) throws ScmException {
		ScmId fileId = null;
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(eleName + "_file");
			file.setAuthor(eleName);
			file.setDirectory(dir);
			fileId = file.save();
			fileIdList.add(fileId);
			ScmFactory.Directory.createInstance(ws, dirBasePath + "/" + eleName);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	private void createDiffDirAndFile(ScmWorkspace ws, ScmDirectory dir) throws ScmException {
		ScmId fileId = null;
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(eleName + "_file" + "_" + ws.getName());
			file.setAuthor(eleName);
			file.setDirectory(dir);
			fileId = file.save();
			fileIdList.add(fileId);
			ScmFactory.Directory.createInstance(ws, dirBasePath + "/" + eleName1 + "_" + ws.getName());
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	private void check(ScmId fileId, ScmDirectory dir, ScmWorkspace ws) {
		ScmFile file;
		try {
			file = ScmFactory.File.getInstance(ws, fileId);
			Assert.assertEquals(file.getWorkspaceName(), ws.getName());
			Assert.assertEquals(file.getFileId(), fileId);
			Assert.assertNotNull(file.getFileName());
			Assert.assertEquals(file.getAuthor(), eleName);
			Assert.assertEquals(file.getMinorVersion(), 0);
			Assert.assertEquals(file.getMajorVersion(), 1);
			Assert.assertEquals(file.getUser(), TestScmBase.scmUserName);
			Assert.assertEquals(file.getUpdateUser(), TestScmBase.scmUserName);
			Assert.assertNotNull(file.getCreateTime().getTime());
			Assert.assertNotNull(file.getUpdateTime());
			Assert.assertEquals(file.getDirectory().getPath(), dir.getPath());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	private void deleteDir(ScmWorkspace ws, String dirPath) {
		List<String> pathList = getSubPaths(dirPath);
		for (int i = pathList.size() - 1; i >= 0; i--) {
			try {
				ScmFactory.Directory.deleteInstance(ws, pathList.get(i));
			} catch (ScmException e) {
				if (e.getError() != ScmError.DIR_NOT_FOUND && e.getError() != ScmError.DIR_NOT_EMPTY) {
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