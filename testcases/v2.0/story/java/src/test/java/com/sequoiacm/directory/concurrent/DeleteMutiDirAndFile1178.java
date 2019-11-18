/**
 * 
 */
package com.sequoiacm.directory.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;

import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-1178:并发删除多级文件夹下不同级的子文件夹，子文件
 * @author fanyu
 * @Date:2018年5月2日
 * @version:1.0
 */
public class DeleteMutiDirAndFile1178 extends TestScmBase {
	private boolean runSuccess;
	private ScmSession session;
	private ScmWorkspace ws;
	private SiteWrapper site;
	private WsWrapper wsp;
	private String dirBasePath = "/DeleteMutiDirAndFile1178";
	private String fullPath1 = dirBasePath;
	private List<String> subPathList = new ArrayList<String>();
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private String author = "DeleteMutiDirAndFile1178";
	private ScmDirectory dir;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(wsp, cond);
			for (int i = 'a'; i < 'f'; i++) {
				deleteDir(ws, dirBasePath + "/1177_" + (char) i);
			}
			dir = createDir(ws, fullPath1);
			for (int i = 'a'; i < 'f'; i++) {
				String path = null;
				if (i % 2 == 0) {
					path = fullPath1 + "/1177_" + (char) i;
				} else {
					path = fullPath1 + "/1177_dir/1177_" + (char) i;
				}
				createDir(ws, path);
				subPathList.add(path);
				createFile(ws, dir);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws ScmException {
		DeleteSubDirAndFile dThread = new DeleteSubDirAndFile();
		dThread.start(5);
		boolean dflag = dThread.isSuccess();
		Assert.assertEquals(dflag, true, dThread.getErrorMsg());
		for (int i = 0; i < subPathList.size(); i++) {
			checkDir(subPathList.get(i));
			checkFile(fileIdList.get(i));
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				deleteDir(ws, fullPath1);
				deleteDir(ws, fullPath1 + "/1177_dir");
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

	private class DeleteSubDirAndFile extends TestThreadBase {
		@Override
		public void exec() throws ScmException {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(site);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
				for (int i = 0; i < subPathList.size(); i++) {
					try {
						ScmFactory.Directory.deleteInstance(ws, subPathList.get(i));
					} catch (ScmException e) {
						if (e.getError() != ScmError.DIR_NOT_FOUND) {
							e.printStackTrace();
							Assert.fail(e.getMessage());
						}
					}
					try {
						ScmFactory.File.deleteInstance(ws, fileIdList.get(i), true);
					} catch (ScmException e) {
						if (e.getError() != ScmError.FILE_NOT_FOUND) {
							e.printStackTrace();
							Assert.fail(e.getMessage());
						}
					}
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

	private void checkDir(String path) {
		try {
			ScmDirectory dir = ScmFactory.Directory.getInstance(ws, path);
			Assert.fail("expect fail but act success," + dir.toString());
		} catch (ScmException e) {
			if (e.getError() != ScmError.DIR_NOT_FOUND) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
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
