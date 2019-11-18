
package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
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
 * @Description: SCM-1152 :: 文件移动到新父文件夹
 * @author fanyu
 * @Date:2018年4月25日
 * @version:1.0
 */
public class MoveFileToNewPa1152 extends TestScmBase {
	private boolean runSuccess;
	private ScmSession session;
	private ScmWorkspace ws;
	private SiteWrapper site;
	private WsWrapper wsp;
	private String dirBasePath = "/MoveFileToNewPa1152";
	private String fullPath1 = dirBasePath + "/1152_b/MoveFileToNewPa1152/1152_d/1152_e";
	private String author = "MoveFileToNewPa1152";
	private ScmId fileId;
	private ScmDirectory dir;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(wsp,cond);
			deleteDir(ws, fullPath1);
			dir = createDir(ws, fullPath1);
			createFile(ws, dir);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
    
	//bug:255
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() {
		try {
			// new parent directory does not exits same ele;
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			String newParentPath = dirBasePath + "/1152_b/MoveFileToNewPa1152/1152_d";
			file.setDirectory(ScmFactory.Directory.getInstance(ws, newParentPath));
			Assert.assertEquals(file.getDirectory().getPath(), newParentPath + "/");
			// check new dir
            ScmFile file1 = ScmFactory.File.getInstanceByPath(ws, newParentPath + "/" + author);
			Assert.assertEquals(file1.getDirectory().getPath(), newParentPath + "/");
			// check old dir
			try {
                ScmFactory.File.getInstanceByPath(ws, fullPath1 + "/" + author, 1, 0);
				Assert.fail("exp fail but act success,fileId = " + fileId.get());
			} catch (ScmException e1) {
				if (e1.getError() != ScmError.FILE_NOT_FOUND) {
					e1.printStackTrace();
					Assert.fail(e1.getMessage());
				}
			}
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		try {
			// new parent directory  exits same ele;
			ScmFile file = ScmFactory.File.getInstance(ws, fileId);
			String newParentPath = dirBasePath + "/1152_b";
			file.setDirectory(ScmFactory.Directory.getInstance(ws, newParentPath));
			Assert.fail("it is not successful when new parent's path has same ele");
		} catch (ScmException e) {
			if (e.getError() != ScmError.DIR_EXIST) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
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
			file.setFileName(author);
			file.setAuthor(author);
			file.setDirectory(dir);
			fileId = file.save();
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
		List<String> pathList = new ArrayList<>();
		for (int i = 1; i < arry.length; i++) {
			ele = ele + arry[i];
			pathList.add(ele);
			ele = ele + "/";
		}
		return pathList;
	}
}
