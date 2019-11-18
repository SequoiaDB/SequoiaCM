/**
 * 
 */
package com.sequoiacm.site;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.ScmCursor;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;

/**
 * @Description: listSite获取站点列表 
 * @author fanyu
 * @Date:2017年11月9日
 * @version:1.0
 */
public class ListSite956 extends TestScmBase {
	private SiteWrapper site = null;
	private Sequoiadb sdb = null;
	private List<BSONObject> dbList = new ArrayList<BSONObject>();
	private List<ScmSiteInfo> list = new ArrayList<ScmSiteInfo>();

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		try {
			site = ScmInfo.getSite();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "oneSite", "twoSite", "fourSite" })
	private void testListSite() throws ScmException {
		listSiteByScm();
		listSiteByDB();
		Assert.assertEquals(list.size(), dbList.size());
		for (int i = 0; i < list.size(); i++) {
			ScmSiteInfo info = list.get(i);
			BSONObject obj = dbList.get(info.getId() - 1);
			check(info, obj);
		}
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() {
	}

	private void listSiteByScm() throws ScmException {
		ScmSession session = null;
		ScmCursor<ScmSiteInfo> siteInfo = null;
		try {
			session = TestScmTools.createSession(site);
			siteInfo = ScmFactory.Site.listSite(session);
			while (siteInfo.hasNext()) {
				ScmSiteInfo obj = siteInfo.getNext();
				if (obj != null) {
					list.add(obj);
				}
			}
		} finally {
			if (siteInfo != null) {
				siteInfo.close();
			}
			if(session != null){
				session.close();
			}
		}
	}

	private void listSiteByDB() {
		DBCursor dbcursor = null;
		sdb = new Sequoiadb(TestScmBase.mainSdbUrl, TestScmBase.sdbUserName, TestScmBase.sdbPassword);
		DBCollection siteCL = sdb.getCollectionSpace(TestSdbTools.SCM_CS).getCollection(TestSdbTools.SCM_CL_SITE);
		if (siteCL != null) {
			dbcursor = siteCL.query(null, null, "{\"id\" : 1}", null);
		}
		try {
			while (dbcursor.hasNext()) {
				BSONObject obj = dbcursor.getNext();
				if (obj != null) {
					dbList.add(obj);
				}
			}
		} finally {
			if (dbcursor != null) {
				dbcursor.close();
			}
			if (sdb != null) {
				sdb.close();
			}
		}
	}

	private void check(ScmSiteInfo info, BSONObject obj) {
		BSONObject dataObj = (BSONObject) obj.get("data");
		BSONObject metaObj = (BSONObject) obj.get("meta");
		Assert.assertEquals(info.getName(), obj.get("name"));
		Assert.assertEquals(info.getId(), obj.get("id"));
		Assert.assertEquals(info.isRootSite(), obj.get("root_site_flag"));
		if (dataObj != null) {
			//Assert.assertEquals(info.getDataUrl(), dataObj.get("url"));
			Assert.assertEquals(info.getDataUser(), dataObj.get("user"));
			Assert.assertEquals(info.getDataPasswd(), dataObj.get("password"));
			Assert.assertEquals(info.getDataType().toString(), dataObj.get("type").toString());
			//Assert.assertEquals(info.getDataCryptType(), dataObj.get("password_type"));
		}
		if (metaObj != null) {
			Assert.assertEquals(info.getMetaUrl(), metaObj.get("url"));
			Assert.assertEquals(info.getMetaUser(), metaObj.get("user"));
			Assert.assertEquals(info.getMetaPasswd(), metaObj.get("password"));
			//Assert.assertEquals(info.getMetaCryptType(), metaObj.get("password_type"));
		}
	}
}
