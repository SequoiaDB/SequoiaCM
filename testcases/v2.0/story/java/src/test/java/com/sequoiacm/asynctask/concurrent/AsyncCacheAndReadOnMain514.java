package com.sequoiacm.asynctask.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
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
 * @FileName SCM-514: 并发在分中心缓存文件、主中心读取文件
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、并发在分中心A异步缓存单个文件、在主中心读取文件； 2、检查执行返回结果； 3、后台异步缓存任务执行完成后检查缓存后的文件正确性；
 */

public class AsyncCacheAndReadOnMain514 extends TestScmBase {
	private boolean runSuccess = false;

	private int fileSize = 1024 * new Random().nextInt(1024 + 1);
	private final int fileNum = 30;
	private List<ScmId> fileIdList = new ArrayList<ScmId>();

	private File localPath = null;
	private String filePath = null;
	private String fileName = "AsyncCacheAndReadOnMain514";
	private ScmSession sessionM = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsM = null;
	private ScmWorkspace wsA = null;
	
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
			branceSite = ScmInfo.getBranchSite();
			ws_T = ScmInfo.getWs();

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
			ScmFileUtils.cleanFile(ws_T, cond);

			sessionM = TestScmTools.createSession(rootSite);
			sessionA = TestScmTools.createSession(branceSite);
			wsM = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);
			wsA = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
			prepareFiles();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		try {
			for (int i = 0; i < fileNum; ++i) {
				ScmFactory.File.asyncCache(wsA, fileIdList.get(i));
				readFile(wsM, fileIdList.get(i));
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

	private void prepareFiles() throws Exception {
		for (int i = 0; i < fileNum; ++i) {
			ScmFile scmfile = ScmFactory.File.createInstance(wsM);
			scmfile.setContent(filePath);
			scmfile.setFileName(fileName+"_"+UUID.randomUUID());
			fileIdList.add(scmfile.save());
		}
	}

	private void readFile(ScmWorkspace ws, ScmId fileId) throws Exception {
		OutputStream fos = null;
		ScmInputStream sis = null;
		try {
			ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			fos = new FileOutputStream(new File(downloadPath));
			sis = ScmFactory.File.createInputStream(scmfile);
			sis.read(fos);
		} finally {
			if (fos != null) {
				fos.close();
			}
			if (sis != null) {
				sis.close();
			}
		}
	}

	private void checkResult() {
		try {
			SiteWrapper[] expSiteList = { rootSite, branceSite };
			for (int i = 0; i < fileNum; i++) {
				ScmTaskUtils.waitAsyncTaskFinished(wsM, fileIdList.get(i), expSiteList.length);
			}
			ScmFileUtils.checkMetaAndData(ws_T,fileIdList, expSiteList, localPath, filePath);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}