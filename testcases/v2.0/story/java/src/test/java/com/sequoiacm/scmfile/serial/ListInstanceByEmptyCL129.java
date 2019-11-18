package com.sequoiacm.scmfile.serial;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-129:表中无文件记录，获取该文件列表
 * @author huangxiaoni init
 * @date 2017.4.6
 */

public class ListInstanceByEmptyCL129 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testListInstanceByWS() {
		ScmCursor<ScmFileBasicInfo> cursor = null;
		try {
			ScopeType scopeType = ScopeType.SCOPE_CURRENT;
			BSONObject condition = new BasicBSONObject("name", "test111111111111dfafdafdsafd");
			cursor = ScmFactory.File.listInstance(ws, scopeType, condition);

			int size = 0;
			while (cursor.hasNext()) {
				cursor.getNext();
				size++;
			}
			Assert.assertEquals(size, 0);
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			cursor.close();
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
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