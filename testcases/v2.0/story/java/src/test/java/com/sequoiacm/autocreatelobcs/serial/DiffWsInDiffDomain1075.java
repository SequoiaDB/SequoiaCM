
package com.sequoiacm.autocreatelobcs.serial;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeDefine;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;

/**
 * @Description:SCM-1075:创建多个ws分别指定不同的domain
 * @author fanyu
 * @Date:2018年1月26日
 * @version:1.0
 */
public class DiffWsInDiffDomain1075 extends TestScmBase {
	private boolean runSuccess = false;
	private List<SiteWrapper> siteList = null;
	private ScmSession session = null;
	private String wsName1 = "ws1_test1075";
	private SiteWrapper rootSite = null;
	private String fileName = "DiffWsInDiffDomain1075";
	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private File localPath = null;
	private String filePath = null;
	private List<String> domainNameList = new ArrayList<String>();

	@BeforeClass(alwaysRun = true)
	private void setUp() throws Exception {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + 0 + ".txt";
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, 0);
		rootSite = ScmInfo.getRootSite();
		siteList = ScmInfo.getAllSites();
		if (siteList == null || siteList.size() == 0) {
			throw new Exception("no site!");
		}
		session = TestScmTools.createSession(rootSite);
		domainNameList.add("metaDomain1");
		domainNameList.add("dataDomain1");
		domainNameList.add("dataDomain2");
		ScmWorkspaceUtil.deleteWs(wsName1, session);
	}

	@Test(groups = { "twoSite", "fourSite" })
	private void testMetaDomain() throws Exception {
		testDiffWsInDiffDomain();
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws Exception {
		ScmWorkspaceUtil.deleteWs(wsName1, session);
		try {
			if (runSuccess || TestScmBase.forceClear) {
				for (SiteWrapper site : siteList) {
					if (site.getDataType().equals(DatasourceType.SEQUOIADB)) {
						dropAllDomain(site, domainNameList, false);
					}
				}
				dropAllDomain(rootSite, domainNameList.subList(0, 1), true);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	private void testDiffWsInDiffDomain() throws Exception {
		
		String metaStr1 = "{site:\'" + rootSite.getSiteName() + "\',domain:\'"
				+ createDomain(rootSite, domainNameList.get(0), true) + "\'}";
		String dataStr1 = createDataStr(domainNameList);
		
		ScmWorkspaceUtil.createWs(session, wsName1, metaStr1, dataStr1);

		ScmUser superuser = ScmFactory.User.getUser(session, TestScmBase.scmUserName);
		ScmResource rs = ScmResourceFactory.createWorkspaceResource(wsName1);
		ScmFactory.Role.grantPrivilege(session, superuser.getRoles().iterator().next(), rs, ScmPrivilegeDefine.ALL);
		boolean success = false;
		for (int i = 0; i < 60; i++) {
			try {
				Thread.sleep(1000);
				ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace(wsName1, session);
				ScmId fileId = ScmFileUtils.create(ws1, fileName, filePath);
				fileIdList.add(fileId);
				success = true;
				break;
			} catch (ScmException e) {
				Assert.assertEquals(e.getErrorCode(), ScmError.OPERATION_UNAUTHORIZED.getErrorCode(),e.getMessage());
			}
		}
		Assert.assertTrue(success, "getting priority spends over 60s!");
	}

	private String createDataStr(List<String> domainNameList) throws Exception {
		String dataStr = "[";
		for (int i = 0; i < siteList.size() - 1; i++) {
			if (siteList.get(i).getDataType().equals(DatasourceType.SEQUOIADB)) {
				dataStr += "{site:\'" + siteList.get(i).getSiteName() + "\',domain:\'"
						+ createDomain(siteList.get(i), domainNameList.get(i % domainNameList.size()), false)
						+ "\',data_sharding_type:{collection_space:\'year\',collection:\'month\'}},";

			} else {
				dataStr += "{site:\'" + siteList.get(i).getSiteName() + "'},";
			}
		}
		SiteWrapper lastSite = siteList.get(siteList.size() - 1);
		if (lastSite.getDataType() == DatasourceType.SEQUOIADB) {
			dataStr += "{site:\'" + lastSite.getSiteName() + "\',domain:\'"
					+ createDomain(lastSite, domainNameList.get(siteList.size() % domainNameList.size()), false)
					+ "\',data_sharding_type:{collection_space:\'year\',collection:\'month\'}}]";
		} else {
			dataStr += "{site:\'" + lastSite.getSiteName() + "\'}]";
		}
		return dataStr;
	}

	private List<String> getGroupNames(Sequoiadb db) {
		List<String> groupNameList = db.getReplicaGroupNames();
		List<String> sysGroupname = new ArrayList<String>();
		int num = groupNameList.size();
		for (int i = 0; i < num; i++) {
			if (groupNameList.get(i).contains("SYS")) {
				sysGroupname.add(groupNameList.get(i));
			}
		}
		groupNameList.removeAll(sysGroupname);
		return groupNameList;
	}

	private String createDomain(SiteWrapper site, String domainName, boolean flag) throws Exception {
		Sequoiadb db = null;
		try {
			if (!flag) {
				db = new Sequoiadb(site.getDataDsUrl(), TestScmBase.sdbUserName, TestScmBase.sdbPassword);
			} else {
				db = new Sequoiadb(site.getMetaDsUrl(), TestScmBase.sdbUserName, TestScmBase.sdbPassword);
			}
			if (db.isDomainExist(domainName)) {
				return domainName;
			}
			List<String> groupNameList = getGroupNames(db);
			if (groupNameList == null || groupNameList.size() == 0) {
				throw new Exception("db does not exist group," + groupNameList);
			}
			BSONObject obj = new BasicBSONObject();
			obj.put("Groups", groupNameList.toArray());
			try {
				db.createDomain(domainName, obj);
			} catch (BaseException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (db != null) {
				db.close();
			}
		}
		return domainName;
	}

	private void dropAllDomain(SiteWrapper site, List<String> domainNameList, boolean flag) {
		Sequoiadb db = null;
		try {
			if (!flag) {
				db = new Sequoiadb(site.getDataDsUrl(), TestScmBase.sdbUserName, TestScmBase.sdbPassword);
			} else {
				db = new Sequoiadb(site.getMetaDsUrl(), TestScmBase.sdbUserName, TestScmBase.sdbPassword);
			}
			try {
				for (String domainName : domainNameList) {
					System.out.println("domainName1 = " + domainName + " : " + site.toString());
					db.dropDomain(domainName);
				}
			} catch (BaseException e) {
				if (e.getErrorCode() != -214) {
					e.printStackTrace();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}
}
