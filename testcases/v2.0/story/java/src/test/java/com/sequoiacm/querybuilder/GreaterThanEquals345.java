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
 * @Testcase: SCM-345:greaterThanEquals多个不同字段，覆盖所有文件属性
 * @author huangxiaoni init
 * @date 2017.5.25
 */

public class GreaterThanEquals345 extends TestScmBase {
	private boolean runSuccess = false;
	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;

	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private int fileNum = 3;
	private static String authorName = "GreaterThanEquals345";

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
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testQuery() throws Exception {
		try {
			// build condition
			BSONObject cond = null;
			Object[][] kvs = this.kvsArr();
			ScmQueryBuilder builder = null;
			String bsStr = "{ \"";
			for (Object[] kv : kvs) {
				String key = (String) kv[0];
				Object value = kv[1];
				String subStr = null;
				if (kv[1] instanceof String) {
					subStr = key + "\" : { \"$gte\" : \"" + value + "\"}";
				} else {
					subStr = key + "\" : { \"$gte\" : " + value + "}";
				}
				if (null == builder) {
					builder = ScmQueryBuilder.start(key).greaterThanEquals(value);
					bsStr = bsStr + subStr;
				} else {
					builder.put(key).greaterThanEquals(value);
					bsStr = bsStr + " , \"" + subStr;
				}
			}
			cond = builder.put(ScmAttributeName.File.AUTHOR).is(authorName).get();
			bsStr = bsStr + " , \"author\" : \"" + authorName + "\"}";
			// System.out.println(cond.toString());
			Assert.assertEquals(cond.toString().replaceAll("\\s*",""), bsStr.replaceAll("\\s*",""));

			// count
			long count = ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, cond);
			Assert.assertEquals(count, 2);

			runSuccess = true;
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || forceClear) {
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(ws, fileId, true);
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
				String str = "345_" + i;
				ScmFile scmfile = ScmFactory.File.createInstance(ws);
				scmfile.setFileName(str);
				scmfile.setAuthor(authorName);
				scmfile.setTitle(str);
				scmfile.setMimeType(str);
				ScmId fileId = scmfile.save();
				fileIdList.add(fileId);
			}
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

	private Object[][] kvsArr() throws ScmException {
		ScmFile file = ScmFactory.File.getInstance(ws, fileIdList.get(1));
		return new Object[][] { new Object[] { ScmAttributeName.File.FILE_ID, "" },
				new Object[] { ScmAttributeName.File.FILE_NAME, file.getFileName() },
				new Object[] { ScmAttributeName.File.TITLE, file.getTitle() },
				new Object[] { ScmAttributeName.File.MIME_TYPE, file.getMimeType() },
				new Object[] { ScmAttributeName.File.SIZE, -1 }, // file.getSize()
				new Object[] { ScmAttributeName.File.MAJOR_VERSION, -1 },
				new Object[] { ScmAttributeName.File.MINOR_VERSION, -1 },
				new Object[] { ScmAttributeName.File.USER, "1" },
				new Object[] { ScmAttributeName.File.CREATE_TIME, file.getCreateTime().getTime() },
				new Object[] { ScmAttributeName.File.UPDATE_USER, "1" },
				new Object[] { ScmAttributeName.File.UPDATE_TIME, file.getUpdateTime().getTime() } };
	}

}