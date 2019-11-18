package com.sequoiacm.autocreatelobcs.serial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.base.Sequoiadb;

/**
 * @FileName SCM-885:元数据子表不存在，并发写文件
 * @Author huangxiaoni
 * @Date 2017-10-10
 */

public class AutoCreateMetaSubCL885 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static ScmSession session = null;
	private String wsName = "test_885_0";
	private AtomicInteger atom = new AtomicInteger(0);

	private final String fileName = "AutoCreateMetaSubCL885";
	private List<ScmId> fileIdList = Collections.synchronizedList(new ArrayList<ScmId>());

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException, Exception {
		try {
			site = ScmInfo.getSite();
			session = TestScmTools.createSession(site);

			try {
				ScmFactory.Workspace.deleteWorkspace(session, wsName, true);
			} catch (ScmException e) {
				if(ScmError.WORKSPACE_NOT_EXIST != e.getError()){
					throw e;
				}
			}
			ScmWorkspaceUtil.createWS(session, wsName, ScmInfo.getSiteNum());
			ScmWorkspaceUtil.wsSetPriority(session, wsName);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = {"twoSite","fourSite" })
	private void test() {
		try {
			WriteFile writeFile = new WriteFile();
			writeFile.start(10);
			Assert.assertTrue(writeFile.isSuccess(), writeFile.getErrorMsg());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	private class WriteFile extends TestThreadBase {
		@Override
		public void exec() throws Exception {
			Calendar cal = Calendar.getInstance();
			ScmSession ss = null;
			ScmId fileId = null;
			try {
				ss = TestScmTools.createSession(site);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);
                cal.set(Calendar.YEAR, cal.get(Calendar.YEAR)-atom.getAndIncrement());
				ScmFile file = ScmFactory.File.createInstance(ws);
				file.setFileName(fileName+"_"+UUID.randomUUID());
				file.setCreateTime(cal.getTime());
				fileId = file.save();
				fileIdList.add(fileId);
			} catch (Exception e) {
				System.out.println("error, fileId=" + fileId.get());
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (null != ss) {
					ss.close();
				}
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, session);
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(ws, fileId, true);
				}
			}
			this.clearWS(wsName, session);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}
	
	private void clearWS(String wsName, ScmSession session) throws ScmException {
		try {
			ScmFactory.Workspace.deleteWorkspace(session, wsName, true);
		} catch (ScmException e) {
			if(ScmError.WORKSPACE_NOT_EXIST != e.getError()){
				throw e;
			}
		}
		
		// check ws's metaCS
		Sequoiadb sdb = null;
		try {			
			sdb = new Sequoiadb(TestScmBase.mainSdbUrl, TestScmBase.sdbUserName, TestScmBase.sdbPassword);
			// check workspace's cs
			String metaCSName = wsName+"_META";
			boolean isExist = sdb.isCollectionSpaceExist(metaCSName);
			if (true == isExist) {
				sdb.dropCollectionSpace(metaCSName);
			}
			Assert.assertFalse(isExist);
		} finally {
			if (sdb != null) {
				sdb.close();
			}
		}
	}
}