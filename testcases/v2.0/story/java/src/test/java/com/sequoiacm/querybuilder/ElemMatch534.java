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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-534: eleMatch单个字段
 * @Author fanyu
 * @Date 2017-06-28
 * @Version 1.00
 */

/*
 * 1、countInstance带查询条件查询文件，eleMatch匹配查询单个字段和值； 2、检查ScmQueryBuilder结果正确性；
 * 3、检查查询结果正确性
 */

public class ElemMatch534 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private String fileName = "ElemMatch534";
	private String author = fileName;
	private BSONObject cond = null;

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
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testEleMatch() throws Exception {
		try {
			String key = ScmAttributeName.File.SITE_LIST;
			String objKey = ScmAttributeName.File.SITE_ID;
			BSONObject obj = ScmQueryBuilder.start(objKey).is(site.getSiteId()).get();
			cond = ScmQueryBuilder.start(key).elemMatch(obj).and(ScmAttributeName.File.AUTHOR).is(author).get();
			Assert.assertEquals(cond.toString().replaceAll("\\s*",""), ("{ \"" + key + "\"" + " : { \"$elemMatch\" : " + obj.toString()
					+ "} , \"author\" : " + "\"" + author + "\"}").replaceAll("\\s*",""));
			// count
			long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);			
			Assert.assertEquals(count, 1);
		} catch (Exception e) {
			Assert.fail(e.getMessage()+" cond = "+ cond);
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || forceClear) {
				ScmFileUtils.cleanFile(wsp, cond);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}

	private void readyScmFile() {
		try {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setFileName(fileName+"_"+UUID.randomUUID());
			scmfile.setAuthor(author);
			scmfile.save();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
