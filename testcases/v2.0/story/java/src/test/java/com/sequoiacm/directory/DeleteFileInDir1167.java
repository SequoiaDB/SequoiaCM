/**
 * 
 */
package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
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
 * @Description:SCM-1167 :: 删除根文件夹/多级文件夹下的文件
 * @author fanyu
 * @Date:2018年4月25日
 * @version:1.0
 */
public class DeleteFileInDir1167 extends TestScmBase {
	private boolean runSuccess;
	private ScmSession session;
	private ScmWorkspace ws;
	private SiteWrapper site;
	private WsWrapper wsp;
	private String dirBasePath = "/DeleteFileInDir1167";
	private String fullPath1 = dirBasePath + "/1167_b/1167_c/1167_d/1167_e";
	private String author = "DeleteFileInDir1167";
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
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
			deleteDir(ws, fullPath1);
			dir = createDir(ws, fullPath1);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws ScmException {
		// file in multi dir
	    createFile(ws, dir);
		// delete file in multi dir
		ScmFactory.File.deleteInstance(ws, fileIdList.get(0), true);
		// check file_meta table
		BSONObject cond1 = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
		check(ws, cond1);
		// check file_rel table
		BSONObject cond2 = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).is(fileIdList.get(0).get()).get();
		check(ws, cond2);

		// file in root
	    createFile(ws, ScmFactory.Directory.getInstance(ws, "/"));
		// delete file in root dir
		ScmFactory.File.deleteInstance(ws, fileIdList.get(1), true);
		// check file_meta table
		BSONObject cond3 = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
		check(ws, cond3);
		// check file_rel table
		BSONObject cond4 = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).is(fileIdList.get(1).get()).get();
		check(ws, cond4);
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				deleteDir(ws, dir.getPath());
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

	private void createFile(ScmWorkspace ws, ScmDirectory dir) {
		ScmFile file;
		try {
			file = ScmFactory.File.createInstance(ws);
			file.setFileName(author + "_" + UUID.randomUUID());
			file.setAuthor(author);
			file.setDirectory(dir);
			ScmId fileId = file.save();
			fileIdList.add(fileId);
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
				dir = ScmFactory.Directory.getInstance(ws, path);
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

	private void check(ScmWorkspace ws, BSONObject cond) {
		try {
			long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
			ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_CURRENT, cond);
			while(cursor.hasNext()){
				System.out.println(cursor.getNext().toString());
			}
			Assert.assertEquals(count, 0);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}
