package com.sequoiacm.readcachefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.InputStreamType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine.SeekType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-248:read文件，偏移+长度<byte总长度 
 *            	1、分中心A写文件 
 *              2、分中心B调用read(byte[]b, int off, int len)读取文件,偏移+长度<byte总长度,len > 1M
 * @author huangxiaoni init
 * @date 2017.5.6
 * @modified By wuyan
 * @modified Date 2018.7.23
 */

public class ReadFileByOff248 extends TestScmBase {
	private boolean runSuccess = false;
	private SiteWrapper rootSite = null;
	private List<SiteWrapper> branSites = null;
	private final int branSitesNum = 2;
	private WsWrapper wsp = null;
	private ScmSession sessionA = null;
	private ScmWorkspace wsA = null;
	private ScmSession sessionB = null;
	private ScmWorkspace wsB = null;

	private String fileName = "readCacheFile248";
	private ScmId fileId = null;
	private int fileSize = 1024 * 1024 * 3;
	private int seekSize = 1024;
	private int off = 1024 * 10;
	private int len = 1024 * 1024 * 2;
	private File localPath = null;
	private String filePath = null;

	@BeforeClass()
	private void setUp() throws IOException, ScmException {
		localPath = new File(TestScmBase.dataDirectory + File.separator + TestTools.getClassName());
		filePath = localPath + File.separator + "localFile_" + fileSize + ".txt";
	
		TestTools.LocalFile.removeFile(localPath);
		TestTools.LocalFile.createDir(localPath.toString());
		TestTools.LocalFile.createFile(filePath, "aa1", fileSize);

		rootSite = ScmInfo.getRootSite();
		branSites = ScmInfo.getBranchSites(branSitesNum);
		wsp = ScmInfo.getWs();
		
		//clean file
		BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is(fileName).get();
		ScmFileUtils.cleanFile(wsp, cond);

		sessionA = TestScmTools.createSession(branSites.get(0));
		wsA = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionA);	
		sessionB = TestScmTools.createSession(branSites.get(1));
		wsB = ScmFactory.Workspace.getWorkspace(wsp.getName(), sessionB);	
	}

	@Test(groups = { "fourSite" })
	private void test() throws Exception {
		fileId = ScmFileUtils.create(wsA, fileName, filePath);
		this.readFileFromB( wsB );		
		runSuccess = true;
	}

	@AfterClass()
	private void tearDown() {
		try {
			if (runSuccess || forceClear) {
				ScmFactory.File.deleteInstance(wsA, fileId, true);
				TestTools.LocalFile.removeFile(localPath);
			}
		} catch ( Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			if (sessionA != null) {
				sessionA.close();
			}
			if (sessionB != null) {
				sessionB.close();
			}
		}
	}

	private void readFileFromB( ScmWorkspace ws ) throws Exception {		
		OutputStream fos = null;
		ScmInputStream in = null;
		try {			
			// read content
			ScmFile scmfile = ScmFactory.File.getInstance(ws, fileId);
			String downloadPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());

			in = ScmFactory.File.createInputStream(InputStreamType.SEEKABLE, scmfile);
			in.seek(SeekType.SCM_FILE_SEEK_SET, seekSize);
			fos = new FileOutputStream(new File(downloadPath));
			byte[] buffer = new byte[fileSize];
			int curOff = off;
			int curLen = len;
			int endOff = off + len;
			while (true) {
				int readSize = in.read(buffer, curOff, curLen);				
				if (readSize == -1 ) {
					Assert.fail("can not read to the end!");
				}		
				
				fos.write(buffer, curOff, readSize);				
				if( curOff + readSize < endOff  ){
					curOff += readSize;
					curLen = endOff - curOff;
				}else{
					break;
				}				
			}		

			// check results
			String tmpPath = TestTools.LocalFile.initDownloadPath(localPath, TestTools.getMethodName(),
					Thread.currentThread().getId());
			TestTools.LocalFile.readFile(filePath, seekSize, len, tmpPath);
			Assert.assertEquals(TestTools.getMD5(tmpPath), TestTools.getMD5(downloadPath));

			SiteWrapper[] expSites = { rootSite, branSites.get(0), branSites.get(1) };
			ScmFileUtils.checkMetaAndData(wsp, fileId, expSites, localPath, filePath);
		} finally {
			if (fos != null)
				fos.close();
			if (in != null)
				in.close();			
		}
	}

}