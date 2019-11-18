package com.sequoiacm.net.readcachefile;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * @Description:  SCM-2412 :: 分站点存在残留大小不一致的文件，通过seekable的方式跨中心读取文件
 * @author fanyu
 * @Date:2019年02月28日
 * @version:1.0
 */

public class TD2412_SeekFileWhenRemainFile extends TestScmBase {
	private boolean runSuccess = false;
	private List<SiteWrapper> sites = null;
	private WsWrapper wsp = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private String fileName = "file2412";
	private ScmId fileId = null;
	private int fileSize = 100;
	private File localPath = null;
	private String filePath = null;
	private String remainFilePath = null;

	@BeforeClass(alwaysRun = true)
	private void setUp() throws ScmException, IOException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
		remainFilePath = localPath + File.separator + "localFile_" + fileSize/2 + "_2.txt";
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, fileSize);
		TestTools.LocalFile.createFile(remainFilePath, fileSize/2);
		wsp = ScmInfo.getWs();
		sites = ScmNetUtils.getRandomSites(wsp);
		sessionA = TestScmTools.createSession(sites.get(0));
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is(fileName).get();
		ScmFileUtils.cleanFile(wsp, cond);
	}

	@Test(groups = {"fourSite"})
	private void test() throws Exception {
		// write from centerA
		fileId = ScmFileUtils.create(wsA, fileName, filePath);
		// remain file from centerB
		TestSdbTools.Lob.putLob(sites.get(1), wsp, fileId, remainFilePath);
		// read from centerB
		this.seekFileFromB();
		// check result
		SiteWrapper[] expSites = {sites.get(0), sites.get(1)};
		ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);
		runSuccess = true;
	}

	@AfterClass(alwaysRun = true)
	private void tearDown() throws ScmException {
		try {
			if (runSuccess || forceClear) {
				ScmFactory.File.deleteInstance(wsA, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
		}
	}

	private void seekFileFromB() throws Exception {
		ScmSession session = null;
		OutputStream fos = null;
		ScmInputStream in = null;
		try {
			session = TestScmTools.createSession(sites.get(1));
			ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
			// read content
			ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			fos = new FileOutputStream(new File(downloadPath));
			in = ScmFactory.File.createInputStream(ScmType.InputStreamType.SEEKABLE, scmfile);
			in.seek(CommonDefine.SeekType.SCM_FILE_SEEK_SET, 0);
			in.read(fos);
		} finally {
			if (fos != null)
				fos.close();
			if (in != null)
				in.close();
			if (session != null) {
				session.close();
			}
		}
	}
}