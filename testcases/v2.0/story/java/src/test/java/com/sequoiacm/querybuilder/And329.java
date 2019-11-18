package com.sequoiacm.querybuilder;

import java.util.UUID;

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
 * @Testcase: SCM-329:and匹配不存在的字段
 * @author huangxiaoni init
 * @date 2017.5.25
 */

public class And329 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private ScmId fileId = null;
	private String fileName = "and329";
    private String author = fileName;
	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(wsp, cond);
			ScmFile file = ScmFactory.File.createInstance(ws);
			file.setFileName(fileName+"_"+UUID.randomUUID());
			file.setAuthor(author);
			fileId = file.save();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testQuery() throws Exception {
		try {
			// build condition
			BSONObject obj = ScmQueryBuilder.start("notExist").is(" ").get();

			BSONObject cond = ScmQueryBuilder.start().and(obj).get();

			Assert.assertEquals(cond.toString().replaceAll("\\s*",""), ("{ \"$and\" : [ { \"notExist\" : \" \"}]}").replaceAll("\\s*",""));

			// count
			long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
			Assert.assertEquals(count, 0);

			runSuccess = true;
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
			}
		} catch (BaseException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}
}