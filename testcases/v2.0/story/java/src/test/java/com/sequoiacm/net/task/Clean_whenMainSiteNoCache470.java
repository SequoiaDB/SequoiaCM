package com.sequoiacm.net.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-470: 分中心存在缓存，主中心不存在缓存，在分中心清理文件
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、在分中心A开始清理任务，清理条件匹配清理主中心不存在缓存的文件； 2、检查执行结果正确性；
 */

public class Clean_whenMainSiteNoCache470 extends TestScmBase {

	private boolean runSuccess = false;

	private final int fileSize = 200 * 1024;
	private final int fileNum = 100;
	private final String authorName = "CleanFileWhenMainSiteNoCache470";
	private List<ScmId> fileIdList = new ArrayList<ScmId>();

	private File localPath = null;
	private String filePath = null;

	private ScmSession sessionM = null;
	private ScmSession sessionA = null;

	private ScmId taskId = null;
	
	private SiteWrapper rootSite = null;
	private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {

		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";

		try {
			// ready file
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
			
			ws_T = ScmInfo.getWs();
			List<SiteWrapper> siteList = ScmNetUtils.getCleanSites(ws_T);
			rootSite = siteList.get(1);
			branceSite= siteList.get(0);
			
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(ws_T,cond);

			sessionA = TestScmTools.createSession(branceSite);
			sessionM = TestScmTools.createSession(rootSite);
			prepareFiles(sessionA);
		} catch (Exception e) {
			if (sessionA != null) {
				sessionA.close();
			}
			if (sessionM != null) {
				sessionM.close();
			}
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			taskId = cleanAllFile(sessionA);
			ScmTaskUtils.waitTaskFinish(sessionA, taskId);
			checkResult();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);
				for (int i = 0; i < fileNum; ++i) {
					ScmFactory.File.deleteInstance(ws, fileIdList.get(i), true);
				}
				TestSdbTools.Task.deleteMeta(taskId);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (BaseException | ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionM != null) {
				sessionM.close();
			}
			if (sessionA != null) {
				sessionA.close();
			}

		}
	}

	private void prepareFiles(ScmSession session) throws Exception {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
		for (int i = 0; i < fileNum; ++i) {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setFileName(authorName+"_"+UUID.randomUUID());
			scmfile.setAuthor(authorName);
			scmfile.setContent(filePath);
			fileIdList.add(scmfile.save());
		}
	}

	private ScmId cleanAllFile(ScmSession session) throws ScmException {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), session);
		BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		return ScmSystem.Task.startCleanTask(ws, condition);
	}

	private void checkResult() {
		try {
			SiteWrapper[] expSiteList = { branceSite };
			ScmFileUtils.checkMetaAndData(ws_T,fileIdList, expSiteList, localPath, filePath);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}