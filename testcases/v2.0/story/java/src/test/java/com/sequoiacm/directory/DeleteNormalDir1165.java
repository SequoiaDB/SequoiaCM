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
 * @Description:SCM-1165 :: 删除普通文件夹 
 * @author fanyu
 * @Date:2018年4月25日
 * @version:1.0
 */
public class DeleteNormalDir1165 extends TestScmBase{
	private boolean runSuccess;
	private ScmSession session;
	private ScmWorkspace ws;
	private SiteWrapper site;
	private WsWrapper wsp;
	private String dirBasePath = "/DeleteNormalDir1165";
	private String fullPath1 = dirBasePath + "/1165_b/1165_c/1165_d/1165_e";
	private String author = "DeleteNormalDir1165";
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
			ScmFileUtils.cleanFile(wsp, cond);
			//ScmFactory.Directory.deleteInstance(ws, fullPath1);
			deleteDir(ws, fullPath1);
			dir = createDir(ws, fullPath1);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() throws ScmException {
		String dirPath = dirBasePath + "/1165_a";
		try {
			ScmFactory.Directory.createInstance(ws, dirPath);
			ScmFactory.Directory.deleteInstance(ws, dirPath);
			checkDir(ws,dirPath);
			ScmFactory.Directory.createInstance(ws, dirPath).delete();
			checkDir(ws,dirPath);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		
		//dir is not not empty
		try {
			ScmDirectory dir = ScmFactory.Directory.getInstance(ws, dirBasePath + "/1165_b/1165_c");
			dir.delete();
			Assert.fail("expect dir should not be deleted but actually it was deleted");
		} catch (ScmException e) {
			if (e.getError() != ScmError.DIR_NOT_EMPTY) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		Assert.assertEquals(ScmFactory.Directory.getInstance(ws, dirBasePath + "/1165_b/1165_c")
				.getPath(), dirBasePath + "/1165_b/1165_c/");
		
		//dir is not not empty
		try {
			createFile(ws, dir);
			ScmFactory.Directory.deleteInstance(ws, dirBasePath + "/1165_b/1165_c");
			Assert.fail("expect dir should not be deleted but actually it was deleted");
		} catch (ScmException e) {
			if (e.getError() != ScmError.DIR_NOT_EMPTY) {
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
			file.setFileName(author + "_" + UUID.randomUUID());
			file.setAuthor(author);
			file.setDirectory(dir);
			fileId = file.save();
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	private void checkDir(ScmWorkspace ws, String dirPath){
		try{
			ScmFactory.Directory.getInstance(ws, dirPath);
			Assert.fail("expect dir does not exist but actually it existed");
		}catch(ScmException e){
			if(e.getError() != ScmError.DIR_NOT_FOUND){
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
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
}

