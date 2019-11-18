package com.sequoiacm.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.client.core.*;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-424: 游标直接调用getNext获取任务信息
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、获取任务列表，查询条件匹配查询多条记录； 2、调用getNext()获取任务； 3、检查执行结果正确性；
 */

public class Transfer_cursorGetNext424 extends TestScmBase {

	private boolean runSuccess = false;

	private final int fileSize = 200 * 1024;
	private final int fileNum = 10;
	private final String authorName = "file424";
	private List<ScmId> fileIdList = new ArrayList<ScmId>();

	private File localPath = null;
	private String filePath = null;
	private BSONObject cond = null;

	private ScmSession sessionA = null;
	private ScmWorkspace ws = null;
	private ScmId taskId = null;
    
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
            
			branceSite = ScmInfo.getBranchSite();
		    ws_T = ScmInfo.getWs();
			
			sessionA = TestScmTools.createSession( branceSite);
			ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
			
			cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(ws_T, cond);
			
			prepareFiles();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
			if (sessionA != null) {
				sessionA.close();
			}
		}
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void test() throws Exception {
		ScmCursor<ScmTaskBasicInfo> cursor = null;
		try {
			taskId = transferFile(sessionA);

			ScmTaskUtils.waitTaskFinish(sessionA, taskId);

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.Task.ID).is(taskId.get()).get();
			cursor = ScmSystem.Task.listTask(sessionA, cond);
			ScmTaskBasicInfo info = cursor.getNext();
			Assert.assertEquals(info.getId().get(), taskId.get());
			Assert.assertEquals(info.getRunningFlag(), CommonDefine.TaskRunningFlag.SCM_TASK_FINISH); // 3:
																										// finish
			Assert.assertEquals(info.getType(), CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE); // 1:
																								// transfer
			Assert.assertEquals(info.getWorkspaceName(), ws.getName());
			Assert.assertNull(cursor.getNext());

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage() + " task INFO "+cursor.toString());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				for (int i = 0; i < fileNum; ++i) {
					ScmFactory.File.getInstance(ws, fileIdList.get(i)).delete(true);
				}
				TestTools.LocalFile.removeFile(localPath);
				TestSdbTools.Task.deleteMeta(taskId);
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

		for (int i = 0; i < fileNum; ++i) {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setFileName(authorName+"_"+UUID.randomUUID());
			scmfile.setAuthor(authorName);
			scmfile.setContent(filePath);
			fileIdList.add(scmfile.save());
		}
	}

	private ScmId transferFile(ScmSession session) throws ScmException {
		ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(ws_T.getName(), sessionA);
		BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
		return ScmSystem.Task.startTransferTask(ws, condition);
	}
}