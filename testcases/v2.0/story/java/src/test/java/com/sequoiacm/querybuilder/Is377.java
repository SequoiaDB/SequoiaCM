package com.sequoiacm.querybuilder;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-377: is单个字段
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、countInstance带查询条件查询文件，对单个字段做is（给key添加指定value）匹配查询；
 * 2、检查ScmQueryBuilder结果正确性； 3、检查查询结果正确性；
 */

public class Is377 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private int fileNum = 3;
	private static String authorName = "Is377";

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(authorName).get();
			ScmFileUtils.cleanFile(wsp, cond);

			readyScmFile();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testQuery() throws Exception {
		try {
			// build condition
			ScmFile file = ScmFactory.File.getInstance(ws, fileIdList.get(1));
			String key1 = ScmAttributeName.File.AUTHOR;
			String val1 = authorName;
			String key2 = ScmAttributeName.File.FILE_NAME;
			String val2 = file.getFileName();
			BSONObject cond = ScmQueryBuilder.start(key1).is(val1).and(key2).is(val2).get();
			String expStr = "{ \"author\" : \""+ val1 +"\" , \"name\" : \""+ val2 +"\" }";
			Assert.assertEquals(cond.toString(), expStr);

			// count
			long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
			Assert.assertEquals(count, 1);

			runSuccess = true;
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.getInstance(ws, fileId).delete(true);
				}
			}
		} catch (BaseException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}

	private void readyScmFile() {
		try {
			for (int i = 0; i < fileNum; i++) {
				ScmFile file = ScmFactory.File.createInstance(ws);
				file.setFileName(authorName + "_" + i);
				file.setAuthor(authorName);
				ScmId fileId = file.save();
				fileIdList.add(fileId);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}
}