
package com.sequoiacm.net.task;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.text.ParseException;
import java.util.*;

/**
 * @Description: SCM-2069 :: 重命名文件夹，文件夹下文件执行清理操作
 * @author fanyu
 * @Date:2018年7月11日
 * @version:1.0
 */
public class MoveDirThenTransferFile2070 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper site1 = null;
	private SiteWrapper site2 = null;
	private WsWrapper wsp = null;
	private ScmSession session1 = null;
	private ScmSession session2 = null;
	private ScmWorkspace ws1 = null;

	private String name = "MoveDirThenTransferFile2070";
	private int fileSize = 1024;
	private int fileNum = 50;
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private String dirPath = "/2070_A/2070_B/2070_A/2070_D/";
	private String newDirPath = "/2070_A/2070_D/";
	private ScmDirectory dir = null;

	private ScmId taskId = null;

	private File localPath = null;
	private String filePath = null;
	private Calendar calendar = Calendar.getInstance();

	@BeforeClass
	private void setUp() throws Exception {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";

		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);

		wsp = ScmInfo.getWs();
		List<SiteWrapper> siteList = ScmNetUtils.getCleanSites(wsp);
		site1 = siteList.get(0);
		site2 = siteList.get(1);

		session1 = TestScmTools.createSession(site1);
		session2 = TestScmTools.createSession(site2);
		ws1 = ScmFactory.Workspace.getWorkspace(wsp.getName(), session1);

		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
		ScmFileUtils.cleanFile(wsp, cond);

		BSONObject cond1 = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name + "_2").get();
		ScmFileUtils.cleanFile(wsp, cond1);

		deleteDir(ws1, dirPath);
		deleteDir(ws1, newDirPath);
		dir = createDir(ws1, dirPath);
		calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR)-3);
		prepareFile(dir);
	}

	@Test(groups = { "fourSite" })
	private void test() throws Exception {
		// rename dir
		ScmDirectory srcdir = ScmFactory.Directory.getInstance(ws1, dirPath);
		ScmDirectory destdir = ScmFactory.Directory.getInstance(ws1, "/2070_A");
		srcdir.move(destdir);

		// create file in /2070_A/2070_D
		prepareFile(srcdir);

		// clean
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name)
				.and(ScmAttributeName.File.DIRECTORY_ID).is(srcdir.getId()).get();
		System.out.println("cond = " + cond.toString());
		taskId = ScmSystem.Task.startTransferTask(ws1, cond, ScopeType.SCOPE_CURRENT, site2.getSiteName());
		waitTaskStop();

		// check meta data
		// transfer
		checkResult(fileIdList.subList(0, fileNum / 2), true);
		checkResult(fileIdList.subList(fileNum, fileNum + fileNum / 2), true);
		// untransfer
		checkResult(fileIdList.subList(fileNum / 2, fileNum), false);
		checkResult(fileIdList.subList(fileNum + fileNum / 2, 2 * fileNum), false);
		runSuccess = true;
	}

	@AfterClass
	private void tearDown() {
		try {
			if (!runSuccess || TestScmBase.forceClear) {
				if (fileIdList != null) {
					for (ScmId fileId : fileIdList) {
						System.out.println("fileId = " + fileId.get());
					}
				}
			}
			for (ScmId fileId : fileIdList) {
				ScmFactory.File.deleteInstance(ws1, fileId, true);
			}
			deleteDir(ws1, dirPath);
			deleteDir(ws1, newDirPath);
			TestSdbTools.Task.deleteMeta(taskId);
			TestTools.LocalFile.removeFile(localPath);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (session1 != null) {
				session1.close();
			}
			if (session2 != null) {
				session2.close();
			}
		}
	}

	private void checkResult(List<ScmId> fileIdList, boolean isTransfered) throws Exception {
		if (!isTransfered) {
			SiteWrapper[] expSiteList = { site1 };
			ScmFileUtils.checkMetaAndData(wsp, fileIdList, (SiteWrapper[]) expSiteList, localPath, filePath);
		} else {
			SiteWrapper[] expSiteList = { site1, site2 };
			ScmFileUtils.checkMetaAndData(wsp, fileIdList, expSiteList, localPath, filePath);
		}
	}

	private void waitTaskStop() throws ScmException {
		Date stopTime = null;
		while (stopTime == null) {
			stopTime = ScmSystem.Task.getTask(session1, taskId).getStopTime();
		}
	}

	private void prepareFile(ScmDirectory dir) throws Exception {
		for (int i = 0; i < fileNum; i++) {
			ScmId fileId = null;
			if (i < fileNum / 2) {
				fileId = createFile(filePath, name, dir);
			} else {
				fileId = createFile(filePath, name + "_2", dir);
			}
			fileIdList.add(fileId);
		}
	}

	private ScmId createFile(String filePath, String name, ScmDirectory dir) throws ScmException, ParseException {
		ScmFile file = ScmFactory.File.createInstance(ws1);
		file.setContent(filePath);
		file.setFileName(name + "_" + UUID.randomUUID());
		file.setAuthor(name);
		file.setCreateTime(calendar.getTime());
		file.setDirectory(dir);
		ScmId fileId = file.save();
		return fileId;
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
