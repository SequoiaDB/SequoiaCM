package com.sequoiacm.session.seria;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-530: User表密码非md5值
 * @Author linsuqiang
 * @Date 2017-06-25
 * @Version 1.00
 */

/*
 * 1、SCMSYSTEM.USER表用户存在、密码非md5值（如test123）； 2、登入SCM，检查登入结果；
 */

public class LoginWhenPasswdNotMd5_530 extends TestScmBase {
	private static SiteWrapper site = null;
	private Sequoiadb sdb = null;
	private DBCollection userCL = null;
	private String oldPasswd = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();

			sdb = new Sequoiadb(TestScmBase.mainSdbUrl, TestScmBase.sdbUserName, TestScmBase.sdbPassword);
			userCL = sdb.getCollectionSpace(TestSdbTools.SCM_CS).getCollection(TestSdbTools.SCM_CL_USER);

			saveAndChangePasswd(userCL, TestScmBase.scmUserName, "test123");
		} catch (BaseException e) {
			e.printStackTrace();
			if (sdb != null) {
				sdb.close();
			}
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void test() {
		try {
			ScmConfigOption scOpt = new ScmConfigOption(TestScmBase.gateWayList.get(0)+"/"+ site, TestScmBase.scmUserName,
					TestScmBase.scmPassword);
			ScmFactory.Session.createSession(SessionType.AUTH_SESSION, scOpt);
			Assert.fail("login shouldn't succeed when password is not md5!");
		} catch (ScmException e) {
			if (-301 != e.getErrorCode()) { // EN_SCM_BUSINESS_LOGIN_FAILED(-301)
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
		try {
			restorePasswd(userCL, TestScmBase.scmUserName);
		} catch (BaseException e) {
			System.out
					.println("fail to restore passwd, user: " + TestScmBase.scmUserName + " old passwd: " + oldPasswd);
			Assert.fail(e.getMessage());
		} finally {
			if (sdb != null) {
				sdb.close();
			}
		}

	}

	private void saveAndChangePasswd(DBCollection cl, String userName, String newPasswd) {
		// save password to restore then
		DBCursor cursor = null;
		try {
			cursor = userCL.query("{ user: '" + userName + "' }", null, null, null);
			BSONObject rec = cursor.getNext();
			oldPasswd = (String) rec.get("password");
		} finally {
			cursor.close();
		}

		// update the password
		cl.update("{ user: '" + userName + "' }", "{ $set: { password: '" + newPasswd + "' } }", null);
	}

	private void restorePasswd(DBCollection cl, String userName) {
		cl.update("{ user: '" + userName + "' }", "{ $set: { password: '" + oldPasswd + "' } }", null);
	}

}