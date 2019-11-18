package com.sequoiacm.scmfile;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-127:指定ScopeType获取文件列表 ScopeType: SCOPE_ALL (SCM-126 covered)
 *            SCOPE_CURRENT SCOPE_HISTORY
 * @author huangxiaoni init
 * @date 2017.4.6
 */

public class ListInstanceByScope127 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private int fileSize = 1024 * 10;
	private int fileNum = 3;
	private String fileTag = "file127";
	private File localPath = null;
	private String filePath = null;
	private List<ScmId> fileIdList = new LinkedList<ScmId>();

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
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(fileTag).get();
			ScmFileUtils.cleanFile(wsp, cond);

			for (int i = 0; i < fileNum; i++) {
				ScmFile scmfile = ScmFactory.File.createInstance(ws);
				scmfile.setFileName(fileTag + i);
				scmfile.setAuthor(fileTag);
				scmfile.setTitle(fileTag);
				scmfile.setMimeType(fileTag);
				scmfile.setContent(filePath);
				ScmId fileId = scmfile.save();
				fileIdList.add(fileId);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testListInstanceByScopeCurrent() {
		ScmCursor<ScmFileBasicInfo> cursor = null;
		try {
			ScopeType scopeType = ScopeType.SCOPE_CURRENT;
			BSONObject condition = new BasicBSONObject(ScmAttributeName.File.AUTHOR, fileTag);
			// condition.setMimeType(MimeType.TXT);
			cursor = ScmFactory.File.listInstance(ws, scopeType, condition);

			int size = 0;
			ScmFileBasicInfo file;
			while (cursor.hasNext()) {
				file = cursor.getNext();
				// check results
				ScmId fileId = file.getFileId();
				checkFileAttributes(file, fileId);

				size++;
			}
			Assert.assertEquals(size, fileNum);
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			cursor.close();
		}
		runSuccess = true;
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	@Deprecated
	private void testListInstanceByScopeHistory() {
		ScmCursor<ScmFileBasicInfo> cursor = null;
		try {
			ScopeType scopeType = ScopeType.SCOPE_HISTORY;
			BSONObject condition = new BasicBSONObject(ScmAttributeName.File.FILE_ID, fileIdList.get(0).get());
			cursor = ScmFactory.File.listInstance(ws, scopeType, condition);
			int size = 0;
			while(cursor.hasNext()){
				cursor.getNext();
				size++;
			}
			Assert.assertEquals(size, 0);
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(ws, fileId, true);
				}
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}

	private void checkFileAttributes(ScmFileBasicInfo file, ScmId fileId) {
		try {
			Assert.assertNotNull(file.getFileId());
			Assert.assertEquals(file.getFileId().get(), fileId.get());
			Assert.assertEquals(file.getMinorVersion(), 0);
			Assert.assertEquals(file.getMajorVersion(), 1);
		} catch (BaseException e) {
			throw e;
		}
	}

}