
package com.sequoiacm.monitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmFlow;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description: SCM-2212:多个工作区对应不同的站点,获取工作区域的上传下载流量
 * @author fanyu
 * @Date:2018年9月11日
 * @version:1.0
 */
public class WsFlow2212_1 extends TestScmBase {
	private SiteWrapper site = null;
	private WsWrapper wsp = null;
	private ScmSession session = null;
	private ScmWorkspace ws = null;
	private String name = "WsFlow2212_1";
	private File localPath = null;
	private String filePath = null;
	private int fileSize = 1;
	private String uploadKey = "uploadFlow";
	private String downloadKey = "downloadFlow";
	private int threadNum = 50;
	private List<ScmId> fileIdList = new CopyOnWriteArrayList<ScmId>();
	private ScmId fileId1 = null;
	private String wsName = "ws2212_1";
	private boolean runSuccess = false;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws Exception {
		site = ScmInfo.getRootSite();
		wsp = ScmInfo.getWs();
		try {
			localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
			filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			ScmId fileId = ScmFileUtils.create(ws, name + "_" + UUID.randomUUID(), filePath);
			fileIdList.add(fileId);

			ScmWorkspaceUtil.deleteWs(wsName, session);
			ScmWorkspaceUtil.createWS(session, wsName, ScmInfo.getSiteNum());
			ScmWorkspaceUtil.wsSetPriority(session, wsName);
			ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace(wsName, session);
			fileId1 = ScmFileUtils.create(ws1, name + "_" + UUID.randomUUID(), filePath);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "fourSite" })
	private void testList() throws Exception {
		BasicBSONObject ws_flow_before = getFlowByWsName(wsp.getName());
		UploadFile up = new UploadFile();
		DownloadFile down = new DownloadFile(wsp.getName(), fileIdList.get(0));
		DownloadFile down1 = new DownloadFile(wsName, fileId1);

		up.start(threadNum);
		down.start(threadNum);
		down1.start(threadNum);

		boolean uflag = up.isSuccess();
		boolean dflag = down.isSuccess();
		boolean d1flag = down1.isSuccess();
		Assert.assertEquals(uflag, true, up.getErrorMsg());
		Assert.assertEquals(dflag, true, down.getErrorMsg());
		Assert.assertEquals(d1flag, true, down1.getErrorMsg());

		// check
		checkResult(ws_flow_before);
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		ScmWorkspaceUtil.deleteWs(wsName, session);
		if (runSuccess || TestScmBase.forceClear) {
			for (ScmId fileId : fileIdList) {
				try {
					ScmFactory.File.deleteInstance(ws, fileId, true);
				} catch (Exception e) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}
		}
		if (session != null) {
			session.close();
		}
	}
	
	private void checkResult(BasicBSONObject ws_flow_before){
		//check old ws
		BasicBSONObject ws_flow_after = getFlowByWsName(wsp.getName());
		if (ws_flow_before != null) {
			Assert.assertEquals(ws_flow_after.getLong(uploadKey) - ws_flow_before.getLong(uploadKey),
					fileSize * threadNum);
			Assert.assertEquals(ws_flow_after.getLong(downloadKey) - ws_flow_before.getLong(downloadKey),
					fileSize * threadNum);
		} else {
			Assert.assertEquals(ws_flow_after.getLong(uploadKey), fileSize * threadNum);
			Assert.assertEquals(ws_flow_after.getLong(downloadKey), fileSize * threadNum);
		}
		
        //check new ws
		BasicBSONObject ws2213_flow = getFlowByWsName(wsName);
		Assert.assertEquals(ws2213_flow.getLong(uploadKey), fileSize);
		Assert.assertEquals(ws2213_flow.getLong(downloadKey), fileSize * threadNum);
	}

	private BasicBSONObject getFlowByWsName(String wsName) {
		ScmCursor<ScmFlow> info = null;
		BasicBSONObject obj = null;
		try {
			info = ScmSystem.Monitor.showFlow(session);
			while (info.hasNext()) {
				ScmFlow flow = info.getNext();
				if (flow.getWorkspaceName().equals(wsName)) {
					obj = new BasicBSONObject();
					obj.put(uploadKey, flow.getUploadFlow());
					obj.put(downloadKey, flow.getDownloadFlow());
				}
			}
		} catch (ScmException e) {
			e.printStackTrace();
		} finally {
			if (info != null) {
				info.close();
			}
		}
		return obj;
	}

	private void downloadFile(ScmWorkspace ws, ScmId fileId) throws Exception {
		ScmFile file;
		OutputStream fileOutputStream = null;
		try {
			file = ScmFactory.File.getInstance(ws, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			fileOutputStream = new FileOutputStream(new File(downloadPath));
			file.getContent(fileOutputStream);
			fileOutputStream.close();
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (fileOutputStream != null) {
				fileOutputStream.close();
			}
		}
	}

	private class UploadFile extends TestThreadBase {
		@Override
		public void exec() {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(site);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
				ScmId fileId = ScmFileUtils.create(ws, name + "_" + UUID.randomUUID(), filePath);
				fileIdList.add(fileId);
			} catch (ScmException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}

	private class DownloadFile extends TestThreadBase {
		private String wsName = null;
		private ScmId fileId = null;

		public DownloadFile(String wsName, ScmId fileId) {
			this.wsName = wsName;
			this.fileId = fileId;
		}

		@Override
		public void exec() throws Exception {
			ScmSession session = null;
			try {
				session = TestScmTools.createSession(site);
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, session);
				downloadFile(ws, fileId);
			} catch (ScmException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}
}
