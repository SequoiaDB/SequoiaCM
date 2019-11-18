package com.sequoiacm.asynctask;

import java.io.File;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-490: 在主中心异步迁移单个文件
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、在主中心异步迁移单个文件； 2、检查执行结果正确性；
 */

public class AsyncTransfer_inMainSite490 extends TestScmBase {

	private boolean runSuccess = false;

	private final int fileSize = 200 * 1024;
	private ScmId fileId = null;
	private String fileName = "AsyncTransferOnMainSite490";
	private File localPath = null;
	private String filePath = null;

	private ScmSession sessionA = null;
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

			sessionA = TestScmTools.createSession(branceSite);
			wsA = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
			prepareFiles();
		} catch (Exception e) {
			if (sessionA != null) {
				sessionA.close();
			}
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		ScmSession sessionM = null;
		try {
			sessionM = TestScmTools.createSession(rootSite);
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionM);
			ScmFactory.File.asyncTransfer(ws, fileId);
			Assert.fail("asyncTransfer shouldn't succeed on main site!");
		} catch (ScmException e) {
			if (ScmError.OPERATION_UNSUPPORTED != e.getError()) {
				e.printStackTrace();
				throw e;
			}
		} finally {
			if (sessionM != null) {
				sessionM.close();
			}
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(wsA, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (BaseException | ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}

		}
	}

	private void prepareFiles() throws Exception {
		ScmFile scmfile = ScmFactory.File.createInstance(wsA);
		scmfile.setContent(filePath);
		scmfile.setFileName(fileName+"_"+UUID.randomUUID());
		fileId = scmfile.save();
	}

}