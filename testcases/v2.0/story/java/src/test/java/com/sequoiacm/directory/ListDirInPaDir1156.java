
package com.sequoiacm.directory;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
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
 * @Description: SCM-1156 :: 在当前文件夹下，根据条件检索文件夹
 * @author fanyu
 * @Date:2018年4月26日
 * @version:1.0
 */
public class ListDirInPaDir1156 extends TestScmBase{
	private boolean runSuccess1;
	private boolean runSuccess2;
	private ScmSession session;
	private ScmWorkspace ws;
	private SiteWrapper site;
	private WsWrapper wsp;
	private String dirBasePath = "/ListDirInPaDir1156";
	private String fullPath1 = dirBasePath;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			for (int i = 'a'; i < 'f'; i++) {
				deleteDir(ws, dirBasePath + "/1156_" + (char)i);
			}
			
			createDir(ws, fullPath1);
			for (int i = 'a'; i < 'f'; i++) {
				ScmFactory.Directory.createInstance(ws, dirBasePath + "/1156_" + (char)i);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testCondIsNull() {
		try {
			int i = 0;
			int expDirNum = 5;
			ScmDirectory pdir =  ScmFactory.Directory.getInstance(ws, fullPath1);
			ScmCursor<ScmDirectory> dirCursor = pdir.listDirectories(null);
			while(dirCursor.hasNext()){
				ScmDirectory dir = dirCursor.getNext();
				Assert.assertNotNull(dir.getName());
				i++;
			}
			Assert.assertTrue(i==expDirNum);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess1 = true;
	}
	
	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testCond() {
		String dirName = "1156_b";
		int expDirNum = 1;
		ScmDirectory dir = null;
		try {
			int i = 0;
			BSONObject cond =  ScmQueryBuilder.start(ScmAttributeName.Directory.USER)
					.is(TestScmBase.scmUserName)
					.and(ScmAttributeName.Directory.UPDATE_USER)
					.is(TestScmBase.scmUserName)
					.and(ScmAttributeName.Directory.NAME)
					.is(dirName)
					.and(ScmAttributeName.Directory.CREATE_TIME)
					.greaterThan(0)
					.and(ScmAttributeName.Directory.UPDATE_TIME)
					.greaterThan(0)
					.get();
			ScmDirectory pdir =  ScmFactory.Directory.getInstance(ws, fullPath1);
			ScmCursor<ScmDirectory> dirCursor = pdir.listDirectories(cond);
			while(dirCursor.hasNext()){
			    dir = dirCursor.getNext();
				Assert.assertNotNull(dir.getName());
				i++;
			}
			Assert.assertTrue(i==expDirNum);
		} catch (ScmException e) {
			System.out.println(dir.toString());
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess2 = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess1 || runSuccess2 || TestScmBase.forceClear) {
				for (int i = 'a'; i < 'f'; i++) {
					deleteDir(ws, dirBasePath + "/1156_" + (char)i);
				}
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

