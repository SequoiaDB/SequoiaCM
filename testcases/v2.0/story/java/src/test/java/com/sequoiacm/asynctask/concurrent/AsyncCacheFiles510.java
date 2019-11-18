package com.sequoiacm.asynctask.concurrent;

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
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-510: 并发缓存多个不同文件
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、并发在分中心A异步缓存单个文件（不同文件）； 2、检查执行返回结果； 3、后台异步缓存任务执行完成后检查缓存后的文件正确性；
 */

public class AsyncCacheFiles510 extends TestScmBase {

	private boolean runSuccess = false;

	private final int fileSize = 1024 * 100;
	private final int fileNum = 50;
	private List<ScmId> fileIdList = new ArrayList<ScmId>();

	private File localPath = null;
	private String filePath = null;
	private String fileName = "AsyncCacheFiles510";
	private ScmSession sessionM = null;
	private ScmWorkspace wsM = null;
	private ScmSession sessionA = null;
	
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

			rootSite = ScmInfo.getRootSite();
			branceSite= ScmInfo.getBranchSite();
			ws_T = ScmInfo.getWs();

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
			ScmFileUtils.cleanFile(ws_T, cond);

			sessionM = TestScmTools.createSession(rootSite);
			wsM = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);
			sessionA = TestScmTools.createSession(branceSite);
			prepareFiles(wsM);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(),sessionA);
			for (int i = 0; i < fileNum; ++i) {
				ScmFactory.File.asyncCache(ws, fileIdList.get(i));
			}
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
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(wsM, fileId, true);
				}
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (BaseException | ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
			if (sessionM != null) {
				sessionM.close();
			}
		}
	}

	private void prepareFiles(ScmWorkspace ws) throws Exception {
		for (int i = 0; i < fileNum; ++i) {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setContent(filePath);
			scmfile.setFileName(fileName+"_"+UUID.randomUUID());
			fileIdList.add(scmfile.save());
		}
	}

	private void checkResult() throws Exception {
		SiteWrapper[] expSiteList = { rootSite, branceSite };
		for (int i = 0; i < fileNum; i++) {
			ScmTaskUtils.waitAsyncTaskFinished(wsM, fileIdList.get(i), expSiteList.length);
		}
		ScmFileUtils.checkMetaAndData(ws_T,fileIdList, expSiteList, localPath, filePath);
	}
}