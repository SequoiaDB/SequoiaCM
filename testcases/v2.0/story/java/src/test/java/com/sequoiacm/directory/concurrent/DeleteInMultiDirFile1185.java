
package com.sequoiacm.directory.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;

import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-1185 :: 并发删除多级文件夹下不同级的子文件
 * @author fanyu
 * @Date:2018年5月3日
 * @version:1.0
 */
public class DeleteInMultiDirFile1185 extends TestScmBase {
	private boolean runSuccess;
	private ScmSession session;
	private ScmWorkspace ws;
	private SiteWrapper site;
	private WsWrapper wsp;
	private String dirBasePath = "/DeleteInMultiDirFile1185";
	private String fullPath1 = dirBasePath;
	private String fullPath2;
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private String author = "DeleteInMultiDirFile1185";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(wsp, cond);
			createDir(ws, fullPath1);
			fullPath2 = fullPath1;
			for (int i = 'a'; i < 'f'; i++) {
				fullPath2 = fullPath2 + "/1185_" + (char) i;
				deleteDir(ws, fullPath2);
				ScmDirectory dir = createDir(ws, fullPath2);
				createFile(ws, dir);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws ScmException {
		DeleteFile dThread = new DeleteFile();
		dThread.start(10);
		boolean dflag = dThread.isSuccess();
		Assert.assertEquals(dflag, true, dThread.getErrorMsg());
		for (ScmId fileId : fileIdList) {
			checkFile(fileId);
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess || TestScmBase.forceClear) {
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

	private class DeleteFile extends TestThreadBase {
		@Override
		public void exec() {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(site);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(ws, fileId, true);
				}
			} catch (ScmException e) {
				if (e.getError() != ScmError.FILE_NOT_FOUND) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}

	private void createFile(ScmWorkspace ws, ScmDirectory dir) {
		ScmFile file;
		try {
			file = ScmFactory.File.createInstance(ws);
			file.setFileName(author + "_" + UUID.randomUUID());
			file.setAuthor(author);
			file.setTitle(author);
			file.setDirectory(dir);
			ScmId fileId = file.save();
			fileIdList.add(fileId);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	private void checkFile(ScmId id) throws ScmException {
		BSONObject cond = null;
		try {
			cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).is(id.get()).get();
			long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
			Assert.assertEquals(count, 0);
		} catch (ScmException e) {
			ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_CURRENT, cond);
			System.out.println("fileId = " + cursor.getNext().getFileId());
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
		return ScmFactory.Directory.getInstance(ws, pathList.get(pathList.size() - 1));
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
