package com.sequoiacm.querybuilder;

import java.util.ArrayList;
import java.util.List;
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
 * @Testcase: SCM-344:greaterThanEquals多个相同字段
 * @author huangxiaoni init
 * @date 2017.5.25
 */

public class GreaterThanEquals344 extends TestScmBase {
	private boolean runSuccess1 = false;
	private boolean runSuccess2 = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private ScmFile file = null;
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private int fileNum = 3;
	private static String fileName = "GreaterThanEquals344";
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

			readyScmFile();
			file = ScmFactory.File.getInstance(ws, fileIdList.get(1));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testQueryByExistCond() throws Exception {
		try {
			// build condition
			String key = ScmAttributeName.File.CREATE_TIME;
			long value = file.getCreateTime().getTime();

			BSONObject cond = ScmQueryBuilder.start(key).greaterThanEquals(111).put(key).greaterThanEquals(value)
					.put(ScmAttributeName.File.AUTHOR).is(file.getAuthor()).get();

			String expCond = "{ \"" + key + "\" : { \"$gte\" : " + value + "}" + " , \"author\" : \"" + author + "\"}";
			Assert.assertEquals(cond.toString().replaceAll("\\s*",""), expCond.replaceAll("\\s*",""));

			// count
			long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
			Assert.assertEquals(count, 2);

			runSuccess1 = true;
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testQueryByNotExistCond() throws Exception {
		try {
			// build condition
			String key = ScmAttributeName.File.CREATE_TIME;
			long value = file.getCreateTime().getTime();

			BSONObject cond = ScmQueryBuilder.start(key).greaterThanEquals(value).put("k2").greaterThanEquals(" ")
					.get();

			String expCond = "{ \"" + key + "\" : { \"$gte\" : " + value + "} , " + "\"k2\" : { \"$gte\" : \" \"}}";
			Assert.assertEquals(cond.toString().replaceAll("\\s*",""), expCond.replaceAll("\\s*",""));

			// count
			long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
			Assert.assertEquals(count, 0);

			runSuccess2 = true;
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if ((runSuccess1 && runSuccess2) || forceClear) {
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
				ScmFile scmfile = ScmFactory.File.createInstance(ws);
				scmfile.setFileName(fileName+"_"+UUID.randomUUID());
				scmfile.setAuthor(author);
				ScmId fileId = scmfile.save();
				fileIdList.add(fileId);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}
}