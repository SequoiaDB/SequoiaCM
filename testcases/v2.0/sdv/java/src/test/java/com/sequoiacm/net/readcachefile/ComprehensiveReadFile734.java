package com.sequoiacm.net.readcachefile;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;

/**
 * @FileName SCM-734 : 场景五测试（跨中心修改文件）
 * @Author linsuqiang
 * @Date 2017-08-08
 * @Version 1.00
 */

/*
 * 1、分中心B写入多个文件； 2、分中心A读取文件并新写入多个文件； 3、在分中心B读取所有文件 4、检查文件元数据及内容正确性；
 */

public class ComprehensiveReadFile734 extends TestScmBase {
	private static final Logger logger = Logger.getLogger(ComprehensiveReadFile734.class);
	private boolean runSuccess = false;
	private SiteWrapper site1 = null;
	private SiteWrapper site2 = null;
	private WsWrapper wsp = null;

	private ScmSession ssA = null;
	private ScmWorkspace wsA = null;
	private ScmSession ssB = null;
	private ScmWorkspace wsB = null;

	private List<ScmId> fileIdList = new ArrayList<ScmId>();
	private final int fileSize = 200 * 1024;
	private final String author = "file734";
	private final int writefileNum = 50;

	private File localPath = null;
	private String filePath = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		try {
			TestTools.LocalFile.removeFile(localPath);
			TestTools.LocalFile.createDir(localPath.toString());
			TestTools.LocalFile.createFile(filePath, fileSize);
            
			wsp = ScmInfo.getWs();
			List<SiteWrapper> sites = ScmNetUtils.getSortSites(wsp);
			
			site1 = sites.get(0);
			site2 = sites.get(1);
			
			ssA = TestScmTools.createSession(site1);
			wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), ssA);

			ssB = TestScmTools.createSession(site2);
			wsB = ScmFactory.Workspace.getWorkspace(wsp.getName(), ssB);

			BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(author).get();
			ScmFileUtils.cleanFile(wsp, cond);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "fourSite" } )
	private void test() throws Exception {
		try {
			writeFile(wsB);
			List<List<ScmFileLocation>> befLocLists = getLocationLists(fileIdList);

			readFile(wsA);
			List<List<ScmFileLocation>> aftLocLists = getLocationLists(fileIdList);

			checkLocationLists1(befLocLists, aftLocLists);
			checkMetaAndLob();

			// write more file, and read all file
			writeFile(wsA);
			befLocLists = getLocationLists(fileIdList);

			readFile(wsB);
			aftLocLists = getLocationLists(fileIdList);

			checkLocationLists2(befLocLists, aftLocLists);
			checkMetaAndLob();
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
				for (ScmId fileId : fileIdList) {
					ScmFactory.File.deleteInstance(wsA, fileId, true);
				}
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch (ScmException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (null != ssA) {
				ssA.close();
			}
			if (null != ssB) {
				ssB.close();
			}
		}
	}

	private void writeFile(ScmWorkspace ws) throws ScmException {
		for (int i = 0; i < writefileNum; ++i) {
			ScmFile scmfile = ScmFactory.File.createInstance(ws);
			scmfile.setFileName(author+"_"+UUID.randomUUID()+i);
			scmfile.setAuthor(author);
			scmfile.setContent(filePath);
			fileIdList.add(scmfile.save());
		}
	}

	private void readFile(ScmWorkspace ws) throws ScmException {
		try {
			for (int i = 0; i < fileIdList.size(); ++i) {
				ScmFile scmfile = ScmFactory.File.getInstance(ws, fileIdList.get(i));
				String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
						Thread.currentThread().getId());
				scmfile.getContent(downloadPath);
				Assert.assertEquals(TestTools.getMD5(downloadPath), TestTools.getMD5(filePath));
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	private void checkMetaAndLob() throws Exception {
		SiteWrapper[] expSites = { site1,site2 };
		ScmFileUtils.checkMetaAndData(wsp, fileIdList, expSites, localPath, filePath);
	}

	private void checkLocationLists1(List<List<ScmFileLocation>> befLocLists, List<List<ScmFileLocation>> aftLocLists)
			throws Exception {
		Assert.assertEquals(befLocLists.size(), aftLocLists.size(), "file count is different!");
		for (int i = 0; i < befLocLists.size(); ++i) {
			checkLastAccessTime(befLocLists.get(i), aftLocLists.get(i),site2.getSiteId());
		}
	}

	private void checkLocationLists2(List<List<ScmFileLocation>> befLocLists, List<List<ScmFileLocation>> aftLocLists)
			throws Exception {
		int i = 0;
		try {
			for (i = 0; i < writefileNum; ++i) {
				checkLastAccessTime(befLocLists.get(i), aftLocLists.get(i), site2.getSiteId());
			}

			for (i = writefileNum; i < 2 * writefileNum; ++i) {
				checkLastAccessTime(befLocLists.get(i), aftLocLists.get(i), site1.getSiteId());
			}
		} catch (ScmException e) {
			logger.error("i=" + i + ", locationInfo=" + befLocLists.get(i));
			e.printStackTrace();
		}
	}

	private void checkLastAccessTime(List<ScmFileLocation> befLocList, List<ScmFileLocation> aftLocList, int siteId)
			throws Exception {

		Date befDate = getLastAccessTime(befLocList, siteId);
		Date aftDate = getLastAccessTime(aftLocList, siteId);

		Assert.assertTrue((aftDate.getTime() > befDate.getTime()), "checkLastAccessTime failed, siteId=" + siteId
				+ ", beforeTime=" + aftDate.getTime() + "afterTime=" + aftDate.getTime());
	}

	private Date getLastAccessTime(List<ScmFileLocation> locList, int siteId) throws Exception {
		ScmFileLocation matchLoc = null;
		for (ScmFileLocation loc : locList) {
			if (loc.getSiteId() == siteId) {
				matchLoc = loc;
				break;
			}
		}
		if (null == matchLoc) {
			throw new Exception("no such site id on the location list");
		}
		return matchLoc.getDate();
	}

	private List<List<ScmFileLocation>> getLocationLists(List<ScmId> fileIdList) throws ScmException {
		ScmSession ss = null;
		try {
			List<List<ScmFileLocation>> locationLists = new ArrayList<>();
			ss = TestScmTools.createSession(site1);
			for (ScmId fileId : fileIdList) {
				ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), ss);
				ScmFile file = ScmFactory.File.getInstance(ws, fileId);
				List<ScmFileLocation> locationList = file.getLocationList();
				locationLists.add(locationList);
			}
			return locationLists;
		} finally {
			if (null != ss) {
				ss.close();
			}
		}
	}
}