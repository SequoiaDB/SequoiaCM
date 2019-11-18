package com.sequoiacm.scmfile;

import org.bson.BSONObject;
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

/**
 * @FileName SCM-405: 当文件元数据属性为特殊值时查询
 * @Author linsuqiang
 * @Date 2017-05-24
 * @Version 1.00
 */

/*
 * 1、创建文件，设置文件属性为特殊值， string类型属性覆盖超长字符串（如2048）和空字符串， int类型属性覆盖int的最大最小值，
 * Object类型覆盖null（如果允许为null）； 2、用listInstance精确查询该元数据；
 */

public class ListInstance_metaBoundVal405 extends TestScmBase {
	private boolean runSuccess = false;

	private static SiteWrapper site = null;
	private static WsWrapper wsp = null;
	private static ScmSession session = null;
	private ScmWorkspace ws = null;
	private ScmId fileId = null;
	private String longStr = TestTools.getRandomString(950);

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
			wsp = ScmInfo.getWs();
			session = TestScmTools.createSession(site);
			ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(longStr).get();
			ScmFileUtils.cleanFile(wsp, cond);
			createFileAndSetAttr();
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() {
		try {
			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(longStr).get();
			ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(ws, ScopeType.SCOPE_CURRENT, cond);
			cursor.getNext();
			cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			if (runSuccess || TestScmBase.forceClear) {
				ScmFactory.File.deleteInstance(ws, fileId, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}

		}
	}

	private void createFileAndSetAttr() {
		try {
			ScmFile file = ScmFactory.File.createInstance(ws);
			//file.setAuthor(longStr);
			file.setFileName(longStr);
			//file.setPropertyType(null);
			file.setTitle("");
			fileId = file.save();
		} catch (ScmException e) {
			Assert.fail(e.getMessage());
		}
	}

}
