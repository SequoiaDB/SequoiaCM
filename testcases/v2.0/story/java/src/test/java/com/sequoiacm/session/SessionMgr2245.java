
package com.sequoiacm.session;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSessionMgr;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-2245 ::ScmSessionMgr获取session,做业务操作
 * @author fanyu
 * @Date:2018年9月21日
 * @version:1.0
 */
public class SessionMgr2245 extends TestScmBase {
	private boolean runSuccess = false;
	private File localPath = null;
	private String filePath = null;
	private int fileSize = 1024 * 1;
	private String name = "SessionMgr2245";
	private SiteWrapper site = null;
	private WsWrapper wsp = null;
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private ScmSessionMgr sessionMgr = null;
	private String key = "server.port";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);

			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(name).get();
			ScmFileUtils.cleanFile(wsp, cond);

			sessionMgr = createSessionMgr();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	private void testAuth() throws Exception {
		ScmSession session = null;
		try {
			session = sessionMgr.getSession(SessionType.AUTH_SESSION);
			// operation that require permissions
			write(session);
			// check results
			SiteWrapper[] expSites = { site };
			ScmFileUtils.checkMetaAndData(wsp, fileIdList.get(0), expSites, localPath, filePath);

			// operation that do not require permissions
			String info = getConfProp(session, key);
			Assert.assertNotNull(info);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
		}
		runSuccess = true;
	}

	@Test
	private void testNotAuth() {
		ScmSession session = null;
		try {
			session = sessionMgr.getSession(SessionType.NOT_AUTH_SESSION);
			// operation that require permissions
			write(session);
			Assert.fail("operation that require permissions");
		} catch (ScmException e) {
			if (e.getError() != ScmError.HTTP_FORBIDDEN) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}

		// operation that do not require permissions
		try {
			String info = getConfProp(session, key);
			Assert.assertNotNull(info);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		ScmSession session = null;
		try {
			session = sessionMgr.getSession(SessionType.AUTH_SESSION);
			if (runSuccess || TestScmBase.forceClear) {
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
				TestTools.LocalFile.removeFile(localPath);
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(ws, fileId, true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
			if (sessionMgr != null) {
				sessionMgr.close();
			}
		}
	}

	private ScmSessionMgr createSessionMgr() {
		List<String> urlList = new ArrayList<String>();
		for (String gateway : gateWayList) {
			urlList.add(gateway + "/" + site.getSiteServiceName());
		}
		ScmConfigOption scOpt;
		ScmSessionMgr sessionMgr = null;
		try {
			scOpt = new ScmConfigOption(urlList, TestScmBase.scmUserName, TestScmBase.scmPassword);
			sessionMgr = ScmFactory.Session.createSessionMgr(scOpt, 1000);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		return sessionMgr;
	}

	private ScmId write(ScmSession session) throws ScmException {
		ScmId fileId = null;
		// create file
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		ScmFile file = ScmFactory.File.createInstance(ws);
		file.setContent(filePath);
		file.setFileName(name + "_" + UUID.randomUUID());
		file.setAuthor(name);
		file.setTitle("sequoiacm");
		fileId = file.save();
		fileIdList.add(fileId);
		return fileId;
	}

	private String getConfProp(ScmSession session, String key) throws ScmException {
		String info = ScmSystem.Configuration.getConfProperty(session, key);
		return info;
	}
}
